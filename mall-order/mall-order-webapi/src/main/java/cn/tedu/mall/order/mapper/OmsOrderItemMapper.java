package cn.tedu.mall.order.mapper;


import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderItemMapper {

    // 新增订单项的方法 order_item 方法
    // 一个订单可能包含多个商品订单项 如果循环遍历新增每一个订单项,连库次数多 效率降低
    // 所以采用一次连库 新增多条订单项的方法 完成业务.
    int insertOrderItemList(List<OmsOrderItem> omsOrderItems);
}
