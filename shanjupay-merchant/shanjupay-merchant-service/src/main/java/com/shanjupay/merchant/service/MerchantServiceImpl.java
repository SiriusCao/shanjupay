package com.shanjupay.merchant.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.domain.PageVO;
import com.shanjupay.common.util.PhoneUtil;
import com.shanjupay.common.util.StringUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.StaffDTO;
import com.shanjupay.merchant.api.dto.StoreDTO;
import com.shanjupay.merchant.convert.MerchantConvert;
import com.shanjupay.merchant.convert.StaffConvert;
import com.shanjupay.merchant.convert.StoreConvert;
import com.shanjupay.merchant.entity.Merchant;
import com.shanjupay.merchant.entity.Staff;
import com.shanjupay.merchant.entity.Store;
import com.shanjupay.merchant.entity.StoreStaff;
import com.shanjupay.merchant.mapper.MerchantMapper;
import com.shanjupay.merchant.mapper.StaffMapper;
import com.shanjupay.merchant.mapper.StoreMapper;
import com.shanjupay.merchant.mapper.StoreStaffMapper;
import com.shanjupay.user.api.TenantService;
import com.shanjupay.user.api.dto.tenant.CreateTenantRequestDTO;
import com.shanjupay.user.api.dto.tenant.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class MerchantServiceImpl implements MerchantService {
    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private StoreStaffMapper storeStaffMapper;

    @Reference
    private TenantService tenantService;

    @Override
    public MerchantDTO queryMerchantById(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        return MerchantConvert.INSTANCE.entity2dto(merchant);
    }


    @Override
    @Transactional
    public MerchantDTO createMerchant(MerchantDTO merchantDTO) throws BusinessException {
        //校检是否为空
        if (merchantDTO == null) {
            throw new BusinessException(CommonErrorCode.E_100108);
        }
        if (StringUtil.isBlank(merchantDTO.getUsername())) {
            throw new BusinessException(CommonErrorCode.E_100110);
        }
        if (StringUtil.isBlank(merchantDTO.getMobile())) {
            throw new BusinessException(CommonErrorCode.E_100112);
        }
        if (StringUtil.isBlank(merchantDTO.getPassword())) {
            throw new BusinessException(CommonErrorCode.E_100111);
        }
        //校检合法性
        if (!PhoneUtil.isMatches(merchantDTO.getMobile())) {
            throw new BusinessException(CommonErrorCode.E_100109);
        }
        //校检唯一性
        LambdaQueryWrapper<Merchant> lambdaQueryWrapper = new LambdaQueryWrapper<Merchant>().eq(
                Merchant::getMobile,
                merchantDTO.getMobile()
        );
        Integer count = merchantMapper.selectCount(lambdaQueryWrapper);
        if (count > 0) {
            throw new BusinessException(CommonErrorCode.E_100113);
        }
        //调用SaaS接口
        //构建调用参数
        /**
         1、手机号

         2、账号

         3、密码

         4、租户类型：shanju-merchant

         5、默认套餐：shanju-merchant

         6、租户名称，同账号名

         */
        CreateTenantRequestDTO createTenantRequestDTO = new CreateTenantRequestDTO();
        createTenantRequestDTO.setUsername(merchantDTO.getUsername());
        createTenantRequestDTO.setPassword(merchantDTO.getPassword());
        createTenantRequestDTO.setMobile(merchantDTO.getMobile());
        createTenantRequestDTO.setTenantTypeCode("shanju-merchant");//租户类型
        createTenantRequestDTO.setBundleCode("shanju-merchant");//套餐，根据套餐进行分配权限
        createTenantRequestDTO.setName(merchantDTO.getUsername());//租户名称，和账号名一样

        //如果租户在SaaS已经存在，SaaS直接 返回此租户的信息，否则进行添加
        TenantDTO tenantAndAccount = tenantService.createTenantAndAccount(createTenantRequestDTO);
        if (tenantAndAccount == null || tenantAndAccount.getId() == null) {
            throw new BusinessException(CommonErrorCode.E_200012);
        }

        //租户的id
        Long tenantId = tenantAndAccount.getId();
        //租户id在商户表唯一
        //根据租户id从商户表查询，如果存在记录则不允许添加商户
        LambdaQueryWrapper<Merchant> lambdaQueryWrapper1 =
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getTenantId, tenantId);
        Integer count1 = merchantMapper.selectCount(lambdaQueryWrapper);
        if (count1 > 0) {
            throw new BusinessException(CommonErrorCode.E_200017);
        }

        //MerchantDTO向Merchant转换
        Merchant merchant = MerchantConvert.INSTANCE.dto2entity(merchantDTO);
        //设置所对应的租户的Id
        merchant.setTenantId(tenantId);
        //设置审核状态0‐未申请,1‐已申请待审核,2‐审核通过,3‐审核拒绝
        merchant.setAuditStatus("0");
        //保存商户
        merchantMapper.insert(merchant);

        //新增门店
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setStoreName("根门店");
        storeDTO.setMerchantId(merchant.getId());//商户id
        storeDTO = createStore(storeDTO);

        //新增员工
        StaffDTO staffDTO = new StaffDTO();
        staffDTO.setMobile(merchantDTO.getMobile());//手机号
        staffDTO.setUsername(merchantDTO.getUsername());//账号
        staffDTO.setStoreId(storeDTO.getId());//员所属门店id
        staffDTO.setMerchantId(merchant.getId());//商户id
        staffDTO = createStaff(staffDTO);

        //为门店设置管理员
        bindStaffToStore(storeDTO.getId(), staffDTO.getId());

        //将entity转成 dto
        return MerchantConvert.INSTANCE.entity2dto(merchant);
    }

    @Override
    public void applyMerchant(Long merchantId, MerchantDTO merchantDTO) throws BusinessException {
        //判断是否为空
        if (merchantDTO == null || merchantId == null) {
            throw new BusinessException(CommonErrorCode.E_100108);
        }
        //判断是否已经注册
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(CommonErrorCode.E_200002);
        }
        //提交申请资质信息
        Merchant merchantUpdate = MerchantConvert.INSTANCE.dto2entity(merchantDTO);
        merchantUpdate.setAuditStatus("1");//已申请待审核
        merchantUpdate.setId(merchant.getId());
        merchantUpdate.setMobile(merchant.getMobile());
        merchantUpdate.setTenantId(merchant.getTenantId());
        merchantMapper.updateById(merchantUpdate);
    }

    @Override
    public StoreDTO createStore(StoreDTO storeDTO) throws BusinessException {
        //将DTO转换成entity进行持久化操作
        Store store = StoreConvert.INSTANCE.dto2entity(storeDTO);
        log.info("线下新增门店" + JSON.toJSONString(store));
        //新增商户门店
        storeMapper.insert(store);
        //将entity转换DTO成返回
        return StoreConvert.INSTANCE.entity2dto(store);
    }

    @Override
    public StaffDTO createStaff(StaffDTO staffDTO) throws BusinessException {
        //1.校验手机号格式及是否存在
        String mobile = staffDTO.getMobile();
        if (StringUtil.isBlank(mobile)) {
            throw new BusinessException(CommonErrorCode.E_100112);
        }
        if (isExistStaffByMobile(mobile, staffDTO.getMerchantId())) {
            throw new BusinessException(CommonErrorCode.E_100113);
        }
        //2.校验用户名是否为空
        String username = staffDTO.getUsername();
        if (StringUtil.isBlank(username)) {
            throw new BusinessException(CommonErrorCode.E_100110);
        }
        if (isExistStaffByUserName(username, staffDTO.getMerchantId())) {
            throw new BusinessException(CommonErrorCode.E_100114);
        }
        //将DTO装换成entity
        Staff staff = StaffConvert.INSTANCE.dto2entity(staffDTO);
        //商户新员工
        staffMapper.insert(staff);
        log.info("商户下新增员工:" + username + "  手机号:" + mobile);
        //将entity转换成DTO然后返回
        return StaffConvert.INSTANCE.entity2dto(staff);
    }

    /**
     * 根据手机号判断员工是否已在指定商户存在
     *
     * @param mobile     手机号码
     * @param merchantId 商户Id
     * @return
     */
    public boolean isExistStaffByMobile(String mobile, Long merchantId) {
        LambdaQueryWrapper<Staff> lambdaQueryWrapper =
                new LambdaQueryWrapper<Staff>()
                        .eq(Staff::getMobile, mobile)
                        .eq(Staff::getMerchantId, merchantId);
        // 根据手机号判断员工是否已在指定商户存在
        Integer count = staffMapper.selectCount(lambdaQueryWrapper);
        return count > 0;
    }

    /**
     * 根据账号判断员工是否已在指定商户存在
     *
     * @param username   用户名
     * @param merchantId 商户Id
     * @return
     */
    public boolean isExistStaffByUserName(String username, Long merchantId) {
        LambdaQueryWrapper<Staff> lambdaQueryWrapper =
                new LambdaQueryWrapper<Staff>()
                        .eq(Staff::getUsername, username)
                        .eq(Staff::getMerchantId, merchantId);
        //根据账号判断员工是否已在指定商户存在
        Integer count = staffMapper.selectCount(lambdaQueryWrapper);
        return count > 0;
    }

    @Override
    public void bindStaffToStore(Long storeId, Long staffId) throws BusinessException {
        StoreStaff storeStaff = new StoreStaff();
        storeStaff.setStoreId(storeId);
        storeStaff.setStaffId(staffId);
        storeStaffMapper.insert(storeStaff);
    }

    @Override
    public MerchantDTO queryMerchantByTenantId(Long tenantId) throws BusinessException {
        LambdaQueryWrapper<Merchant> lambdaQueryWrapper =
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getTenantId, tenantId);
        Merchant merchant = merchantMapper.selectOne(lambdaQueryWrapper);
        return MerchantConvert.INSTANCE.entity2dto(merchant);
    }

    @Override
    public PageVO<StoreDTO> queryStoreByPage(StoreDTO storeDTO, Integer pageNo, Integer pageSize) {
        //构造分页
        IPage<Store> page = new Page<>(pageNo, pageSize);
        //构造条件
        LambdaQueryWrapper<Store> lambdaQueryWrapper =
                new LambdaQueryWrapper<Store>().eq(Store::getMerchantId, storeDTO.getMerchantId());
        //执行查询
        IPage<Store> storeIPage = storeMapper.selectPage(page, lambdaQueryWrapper);
        //封装结果并返回
        List<StoreDTO> storeDTOS = StoreConvert.INSTANCE.listentity2dto(storeIPage.getRecords());
        return new PageVO<StoreDTO>(storeDTOS, storeIPage.getTotal(), pageNo, pageSize);
    }

    @Override
    public Boolean queryStoreInMerchant(Long storeId, Long merchantId) throws BusinessException {
        LambdaQueryWrapper<Store> lambdaQueryWrapper=
                new LambdaQueryWrapper<Store>()
                .eq(Store::getId,storeId)
                .eq(Store::getMerchantId,merchantId);
        Integer count = storeMapper.selectCount(lambdaQueryWrapper);
        return count>0;
    }
}
