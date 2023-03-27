package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.utils.WebConsts;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oms/cart")
@Api(tags = "购物车管理模块")
public class OmsCartController {
    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增购物车信息")
    //正常登录的用户 在运行控制器方法前 已经将用户信息
    @PreAuthorize("hasAuthority('Role_user')")
    //如果用户没有登录就无法访问此控制器方法
    //Validated激活validation激活
    //参数
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO){
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增sku购物车完成");
    }

    @GetMapping("/list")
    @ApiOperation("根据用户id分页查询购物车sku列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码",name = "page",example = "1"),
            @ApiImplicitParam(value = "每条页码",name = "pageSize",example = "3")
    })
    @PreAuthorize("hasAuthoity('ROLE_user')")                //RequestParam 用来设置参数默认值
    public JsonResult<JsonPage<CartStandardVO>> listCartsByPage
            (@RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE) Integer page,
             @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE_SIZE) Integer pageSize){
        return JsonResult.ok(omsCartService.listCarts(page,pageSize));
    }

    @PostMapping("/delete")
    @ApiOperation("根据id数组删除购物车中的sku信息")
    @ApiImplicitParam(value = "要删除的id数组",name = "ids",required = true,dataType = "array")
    @PreAuthorize("hasAuthoity('Role_user')")
    public JsonResult removeCartsByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok("删除功能运行完毕!");
    }

    @GetMapping("/delete/all")
    @ApiOperation("根据用户id清除购物车中物品")
    @PreAuthorize("hasAuthoity('Role_user')")
    public JsonResult removeAllCarts(){
        omsCartService.removeAllCarts();
        return JsonResult.ok("已清空购物车!");
    }

    @PostMapping("/update/quantity")
    @ApiOperation("修改购物车中sku信息")
    @PreAuthorize("hasRole('user')")
    private JsonResult updateQuantity(@Validated CartUpdateDTO cartUpdateDTO){
        omsCartService.updateQuantity(cartUpdateDTO);
        return JsonResult.ok("修改完成!");
    }
}
