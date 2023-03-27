package cn.tedu.mall.seckill.timer.job;


import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
public class SeckillBloomInitialJob implements Job {
    @Autowired
    private RedisBloomUtils redisBloomUtils;
    //查询出spuId 集合/数组 保存到集合中
    private SeckillSpuMapper seckillSpuMapper;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //今天的Key
        String bloomTodayKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        //获取明天的key
        String bloomTomorrowKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now().plusDays(1));
        Long[] spuIds = seckillSpuMapper.findAllSeckillSpuIds().toArray(new Long[0]);
        //布隆过滤器支持将字符串数组进行保存
        String[] spuIdString = new String[spuIds.length];
        //将元素转换为StringIDString数组中
        for (int i = 0; i < spuIds.length; i++) {
            spuIdString[i] = spuIds[i] + "";
        }
        redisBloomUtils.bfmadd("bloomTodayKey",spuIdString);
        redisBloomUtils.bfmadd("bloomTomorowKey",spuIdString);
        System.out.println("两批次布隆过滤器加载完毕");
    }
}
