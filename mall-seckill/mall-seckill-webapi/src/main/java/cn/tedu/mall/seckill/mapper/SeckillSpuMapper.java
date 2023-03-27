package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.apache.dubbo.rpc.cluster.router.condition.config.ListenableRouter;
import org.springframework.stereotype.Repository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillSpuMapper {
    //查询秒杀商品
    List<SeckillSpu> findSeckillSpus();

    //根据spuid查询spu秒杀信息
    SeckillSpu findSeckillSpuById(Long spuId);

    //布隆过滤器 所用 查询所有spuid
    List<Long> findAllSeckillSpuIds();
}
