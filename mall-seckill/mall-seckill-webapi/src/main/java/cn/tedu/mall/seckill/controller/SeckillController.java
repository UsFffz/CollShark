package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.exception.SeckillBlockHandler;
import cn.tedu.mall.seckill.exception.SeckillFallback;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
@Api(tags = "秒杀订单提交")
public class SeckillController {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ISeckillService seckillService;

    @PostMapping("/{randCode}")
    @ApiOperation("验证码随机参数验证")
    @PreAuthorize("hasRole('user')")     //限流方法所在类
    @SentinelResource(value = "seckill",blockHandlerClass = SeckillBlockHandler.class,blockHandler = "seckillBlock",
                      fallbackClass = SeckillFallback.class,fallback = "seckillFallBack")
    public JsonResult<SeckillCommitVO> commitSeckill(@PathVariable String randCode,
                                                     @Validated SeckillOrderAddDTO seckillOrderAddDTO){
        //获得spuID
        Long spuId = seckillOrderAddDTO.getSpuId();
        //从reids中获取spuId的随机码
        String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
        if (redisTemplate.hasKey(randCodeKey)){
            //如果redis中有key
            String redisRandCode = redisTemplate.boundValueOps(randCodeKey).get()+"";
            if (redisRandCode.equals(randCode)){
                return JsonResult.ok(seckillService.commitSeckill(seckillOrderAddDTO));
            }else {
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"秒杀码错误");
            }
        }else {
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"秒杀码错误");
        }
    }
}
