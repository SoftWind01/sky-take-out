package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "店铺相关接口")
@Slf4j
@RestController("userShopController")
@RequestMapping("/user/shop")
public class ShopController {
    @Autowired
    RedisTemplate redisTemplate;

    private static final String KEY="SHOP_STATUS";

    @ApiOperation("查询店铺状态")
    @GetMapping("/status")
    public Result<Integer> getShopStatus() {
        log.info("获取店铺状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        return Result.success(status);
    }
}
