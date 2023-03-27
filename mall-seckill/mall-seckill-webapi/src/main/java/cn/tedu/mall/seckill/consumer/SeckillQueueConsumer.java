package cn.tedu.mall.seckill.consumer;

import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SuccessMapper;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMqComponentConfiguration.SECKILL_QUEUE)
public class SeckillQueueConsumer {
    @Autowired
    private SeckillSkuMapper seckillSkuMapper;
    @Autowired
    private SuccessMapper successMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @RabbitHandler
    public void process(Success success){
        try {
            //先减少少库存
            seckillSkuMapper.updateReduceStockBySkuId(success.getSkuId(),success.getQuantity());
            //新增success对象到数据库
            successMapper.saveSuccess(success);
            //上面两个数据库操作发生异常 可能会引发事务问题
            //可以在下面编写进入死信队列
            //死信队列为最后解决办法 不要频繁使用
        }catch (Throwable throwable){

        }
    }
}
