package com.shanjupay.merchant.convert;

import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.entity.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MerchantConvert {
    MerchantConvert INSTANCE= Mappers.getMapper(MerchantConvert.class);

    /**
     * Merchant向MerchantDTO转换
     * @param merchant
     * @return
     */
    MerchantDTO entity2dto(Merchant merchant);

    /**
     * MerchantDTO向Merchant转换
     * @param merchantDTO
     * @return
     */
    Merchant dto2entity(MerchantDTO merchantDTO);


}
