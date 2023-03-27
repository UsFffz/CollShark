package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderMapper {
    //新增订单的方法
    int insertOrder(OmsOrder order);

    // 查询用户指定时间范围内的所有订单信息(包含订单项)
    List<OrderListVO> selectOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO);

    //动态修改订单
    int updateOrderById(OmsOrder order);

}
