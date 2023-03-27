package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.pojo.product.vo.SkuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSkuService;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSkuServiceImpl implements ISeckillSkuService {
    @Autowired
    private SeckillSkuMapper seckillSkuMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    // 调用dubbo模块查询sku常规信息
    @DubboReference
    private IForSeckillSkuService dubboSkuService;

    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        //SeckillSkuVO作为返回值集合的泛型 其中既包括sku秒杀信息 又包含常规信息
        List<SeckillSkuVO> seckillSkuVOS = new ArrayList<>();
        //数据库查询 根据spuId查询sku列表
        List<SeckillSku> seckillSkus = seckillSkuMapper.findSeckillSkusBySpuId(spuId);
        //遍历所有sku
        for (SeckillSku sku : seckillSkus) {
            Long skuId = sku.getSkuId();
            //声明key 声明sku对象
            String seckillSkuVoKey = SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            //声明返回值类型对象
            SeckillSkuVO seckillSkuVO;
            if (redisTemplate.hasKey(seckillSkuVoKey)){
                seckillSkuVO = (SeckillSkuVO) redisTemplate.boundValueOps(seckillSkuVoKey).get();
            }else {
                SkuStandardVO skuStandardVO = dubboSkuService.getById(skuId);
                //当前循环正在遍历的对象sku是秒杀信息
                //两个方面信息都有了 就实例化返回值 seckillSkuVO准赋值
                seckillSkuVO = new SeckillSkuVO();
                BeanUtils.copyProperties(skuStandardVO,seckillSkuVO);
                seckillSkuVO.setSeckillPrice(sku.getSeckillPrice());
                seckillSkuVO.setStock(sku.getSeckillStock());
                seckillSkuVO.setSeckillLimit(sku.getSeckillLimit());
                redisTemplate.boundValueOps(seckillSkuVoKey).set(seckillSkuVO,
                        60*60*60+ RandomUtils.nextInt(1000),
                        TimeUnit.MICROSECONDS);
            }
            seckillSkuVOS.add(seckillSkuVO);
        }
        return seckillSkuVOS;
    }
    // 根据spuId查询sku列表
}
