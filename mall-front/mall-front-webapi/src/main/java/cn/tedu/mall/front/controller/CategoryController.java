package cn.tedu.mall.front.controller;


import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/front/category")
@RestController
@Api(tags = "前台分类查询")
public class CategoryController {
    @Autowired
    private IFrontCategoryService frontCategoryService;

    @GetMapping("/all")
    @ApiOperation("查询获得所有三级分类书对象")
    public JsonResult<FrontCategoryTreeVO<FrontCategoryEntity>> getTreeVO(){
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                frontCategoryService.categoryTree();
        return JsonResult.ok(treeVO);
    }
}
