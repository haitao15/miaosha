package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.PromoMapper;
import com.miaoshaproject.dataobject.Promo;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    PromoMapper promoMapper;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        Promo promo = promoMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromPromo(promo);
        if (promoModel == null) {
            return null;
        }

        DateTime now = new DateTime();
        if(promoModel.getDateTime().isAfterNow()) {
            promoModel.setStatus(1);
        }else if(promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    private PromoModel convertFromPromo(Promo promo) {
        if (promo == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promo, promoModel);
        promoModel.setDateTime(new DateTime(promo.getStartDate()));
        promoModel.setPromoItemPice(new BigDecimal(promo.getPromoItemPrice()));
        promoModel.setEndTime(new DateTime(promo.getEndDate()));
        return promoModel;
    }
}
