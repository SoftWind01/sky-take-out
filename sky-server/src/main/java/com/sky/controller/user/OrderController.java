package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.OrderDetail;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@Slf4j
@Api("订单接口")
@RequestMapping("/user/order")
public class OrderController {
    @Autowired
    OrderService orderService;

    @ApiOperation("订单提交")
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("订单提交：{}", ordersSubmitDTO);
        OrderSubmitVO  orderSubmitVO=orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    @ApiOperation("历史订单")
    @GetMapping("/historyOrders")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("历史订单查询：{}", ordersPageQueryDTO);
        PageResult pageResult= orderService.historyOrders(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @ApiOperation("订单详情")
    @GetMapping("orderDetail/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        log.info("订单详情:{}",id);
        OrderVO orderVO= orderService.orderDetail(id);
        return Result.success(orderVO);
    }

    @ApiOperation("取消订单")
    @PutMapping("/cancel/{id}")
    public Result cancleOrder(@PathVariable Long id) {
        log.info("取消订单:{}",id);
        orderService.cancleOrder(id);
        return Result.success();
    }

    @ApiOperation("再来一单")
    @PostMapping("/repetition/{id}")
    public Result repetitionOrder(@PathVariable Long id) {
        log.info("再来一单:{}",id);
        orderService.repetionOrder(id);
        return Result.success();
    }

    @ApiOperation("催单")
    @GetMapping("/reminder/{id}")
    public Result reminderOrder(@PathVariable Long id) {
        log.info("催单:{}",id);
        orderService.reminderOrder(id);
        return Result.success();
    }
}
