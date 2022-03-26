package com.shanjupay.merchant.convert;

import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.vo.MerchantDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MerchantDetailConvert {
    MerchantDetailConvert INSTANCE = Mappers.getMapper(MerchantDetailConvert.class);

    /**
     * vo转成DTO
     *
     * @param merchantDetailVO
     * @return
     */
    MerchantDTO vo2dto(MerchantDetailVO merchantDetailVO);

    /**
     * DTO转成VO
     *
     * @param merchantDTO
     * @return
     */
    MerchantDetailVO dto2vo(MerchantDTO merchantDTO);

}
