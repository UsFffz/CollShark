package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSpuServiceImpl implements ISeckillSpuService {
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisBloomUtils redisBloomUtils;

    private static final String
          SECKILL_SPU_DETAIL_VO_PREFIX = "seckill:spu:detail:vo:";

    //分页查询秒杀商品信息
    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        //设置分页条件 开始设置分页秒杀商品列表
        PageHelper.startPage(page,pageSize);
        List<SeckillSpu> seckillSpus = seckillSpuMapper.findSeckillSpus();
        List<SeckillSpuVO> list = new ArrayList<>();
        //最终返回vo对象
        for ( SeckillSpu seckillSpu : seckillSpus) {
            Long spuId = seckillSpu.getSpuId();
            //常规信息
            SpuStandardVO spuStandardVO = dubboSeckillSpuService.getSpuById(spuId);
            SeckillSpuVO seckillSpuVO = new SeckillSpuVO();
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            list.add(seckillSpuVO);
        }
        return JsonPage.restPage(new PageInfo<>(list));
    }

    //秒杀所有信息都要保存到redis中 包括spudetail
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        //这里需要用到布隆过滤器
        //查看spuid是否在过滤器中 如果不在则抛出异常
        //获得本批次布隆过滤器的key
        String bloomKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        //判断要访问的spuId是否在布隆过滤器中
        if (!redisBloomUtils.bfexists(bloomKey,spuId+"")){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"您访问的商品不存在");
        }
        //获取要使用的spuid 常量
        String seckillSpuKey = SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        //声明要返回的对象
        SeckillSpuVO seckillSpuVO;
        if (redisTemplate.hasKey(seckillSpuKey)){
            //直接从redis中获取
            seckillSpuVO = (SeckillSpuVO) redisTemplate.boundValueOps(seckillSpuKey).get();
        }else {
            //如果redis中没有key 则需要查到秒杀信息 和 普通的信息
            //布隆过滤器会有一定的误判现象
            SeckillSpu spu = seckillSpuMapper.findSeckillSpuById(spuId);
            if (spu == null){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"您所查询商品不存在");
            }
            //这里已经查出spu商品的秒杀信息 其次查询常规信息
            SpuStandardVO spuStandardVO = dubboSeckillSpuService
                                                      .getSpuById(spuId);
            seckillSpuVO = new SeckillSpuVO();
            //将常规spu信息对象的同名属性复制给seckillSpuVo
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            //将秒杀信息赋值给seckillSpuVo
            seckillSpuVO.setSeckillListPrice(spu.getListPrice());
            seckillSpuVO.setStartTime(spu.getStartTime());
            seckillSpuVO.setEndTime(spu.getEndTime());
            redisTemplate.boundValueOps(seckillSpuKey).set(seckillSpuVO,
                    10*60*1000 + RandomUtils.nextInt(10000),
                    TimeUnit.MICROSECONDS);
        }
        //给SeckillSpu的url赋值
        //一旦给url属性赋值 就意味着该当前用户可以提交购买订单
        //必须判断当前时间是否在秒杀时间段内 才能决定是否给url赋值
        LocalDateTime localDateTime = LocalDateTime.now();
        //当前为高并发状态,不要连接数据库判断
        LocalDateTime seckillTimeStart = seckillSpuVO.getStartTime();
        LocalDateTime seckillTimeEnd = seckillSpuVO.getEndTime();
        //判断原则为 判断开始时间小于当前时间小于结束时间
        //本次使用时间差对象Duration来判断时间关系
        //利用Duration提供的一个between方法来获得两个对象关系
        //这个方法有个特征 如果时间差是负数 会返回nagative状态
        //前大 后小 则返回nagative 状态 反之 返回时间差
        Duration afterTime = Duration.between(localDateTime,seckillTimeStart);
        //判断结束时间大于当前时间
        Duration beforeTime = Duration.between(seckillTimeEnd,localDateTime);
        if (afterTime.isNegative() && beforeTime.isNegative()){
            //根据spuid获得redis事先预热好的随机码
            String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
            String randomCode = redisTemplate.boundValueOps(randCodeKey).get().toString();
            //将随机码赋值到url
            seckillSpuVO.setUrl("/seckill/" + randomCode);
            log.warn("随机码为:{}",randomCode);
        }
        return seckillSpuVO;
    }

    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        //获得常量key
        String seckillSpuDetailKey = SECKILL_SPU_DETAIL_VO_PREFIX + spuId;
        SeckillSpuDetailSimpleVO simpleVO;
        //判断redis中是否有包含这个key
        if (redisTemplate.hasKey(seckillSpuDetailKey)){
            return (SeckillSpuDetailSimpleVO) redisTemplate.boundValueOps(seckillSpuDetailKey).get();
        }else {
            SpuDetailStandardVO spuStandardVO =
                    dubboSeckillSpuService.getSpuDetailById(spuId);
            simpleVO = new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuStandardVO,simpleVO);
            redisTemplate.boundValueOps(seckillSpuDetailKey).set(simpleVO,
                    10*60*1000+RandomUtils.nextInt(1000),
                    TimeUnit.MICROSECONDS);
            return simpleVO;
        }
    }
}
