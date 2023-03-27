package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import jodd.time.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SeckillInitialJob implements Job {
    //查询秒杀库存数mapper
    @Autowired
    private SeckillSkuMapper seckillSkuMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException{
        //当前方法是缓存预热操作
        //执行秒杀预热是前五分钟,所以获得一个五分钟后的对象
        LocalDateTime time = LocalDateTime.now().plusMinutes(5);
        //查询这个时间秒杀的商品
        List<SeckillSpu> seckillSpuList = seckillSkuMapper.findSeckillSpusByTime(time);
        //遍历当前批次所有秒杀的spu
        for (SeckillSpu spu  : seckillSpuList) {
            //spu是商品的品类 必须确定规格 也就是确定sku后才能明确库存
            // 所以要根据spuId查询sku,然后将sku库存保存到redis中
            List<SeckillSku> seckillSkus = seckillSkuMapper.findSeckillSkusBySpuId(spu.getSpuId());
            // 遍历获得了当前spu对应的sku列表 还要遍历sku列表才能保存
            for (SeckillSku sku : seckillSkus) {
                log.info("开始将{}号sku商品预热到redis",sku.getSkuId());
                //获得事先准备好的常量字符串方法
                //它的实际的值为
                String skuStockKey = SeckillCacheUtils.getStockKey(sku.getSkuId());
                //检查redis中是否已经包含了这个key
                if (redisTemplate.hasKey(skuStockKey)){
                    System.out.println(skuStockKey);
                }else {
                    stringRedisTemplate.boundValueOps(skuStockKey).set(sku.getSeckillStock()+"",
                            60*1000+RandomUtils.nextInt(10000), TimeUnit.MILLISECONDS);
                    log.warn("开始为{}号进行商品缓存预热",sku.getSkuId());
                }
            }
            // 上面为止完成了库存的预热
            // 下面开始为每个spu商品生成对应随机码
            // 随机码为随机数 范围可自行定义
            // 随机码最终会在生成订单前进行验证,以减轻服务器压力
            String randCodeKey = SeckillCacheUtils.getRandCodeKey(spu.getSpuId());
            //判断随机码是否已经在redis中
            if (redisTemplate.hasKey(randCodeKey)){
                int randomCode = (int)redisTemplate.boundValueOps(randCodeKey).get();
                log.warn("{}号商品已经缓存,随机码:{}",spu.getSpuId(),randomCode);
            }else {
                //生成随机码 100000~50000
                int randomCode = RandomUtils.nextInt(100000)+50000;
                redisTemplate.boundValueOps(randCodeKey).set(randomCode,
                        10*60*1000+RandomUtils.nextInt(10000)
                        ,TimeUnit.MILLISECONDS);
                log.warn("spuId{}号商品生成的随机码为:{}",spu.getSpuId(),randomCode);
            }
         }
    }
}
