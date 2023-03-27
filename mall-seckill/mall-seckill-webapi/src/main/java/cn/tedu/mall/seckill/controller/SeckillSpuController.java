package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "秒杀商品信息模块")
@RequestMapping("/seckill/spu")
public class SeckillSpuController {
    @Autowired
    private ISeckillSpuService seckillSpuService;

    @GetMapping("/list")
    @ApiOperation("分页查询spu秒杀商品详情")
    public JsonResult<JsonPage<SeckillSpuVO>> list(Integer page,Integer size){
        JsonPage<SeckillSpuVO> jsonPage = seckillSpuService.listSeckillSpus(page,size);
        return JsonResult.ok(jsonPage);
    }

    @GetMapping("/{spuId}/detail")
    @ApiOperation("根据spuid查看detai详情")
    public JsonResult<SeckillSpuDetailSimpleVO> getSeckillDeatil(@PathVariable Long spuId){
        return JsonResult.ok(seckillSpuService.getSeckillSpuDetail(spuId));
    }
    @GetMapping("/{spuId}")
    @ApiOperation("根据spuId查询spu信息")
    public JsonResult<SeckillSpuVO> getSeckillSpuVO(@PathVariable Long spuId){
        return JsonResult.ok(seckillSpuService.getSeckillSpu(spuId));
    }
}
