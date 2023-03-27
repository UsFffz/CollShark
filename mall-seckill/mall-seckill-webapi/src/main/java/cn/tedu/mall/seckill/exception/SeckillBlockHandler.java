package cn.tedu.mall.seckill.exception;


import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SeckillBlockHandler {
    //声明限流的方法 返回值必须和限流的控制器方法一致
    //参数是包含所有控制器的方法以外,还有BlockException
    //如果这个方法实例化后
    public static JsonResult seckillBlock(String randCode,
                                   SeckillOrderAddDTO seckillOrderAddDTO,
                                   BlockException blockException){
        log.warn("一个方法已经限流了!");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,"服务器忙,请稍后再试");
    }
}
