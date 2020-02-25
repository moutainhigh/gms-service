package com.zs.gms.service.vehiclemanager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.gms.entity.vehiclemanager.Barney;
import com.zs.gms.entity.vehiclemanager.UserVehicle;
import com.zs.gms.entity.vehiclemanager.BarneyVehicleType;
import com.zs.gms.mapper.vehiclemanager.BarneyMapper;
import com.zs.gms.service.vehiclemanager.UserBarneyService;
import com.zs.gms.service.vehiclemanager.BarneyService;
import com.zs.gms.service.vehiclemanager.BarneyVehicleTypeService;
import com.zs.gms.common.entity.GmsConstant;
import com.zs.gms.common.entity.QueryRequest;
import com.zs.gms.common.service.RedisService;
import com.zs.gms.common.utils.PropertyUtil;
import com.zs.gms.common.utils.SortUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "vehicles")
@Transactional(propagation = Propagation.SUPPORTS,readOnly = true,rollbackFor = Exception.class)
public class BarneyServiceImpl extends ServiceImpl<BarneyMapper, Barney> implements BarneyService {

    @Autowired
    @Lazy
    private UserBarneyService userBarneyService;

    @Autowired
    @Lazy
    private BarneyVehicleTypeService barneyVehicleTypeService;

    /**
     * 添加车辆
     * */
    @Override
    @Transactional
    public void addVehicle(Barney barney) {
        barney.setVehicleStatus(Barney.DEFAULT_STATUS);
        barney.setAddTime(new Date());
        this.save(barney);
        Integer userId = barney.getUserId();
        Integer vehicleId = barney.getVehicleId();
        Integer vehicleTypeId = barney.getVehicleTypeId();
        addVehicleVehicleType(vehicleTypeId,vehicleId);
        if(null!=userId){
            addUserVehicle(userId,vehicleId);
        }
    }

    /**
     * 修改车辆信息
     * */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "vehicles",key = "'getUserIdByVehicleNo'+#p0.vehicleNo")
    public void updateVehicle(Barney barney) {
        Integer userId = barney.getUserId();
        Integer vehicleId = barney.getVehicleId();
        //修改车辆归属
        if(null!=userId){
           userBarneyService.deteleByVehicleId(vehicleId);
           addUserVehicle(userId,vehicleId);
            barney.setUserId(null);
       }
        //修改车辆类型
        Integer vehicleTypeId = barney.getVehicleTypeId();
        if(null!=vehicleTypeId){
            barneyVehicleTypeService.deteleByVehicleId(vehicleId);
            addVehicleVehicleType(vehicleTypeId,vehicleId);
            barney.setVehicleTypeId(null);
        }
        if(!PropertyUtil.isAllFieldNull(barney,"vehicleId")){
            this.updateById(barney);
        }

    }

    /**
     * 添加用户车辆关系
     * */
    @Transactional
    public void addUserVehicle(Integer userId,Integer vehicleId){
        UserVehicle userVehicle=new UserVehicle();
        userVehicle.setUserId(userId);
        userVehicle.setVehicleId(vehicleId);
        userBarneyService.addUserVehicle(userVehicle);
    }

    /**
     * 批量分配用户车辆
     * */
    public void addUserVehicles(Integer userId,String vehicleIds){
        String[] ids = StringUtils.split(vehicleIds, StringPool.COMMA);
        for (String id : ids) {
            UserVehicle userVehicle=new UserVehicle();
            userVehicle.setUserId(userId);
            userVehicle.setVehicleId(Integer.valueOf(id));
            userBarneyService.addUserVehicle(userVehicle);
        }
        RedisService.deleteLikeKey(GmsConstant.KEEP_DB,"getUserIdByVehicleNo");//删除缓存数据
    }

    @Override
    public boolean isVehicleAllot(String vehicleIds) {
        return userBarneyService.isVehiclesAllot(vehicleIds);
    }

    /**
     * 添加车辆和车辆类型关系
     * */
    @Transactional
    public void addVehicleVehicleType(Integer vehicleTypeId,Integer vehicleId){
        BarneyVehicleType barneyVehicleType =new BarneyVehicleType();
        barneyVehicleType.setVehicleTypeId(vehicleTypeId);
        barneyVehicleType.setVehicleId(vehicleId);
        barneyVehicleTypeService.addVehicleVehicleType(barneyVehicleType);
    }

    /**
     *删除车辆信息
     * */
    @Override
    @Transactional
    public void deleteVehicle(String vehicleIds) {
        String[] ids = vehicleIds.split(StringPool.COMMA);
        this.baseMapper.deleteBatchIds(Arrays.asList(ids));
        userBarneyService.deteleByVehicleIds(ids);
        barneyVehicleTypeService.deteleByVehicleIdS(ids);
        RedisService.deleteLikeKey(GmsConstant.KEEP_DB,"getUserIdByVehicleNo");//删除缓存数据
    }

    /**
     * 分页获取车辆列表
     * @param queryRquest 分页对象
     * */
    @Override
    @Transactional
    public IPage<Barney> getVehicleList(Barney barney, QueryRequest queryRquest) {
        Page page=new Page();
        SortUtil.handlePageSort(queryRquest,page, GmsConstant.SORT_DESC,"VEHICLEID");
        return this.baseMapper.findVehicleListPage(page, barney);
    }

    /**
     * 根据用户id获取车辆列表
     * */
    @Override
    @Transactional
    public List<Barney> getVehicleListByUserId(Integer userId) {
        Barney barney =new Barney();
        barney.setUserId(userId);
        List<Barney> barneyList = this.baseMapper.findVehicleList(barney);
        return barneyList.stream().filter(v->{
            return v.getVehicleStatus().equals("1");//0停用，1再用
        }).collect(Collectors.toList());
    }

    /**
     * 根据车辆编号获取用户id
     * */
    @Override
    @Transactional
    @Cacheable(cacheNames = "vehicles",key = "'getUserIdByVehicleNo'+#p0",unless = "#result==null")
    public Integer getUserIdByVehicleNo(Integer vehicleNo) {
         return this.baseMapper.findUserIdByVehicleNo(vehicleNo);
    }

    /**
     * 根据车牌号是否已添加
     * */
    @Override
    @Transactional
    public boolean queryVhicleExist(Integer vhicleNo) {
        Integer count = this.baseMapper.selectCount(new LambdaQueryWrapper<Barney>().eq(Barney::getVehicleNo, vhicleNo));
        return count > 0? true : false;
    }

    @Override
    @Transactional
    public List<Integer> getAllVehicleNos() {
        LambdaQueryWrapper<Barney> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Barney::getVehicleStatus,"1");
        queryWrapper.select(Barney::getVehicleNo);
        List<Barney> barneys = this.baseMapper.selectList(queryWrapper);
        return barneys.stream().map(vehicle ->vehicle.getVehicleNo()).collect(Collectors.toList());
    }
}