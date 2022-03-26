package com.shanjupay.merchant.convert;

import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.vo.MerchantRegisterVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MerchantRegisterConvert {
    MerchantRegisterConvert INSTANCE = Mappers.getMapper(MerchantRegisterConvert.class);

    /**
     * MerchantRegisterVo转成MerchantDTO
     * @param merchantRegisterVO
     * @return
     */
    MerchantDTO vo2dto(MerchantRegisterVO merchantRegisterVO);

    /**
     * MerchantDTO转成MerchantRegisterVO
     * @param merchantDTO
     * @return
     */
    MerchantRegisterVO dto2vo(MerchantDTO merchantDTO);
}
