package cn.tedu.mall.front.service.impl;


import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FrontProductServiceImpl implements IFrontProductService {


    @DubboReference
    private IForFrontSpuService dubboSpuService;

    //消费sku ,根据spuid查询 sku列表功能
    @DubboReference
    private IForFrontSkuService dubboSkuService;
    //消费根据spuid查询当前spu对应的所有参数/功能
    @DubboReference
    private IForFrontAttributeService dubboAttributeService;

    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        JsonPage<SpuListItemVO> list =
                dubboSpuService.listSpuByCategoryId(categoryId,page,pageSize);
        return list;
    }

    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        SpuStandardVO spuStandardVO = dubboSpuService.getSpuById(id);
        return spuStandardVO;
    }
    //根据spuid查询对应的skuid列表
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        List<SkuStandardVO> list = dubboSkuService.getSkusBySpuId(spuId);
        return list;
    }

    //根据spuid查询spuDetail信息
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailStandardVO =
                dubboSpuService.getSpuDetailById(spuId);
        return spuDetailStandardVO;
    }
    //根据spuid查询对应的所有属性列表
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        List<AttributeStandardVO> list =
                dubboAttributeService.getSpuAttributesBySpuId(spuId);
        return list;
    }
}
