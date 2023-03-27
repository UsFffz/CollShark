package cn.tedu.mall.order.service.impl;


import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OmsCartServiceImpl implements IOmsCartService {

    //装配mapper
    @Autowired
    private OmsCartMapper omsCartMapper;

    @Override
    public void addCart(CartAddDTO cartDTO) {
        //在查询购物车中是否有商品之前 必须先明确用户身份 也就是id
        Long userId =  getUserId();
        //根据用户id和skuid检查当前购物车中是否已经存在该商品
        OmsCart omsCart = omsCartMapper.selectExistsCart(userId,cartDTO.getSkuId());
        //判断omsCart是否为null
        if (omsCart==null){
            //如果omsCart为null 表示购物车中无商品 所以添加进去
            //因为新增方法的参数时omsCart对象 所以要先实例化出
            OmsCart omsCart1 = new OmsCart();
            omsCart1.setUserId(userId);
            BeanUtils.copyProperties(cartDTO,omsCart1);
            omsCartMapper.saveCart(omsCart1);
        }else {
            //如果不是空则表示购物车中已经有该商品 直接加上前端传的数量就可以了
            omsCart.setQuantity(omsCart.getQuantity()+cartDTO.getQuantity());
            //修改购物车数量方法
            omsCartMapper.updateQuantityById(omsCart);
        }
    }
    //根据用户id分页查询用户购物车中信息
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        // 从spring security中获得用户id
        Long userId = getUserId();
        //PageHelper设置分页查询条件
        PageHelper.startPage(page,pageSize);
        //执行查询 本次查询会在sql语句中末尾添加limit关键字完成分页
        List<CartStandardVO> list = omsCartMapper.selectCartsByUserId(userId);
        //list就是分页查询数据 下面将分页数据和分页信息转换为JsonPage
        JsonPage<CartStandardVO> jsonPage = JsonPage.restPage(new PageInfo<>(list));
        return jsonPage;
    }

    @Override
    public void removeCart(Long[] ids) {
        log.warn("开始执行删除购物车物品方法,也有可能是批量删除:{}",ids.toString());
        int rows = omsCartMapper.deleteCartsByIds(ids);
        if (rows==0){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"该商品已被删除!请刷新后重试");
        }
    }

    @Override
    public void removeAllCarts() {
        log.warn("开始删除购物车中物品");
        Long userId = getUserId();
        omsCartMapper.deleteCartsByUserId(userId);
    }

    @Override
    public void removeUserCarts(OmsCart omsCart) {
        omsCartMapper.deleteCartByUserIdAndSkuId(omsCart);
    }

    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {
        OmsCart omsCart = new OmsCart();
        BeanUtils.copyProperties(cartUpdateDTO,omsCart);
        //执行修改
        omsCartMapper.updateQuantityById(omsCart);
    }

    //业务逻辑层中有获得当前登录用户信息的需求
    //我们的程序在控制器方法运行前执行了过滤器,过滤器中解析了请求头中包含的jwt
    //解析获得jwt的用户信息后保存到了spring security的上下文中
    //所以我们可以从spring security中获得用户信息
    public CsmallAuthenticationInfo getUserInfo(){
        // 编码获得springSecurity上下文中保存的权限
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        //为了保险起见,判断一下从SpringSecurity中获得的信息是不是null
        if (authenticationToken == null){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"请先登录!");
        }
        //上下文信息确定存在后,来获取其中的用户信息
        //这个信息就是由jwt解析获得的
        CsmallAuthenticationInfo csmallAuthenticationInfo =
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        //返回登录信息
        return csmallAuthenticationInfo;
    }

    //业务逻辑层大多数只需要用户的id即可 实际上用不上很多
    public Long getUserId(){
        Long id = getUserInfo().getId();
        return id;
    }
}
