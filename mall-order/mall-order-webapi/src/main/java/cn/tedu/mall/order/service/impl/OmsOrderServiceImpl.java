package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//后期秒杀也需要生成订单 可以直接调用当前类方法
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    private IOmsCartService omsCartService;
    @DubboReference
    private IForOrderSkuService forOrderSkuService;

    //新增订单的方法
    //这个方法调用了product模块中的功能 数据库操作.
    //运行出现异常 必须依靠分布式事务组件进行回滚
    //所以 运行范围异常时 必须依靠分布式事务组件进行回滚 以保证事务的原子性
    //激活seata分布式事务功能
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        OmsOrder order = new OmsOrder();
        //将当前方法参数OrderAddDTO复制给order对象
        BeanUtils.copyProperties(orderAddDTO,order);
        //order属性中属性较少 还需手动计算和赋值
        loadOrder(order);
        //运行以上以后 order模块赋值完毕
        //下面开始为订单项 orderitem赋值
        // orderDto 中包含了一个OrderItemAddDto
        //将集合转换为 OmsOrderItem 类型
        if (orderAddDTO.getOrderItems()== null || orderAddDTO.getOrderItems().isEmpty() ||orderAddDTO.getOrderItems().size() == 0){
            throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR,"订单不可为空!");
        }
        //先将要获得的最终结果集合实例化
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        for ( OrderItemAddDTO addDTO : orderAddDTO.getOrderItems()) {
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            BeanUtils.copyProperties(addDTO,omsOrderItem);
            //以上赋值操作后 仍然有个别属性没有被赋值
            Long itemId = IdGeneratorUtils.getDistributeId("order_item");
            omsOrderItem.setId(itemId);
            omsOrderItem.setOrderId(order.getId());
            omsOrderItems.add(omsOrderItem);
            //获得skuId
            Long skuId = omsOrderItem.getSkuId();
            Integer quantity = omsOrderItem.getQuantity();
            int rows = forOrderSkuService.reduceStockNum(skuId,quantity);
            if (rows == 0){
                log.warn("商品skuId:{}库存不足",skuId);
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"商品库存不足!");
            }
            //删除购物车信息
            OmsCart omsCart = new OmsCart();
            omsCart.setUserId(omsCart.getUserId());
            omsCart.setSkuId(omsCart.getSkuId());
            omsCartService.removeUserCarts(omsCart);
        }
        //新增订单
        omsOrderMapper.insertOrder(order);
        //新增订单项
        omsOrderItemMapper.insertOrderItemList(omsOrderItems);
        OrderAddVO orderAddVO = new OrderAddVO();
        orderAddVO.setId(order.getId());
        orderAddVO.setSn(order.getSn());
        orderAddVO.setCreateTime(order.getGmtPay());
        orderAddVO.setPayAmount(order.getAmountOfActualPay());
        return orderAddVO;
    }

    private void loadOrder(OmsOrder order) {
        //针对order进行其他额外赋值
        //给id赋值 给leaf分布式赋值
        Long id = IdGeneratorUtils.getDistributeId("order");
        order.setId(id);
        //赋值用户id
        if (order.getUserId() == null){
            order.setUserId(new OmsCartServiceImpl().getUserId());
        }
        //赋值订单号
        //使用随机uuid做随机订单号进行生成
        order.setSn(UUID.randomUUID().toString());
        //如果订单状态 为null
        if (order.getState()==null){
            order.setState(0);
        }
        //为了保证下单时间 还有时间创建实现 和最后修改时间一致 统一手动赋值
        LocalDateTime localDateTime = LocalDateTime.now();
        order.setGmtOrder(localDateTime);
        order.setGmtCreate(localDateTime);
        order.setGmtModified(localDateTime);
        // 计算实际金额
        // 计算公式: 实际支付金额=原价-优惠+运费
        //数据类型使用BigDicimal 是没有浮点偏移的精确运算
        BigDecimal price = order.getAmountOfOriginalPrice();
        BigDecimal freight = order.getAmountOfFreight();
        BigDecimal discount = order.getAmountOfDiscount();
        BigDecimal actualPay = price.subtract(discount).add(freight);
        //将最后实际金额赋给order
        order.setAmountOfActualPay(actualPay);
    }

    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {
        OmsOrder order = new OmsOrder();
        BeanUtils.copyProperties(orderStateUpdateDTO,order);
        omsOrderMapper.updateOrderById(order);
    }

    // 分页查询当前登录用户指定时间 指定时间范围内的所有订单
    // 默认查询一个月内的订单 查询返回值 OrderListVo 是包含订单信息和订单中商品信息的对象
    // 持久层已经编写号了
    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        //业务逻辑层判断指定用户指定时间信息
        validateTimeAndLoadTimes(orderListTimeDTO);
        Long userId = new OmsCartServiceImpl().getUserId();
        orderListTimeDTO.setUserId(userId);
        PageHelper.startPage(orderListTimeDTO.getPage(),orderListTimeDTO.getPageSize());
        List<OrderListVO> list = omsOrderMapper.selectOrdersBetweenTimes(orderListTimeDTO);
        return JsonPage.restPage(new PageInfo<>(list));
    }

    private void validateTimeAndLoadTimes(OrderListTimeDTO orderListTimeDTO) {
        LocalDateTime startTime = orderListTimeDTO.getStartTime();
        LocalDateTime endTime = orderListTimeDTO.getEndTime();

        if (startTime ==null && endTime == null){
            startTime = LocalDateTime.now().minusMonths(1);
            endTime = LocalDateTime.now();
            orderListTimeDTO.setStartTime(startTime);
            orderListTimeDTO.setEndTime(endTime);
        }else if (startTime == null && endTime != null){
            startTime = LocalDateTime.now().minusYears(20);
            orderListTimeDTO.setStartTime(startTime);
        }else if (endTime == null && startTime !=null){
            endTime = LocalDateTime.now();
            orderListTimeDTO.setEndTime(endTime);
        }
        if (endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli() <
            startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli()){
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"结束时间应大于起始时间");
        }
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }
}
