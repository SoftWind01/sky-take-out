package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.config.WebSocketConfiguration;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    ShoppingCartMapper shoppingCartMapper;
    @Autowired
    AddressBookMapper addressBookMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    WeChatPayUtil wechatPayUtil;
    private Orders orders;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况检查
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list == null || list.size() == 0){
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //构造Order对象
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());
        this.orders = orders;
        //向订单表插入数据
        orderMapper.insert(orders);

        List<OrderDetail> orderDetails = new ArrayList<OrderDetail>();
        for(ShoppingCart cart:list){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();
        shoppingCartMapper.clean(BaseContext.getCurrentId());
        return orderSubmitVO;
    }

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

/*        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());

        Map map=new HashMap();
        map.put("type",1);
        map.put("orderId",orders.getId());
        map.put("content","订单号"+this.orders.getId());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page= orderMapper.page(ordersPageQueryDTO);
        List<OrderVO> orderVOS = new ArrayList<>();
        if(page!=null && page.getTotal()>0){
            for(Orders order:page){
                Long orderId = order.getId();
                List<OrderDetail> orderDetailList=orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                orderVOS.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),orderVOS);
    }

    @Override
    public OrderVO orderDetail(Long id) {
        Orders orders=orderMapper.getByOrderId(id);
        List<OrderDetail> orderDetailList=orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void cancleOrder(Long id) {
        Integer OrderPaidStatus = Orders.UN_PAID;//支付状态，未支付
        Integer OrderStatus = Orders.CANCELLED;  //订单状态，取消
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, id);
    }

    @Override
    public void repetionOrder(Long id) {
        List<OrderDetail> orderDetailList=orderDetailMapper.getByOrderId(id);
        if(orderDetailList!=null && orderDetailList.size()>0){
            for(OrderDetail orderDetail:orderDetailList){
                ShoppingCart shoppingCart=new ShoppingCart();
                shoppingCart.setUserId(BaseContext.getCurrentId());
                BeanUtils.copyProperties(orderDetail, shoppingCart);
                shoppingCartMapper.insert(shoppingCart);
            }
        }
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        List<OrderVO> list=new ArrayList<>();
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersList=orderMapper.page(ordersPageQueryDTO);
        if(ordersList!=null && ordersList.size()>0){
            List<String> nameList=new ArrayList<>();

            for(Orders order:ordersList){
                OrderVO orderVO=new OrderVO();
                List<OrderDetail> orderDetailList= orderDetailMapper.getByOrderId(order.getId());
                if(orderDetailList!=null && orderDetailList.size()>0){
                    for(OrderDetail orderDetail:orderDetailList){
                           nameList.add(orderDetail.getName());
                    }
                }
                String orderDishes=String.join(",",nameList);
                orderVO.setOrderDishes(orderDishes);
                BeanUtils.copyProperties(order, orderVO);
                list.add(orderVO);
            }
        }
        return new PageResult(ordersList.getTotal(),list);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO=new OrderStatisticsVO();
        int toBeConfirmed=orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        int confrimed=orderMapper.countByStatus(Orders.CONFIRMED);
        int deliveryInProgress=orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confrimed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders=new Orders();
        orders.setStatus(Orders.CONFIRMED);
        orders.setId(ordersConfirmDTO.getId());
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders=new Orders();
        orders.setStatus(Orders.CANCELLED);
        orders.setId(ordersRejectionDTO.getId());
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());
        orderMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders=new Orders();
        orders.setStatus(Orders.CANCELLED);
        orders.setId(ordersCancelDTO.getId());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders=new Orders();
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orders.setId(id);
        orderMapper.update(orders);
    }

    @Override
    public void reminderOrder(Long id) {
        Orders orders=orderMapper.getByOrderId(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map=new HashMap<>();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号："+orders.getNumber());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }
}
