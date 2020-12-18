package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderMapper;
import com.miaoshaproject.dao.SequenceMapper;
import com.miaoshaproject.dataobject.Order;
import com.miaoshaproject.dataobject.Sequence;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SequenceMapper sequenceMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException {
        ItemModel itemModel = itemService.getItemById(itemId);
        if (itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品不存在");
        }
        UserModel userModel = userService.getUserById(userId);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户不存在");
        }
        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量不正确");
        }

        if (promoId != null) {
            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动不正确");
            } else if (itemModel.getPromoModel().getStatus().intValue() != 2) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动未开始");
            }
        }

        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH, "库存不足");
        }

        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPice());
        }else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        orderModel.setId(generateOrderNo());
        orderModel.setPromoId(promoId);
        Order order = convertFromOrderModel(orderModel);
        orderMapper.insertSelective(order);
        itemService.increaseSales(itemId, amount);
        return orderModel;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo() {
        int sequnce = 0;
        StringBuilder stringBuilder = new StringBuilder();
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(localDateTime);

        Sequence order_info = sequenceMapper.getSequenceByName("order_info");
        sequnce = order_info.getCurrentValue();
        order_info.setCurrentValue(order_info.getCurrentValue() + order_info.getStep());
        sequenceMapper.updateByPrimaryKey(order_info);

        String s = String.valueOf(sequnce);
        for (int i = 0; i < 6 - s.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(s);
        stringBuilder.append("00");
        return stringBuilder.toString();
    }

    private Order convertFromOrderModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        Order order = new Order();
        BeanUtils.copyProperties(orderModel, order);
        order.setItemPrice(orderModel.getItemPrice().doubleValue());
        order.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return order;
    }
}
