package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Scheduled(cron ="0 * * * * ?")
    public void timeOutOrder(){
        log.info("超时订单处理");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList=orderMapper.getByStatusAndTime(Orders.UN_PAID,time);
        if(ordersList!=null&&ordersList.size()>0){
            for(Orders order:ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("支付超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 1 ? * ? ")
    public void deliveryOrder(){
        log.info("处理派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList=orderMapper.getByStatusAndTime(Orders.DELIVERY_IN_PROGRESS,time);
        if(ordersList!=null&&ordersList.size()>0){
            for(Orders order:ordersList){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }

    /**
     * 通过WebSocket每隔5秒向客户端发送消息
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void sendMessageToClient() {
        webSocketServer.sendToAllClient("这是来自服务端的消息：" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()));
    }
}
