package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SeckillServiceImpl implements ISeckillService {
    //秒杀业务中需要修改库存 判断数值 所以调用stringRedisTemplate 并且可以存储购买数值 以便判断是否为重复购买
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //需要调用普通订单生成方法
    @DubboReference
    private IOmsOrderService dubboOmsOrderService;
    //因为要将秒杀成功的信息发送给消息队列 所以准备消息队列对象
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        //1 利用redis检查重复购买和库存数
        Long skuId = seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        Long userId = getUserId();
        //已得知 某个用户 购买了某个sku商品
        //业务规定 一个用户只能购买一个skuId商品
        // 所以我们可以根据当前userId和skuId进行重复购买的检查
        //先获得检查重复购买的key
        String reSeckillCheckKey = SeckillCacheUtils.getReseckillCheckKey(skuId,userId);
        //当运行完StringRedisTemplate的increment方法后会将当前值返回
        Long seckillTimes = stringRedisTemplate.boundValueOps(reSeckillCheckKey).increment();
        if (seckillTimes > 1 ){
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,"您已购买过当前商品!");
        }
        //程序运行到此处 表明用户没有购买过当前商品 开始检查库存
        //获得指定skuid库存数
        String seckillSkuCountKey = SeckillCacheUtils.getStockKey(skuId);
        //根据当前key获取redis中保存的sku库存数                                    该方法为自减
        Long leftStock = stringRedisTemplate.boundValueOps(seckillSkuCountKey).decrement();
        //leftStock 是用户购买后剩余库存数 只有leftStock < 0 时才表示 没有库存了
        if (leftStock < 0){
            //先将当前用户的购买此商品的次数修改为0
            stringRedisTemplate.boundValueOps(reSeckillCheckKey).decrement();
            //如果已经没有库存 则终止当前用户对此商品的购买
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,"该商品已售罄!");
        }
        //当前用户经过了重复购买和库存检查的判断 可以开始购买 生成订单
        //2 将秒杀订单转换为普通订单
        //自定义一个普通订单 写一个单方法 将秒杀订单转换为普通订单
        OrderAddDTO orderAddDTO = converSeckillOrderToOrder(seckillOrderAddDTO);
        orderAddDTO.setUserId(userId);
        //信息完成后 dubbo调用新增订单方法
        OrderAddVO orderAddVO = dubboOmsOrderService.addOrder(orderAddDTO);
        //3 秒杀成功信息放进消息队列中
        //向rabbitmq中发送success对象 来实现消息队列的方式到数据库
        //因为success对象作为秒杀成功记录来讲 并不急迫与操作
        //因为它又要操作数据库 最好在服务器不忙时进行运行 进行削峰添谷
        Success success = new Success();
        //success中 大多数属性是和sku实体具备的 可操作的对象中 秒杀订单项是sku 所以可以将秒杀订单项的同名属性赋值给success
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),seckillTimes);
        //补全缺少信息
        success.setUserId(userId);
        success.setOrderSn(orderAddVO.getSn());
        success.setSeckillPrice(seckillOrderAddDTO.getSeckillOrderItemAddDTO().getPrice());
        //success信息完备 向rabbit中发送
        rabbitTemplate.convertSendAndReceive(RabbitMqComponentConfiguration.SECKILL_EX,
                                             RabbitMqComponentConfiguration.SECKILL_RK,
                                             success);
        //最后按系统设计的返回值返回 经过分析 其中的属性和OrderAddVo完全一致
        SeckillCommitVO seckillCommitVO = new SeckillCommitVO();
        BeanUtils.copyProperties(orderAddVO,seckillCommitVO);
        return seckillCommitVO;
    }

    private OrderAddDTO converSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO) {
        OrderAddDTO orderAddDTO = new OrderAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO,orderAddDTO);
        OrderItemAddDTO orderItemAddDTO = new OrderItemAddDTO();
        List<OrderItemAddDTO> list = new ArrayList<>();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),orderItemAddDTO);
        list.add(orderItemAddDTO);
        orderAddDTO.setOrderItems(list);
        return orderAddDTO;
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
