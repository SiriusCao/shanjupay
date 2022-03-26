package com.shanjupay.merchant.api;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.PageVO;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.StaffDTO;
import com.shanjupay.merchant.api.dto.StoreDTO;

public interface MerchantService {
    /**
     * 根据id查详细信息
     *
     * @param merchantId
     * @return
     */
    MerchantDTO queryMerchantById(Long merchantId);

    /**
     * 商户注册
     *
     * @param merchantDTO
     * @return
     */
    MerchantDTO createMerchant(MerchantDTO merchantDTO) throws BusinessException;

    /**
     * 商户服务资质申请
     *
     * @param merchantId  商户ID
     * @param merchantDTO 商户DTO
     * @throws BusinessException 自定义异常
     */
    void applyMerchant(Long merchantId, MerchantDTO merchantDTO) throws BusinessException;

    /**
     * 商户下新增门店
     *
     * @param storeDTO 门店
     * @return
     * @throws BusinessException
     */
    StoreDTO createStore(StoreDTO storeDTO) throws BusinessException;

    /**
     * 商户新增员工
     *
     * @param staffDTO 员工
     * @return
     * @throws BusinessException
     */
    StaffDTO createStaff(StaffDTO staffDTO) throws BusinessException;

    /**
     * 为门店设置管理员
     *
     * @param storeId 门店
     * @param staffId 员工
     * @throws BusinessException
     */
    void bindStaffToStore(Long storeId, Long staffId) throws BusinessException;

    /**
     * 查询租户下的商户
     *
     * @param tenantId 租户ID
     * @return
     * @throws BusinessException
     */
    MerchantDTO queryMerchantByTenantId(Long tenantId) throws BusinessException;

    /**
     * 分页条件查询商户下门店
     *
     * @param storeDTO
     * @param pageNo
     * @param pageSize
     * @return
     */
    PageVO<StoreDTO> queryStoreByPage(StoreDTO storeDTO, Integer pageNo, Integer pageSize);

    /**
     * 查询门店是否属于某商户
     *
     * @param storeId
     * @param merchantId
     * @return
     * @throws BusinessException
     */
    Boolean queryStoreInMerchant(Long storeId, Long merchantId) throws BusinessException;

}
