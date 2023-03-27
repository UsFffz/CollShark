package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequestMapping("/seckill/sku")
@Api(tags = "秒杀sku")
public class SeckillSkuController {
    @Autowired
    private ISeckillSkuService service;
    @GetMapping("/list/{spuId}")
    @ApiOperation("根据spuid查询秒杀sku列表")
    public JsonResult<List<SeckillSkuVO>> listSeckillSkus(@PathVariable Long spuId){
        return JsonResult.ok(service.listSeckillSkus(spuId));
    }
}
