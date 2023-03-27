package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

//秒杀降级
@Slf4j
public class SeckillFallback {
    //降级方法可以和控制器方法一致 也可以添加throwable类型的参数 如果想知道是什么异常导致了服务降级 最好还是声明该错误
    public static JsonResult seckillFallBack(String randCode,
                                             SeckillOrderAddDTO seckillOrderAddDTO,
                                             Throwable throwable){
        log.warn("一个请求降级了!");
        throwable.printStackTrace();
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,"服务器异常 异常信息为:"+throwable.getMessage());
    }
}
