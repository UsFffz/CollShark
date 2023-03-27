package cn.tedu.mall.front.service.impl;


import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DubboService
@Service
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {
    //装配操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    //当前front没有连接数据库的操作，所有数据均来自dubbo调用product模块.
    //这里是消费product模块查询所有数据分类的功能
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;

    // 开发过程中使用redis的一个规范:
    //为了降低redis使用key拼写错误的情况，可以定义常量
    public static final String CATEGORY_TREE_KEY = "category_tree";

    @Override
    public FrontCategoryTreeVO categoryTree() {
        //我们先检查redis是否以及保存了包含所有分类的三级分类树对象.
        if (redisTemplate.hasKey(CATEGORY_TREE_KEY)) {
            //进入此中，那么说明以及包含了三级分类树。获取后直接返回即可。
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO
                    = (FrontCategoryTreeVO<FrontCategoryEntity>)
                    redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            //将从redis中获得的treeVO返回
            return treeVO;
        }
        //Redis中没有三级分类树信息,表示本次请求可能是首次访问
        //dubbo调用查询所有分类对象的方法
        List<CategoryStandardVO> categoryStandardVOS =
                dubboCategoryService.getCategoryList();
        //我们需要将没有关联子分类的CategoryStandardVO类型
        //转换为具备关联子分类能力的FrontCategoryEntity类型
        //并将正确的父子分类关系保存构建起来,最好编写一个方法
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                initTree(categoryStandardVOS);
        //上面已经完成了三级分类树的转换，下面要将对象放入redis中
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY)
                .set(treeVO, 1, TimeUnit.MINUTES);
        //上面时间针对学习测试而定，实际开发中会设置比较长的时间，例如24小时或1天.
        //最后返回
        return treeVO;
    }

    //将从数据库中查询到的分类对象转换为三级分类树的方法
    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOS) {
        //第一步 确定所有分类对象的父分类id 以父分类id为key，将相同父分类对象的子分类对象保存到同一个map的key中
        //一个父分类可能包含多个子分类，所以map的value是一个list 而不是单独的一个对象
        Map<Long, List<FrontCategoryEntity>> map = new HashMap<>();
        log.info("当前分类对象的总个数:{}", categoryStandardVOS.size());
        //遍历所有分类对象的集合
        for (CategoryStandardVO categoryStandardVO : categoryStandardVOS) {
            //CategoryStandardVO无CHILDREN，不能保存父子分类关系
            //所以要先将其中的对象转换为能够保存父子关系的类型
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            //将同名属性复制到FrontCategoryEntity中
            BeanUtils.copyProperties(categoryStandardVO, frontCategoryEntity);
            //获取当前元素对应的父分类ID 0表示根分类
            //后面要反复使用父分类ID，所以最好取出
            Long parentID = frontCategoryEntity.getParentId();
            // 这个父分类ID 要作为key保存到Map中 所以要先判断map中已经包含Key
            //运行到这，表示当前map已经包含了这个父分类key
            if (map.containsKey(parentID)) {
                //直接将当前元素追加到map中的value中
                map.get(parentID).add(frontCategoryEntity);
            } else {
                //不存在直接创建一个新的元素
                List<FrontCategoryEntity> list = new ArrayList<>();
                list.add(frontCategoryEntity);
                map.put(parentID, list);
            }
        }
        //第二步:
        //将子分类对象关联到父分类对象的children属性中
        //下面操作应该从1级分类开始 先通过0作为父id 获得所有一级分类
        List<FrontCategoryEntity> firstLevels = map.get(0L);
        //防空指针判断，判断一级分类集合是否为空 防止后续出现空指针
        if (firstLevels == null || firstLevels.isEmpty()) {
            throw new CoolSharkServiceException(
                    ResponseCode.INTERNAL_SERVER_ERROR, "无一级分类，请求终止");
        }
        //遍历所有一级分类对象
        for (FrontCategoryEntity oneLevel : firstLevels) {
            //获得当前一级分类对象的id
            Long secondLevelParentId = oneLevel.getId();
            //获取当前一级对象集合包含的二级分类集合
            List<FrontCategoryEntity> secondLives = map.get(secondLevelParentId);
            if (secondLives.isEmpty() || secondLives == null) {
                log.warn("当前分类没有二级分类内容:{}", secondLevelParentId);
                // 如果当前一级分类没有二级分类 就跳过本次循环继续下次循环
                continue;
            }
            //遍历二级对象循环集合
            for (FrontCategoryEntity twoLevel : secondLives) {
                //获得当前二级分类对象的ID(三级分类的父ID)
                Long thirdLevelParentID = twoLevel.getId();
                //获取当前二级对象集合包含的三级集合
                List<FrontCategoryEntity> thirdLives = map.get(thirdLevelParentID);
                if (thirdLives == null ||thirdLives.isEmpty()) {
                    log.warn("当前分类没有三级分类内容:{}", thirdLives);
                    continue;
                }
                //将三级分类保存到2级分类对象的children属性中
                twoLevel.setChildrens(thirdLives);
            }
            //将二级分类对象集合保存到1级的children属性中
            oneLevel.setChildrens(secondLives);
        }
        //到此为止，所有的分类对象都保存在了正确的父对象的children属性中
        //最终包含他们的是firstLevels对象,但是返回值不是，所以要实例化，将集合赋值到里面 并返回
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                new FrontCategoryTreeVO<>();
        treeVO.setCategories(firstLevels);
        return treeVO;
    }
}
