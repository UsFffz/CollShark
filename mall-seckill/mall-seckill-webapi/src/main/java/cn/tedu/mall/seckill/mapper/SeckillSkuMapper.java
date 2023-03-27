package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.apache.ibatis.annotations.Param;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.stereotype.Repository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillSkuMapper {
    List<SeckillSku> findSeckillSkusBySpuId(Long spuId);

    //根据指定时间查看正在秒杀的商品
    List<SeckillSpu> findSeckillSpusByTime(LocalDateTime time);


    int updateReduceStockBySkuId(@Param("skuId")Long skuId,@Param("quantity")Integer quantity);
}
