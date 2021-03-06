package com.zs.gms.controller.vehiclemanager;

import com.zs.gms.common.annotation.Mark;
import com.zs.gms.common.entity.WhetherEnum;
import com.zs.gms.common.utils.GmsUtil;
import com.zs.gms.entity.vehiclemanager.Barney;
import com.zs.gms.entity.vehiclemanager.BarneyType;
import com.zs.gms.service.init.SyncRedisData;
import com.zs.gms.service.vehiclemanager.BarneyService;
import com.zs.gms.common.annotation.Log;
import com.zs.gms.common.annotation.MultiRequestBody;
import com.zs.gms.common.controller.BaseController;
import com.zs.gms.common.entity.GmsResponse;
import com.zs.gms.common.entity.QueryRequest;
import com.zs.gms.common.exception.GmsException;
import com.zs.gms.service.vehiclemanager.BarneyTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/vehicles")
@Validated
@Api(tags = {"车辆管理"},description = "vehicle Controller")
public class BarneyController extends BaseController {

    @Autowired
    private BarneyService barneyService;

    @Autowired
    private BarneyTypeService barneyTypeService;

    @Log("新增矿车")
    @Mark(value = "新增矿车",markImpl = SyncRedisData.class)
    @PostMapping("/barneys")
    @ApiOperation(value = "新增矿车", httpMethod = "POST")
    public GmsResponse addVehicle(@Valid @MultiRequestBody Barney barney) throws GmsException {
        try {
            if (this.barneyService.queryVehicleExistNo(barney.getVehicleNo())) {
                return new GmsResponse().message("该车辆编号已添加!").badRequest();
            }
            this.barneyService.addVehicle(barney);
            return new GmsResponse().message("新增矿车成功").success();
        } catch (Exception e) {
            String message = "新增矿车失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }

    @Log("获取矿车列表")
    @GetMapping("/barneys")
    @ApiOperation(value = "获取矿车列表", httpMethod = "GET")
    @ResponseBody
    public String getVehicleList(Barney barney, QueryRequest queryRequest) throws GmsException {
        try {
            Map<String, Object> dataTable = this.getDataTable(this.barneyService.getVehicleList(barney, queryRequest));
            return GmsUtil.toJsonIEnumDesc(new GmsResponse().data(dataTable).message("获取矿车列表成功").success());
        } catch (Exception e) {
            String message = "获取矿车列表失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }

    @Log("修改矿车信息")
    @Mark(value = "修改矿车信息",markImpl = SyncRedisData.class)
    @PutMapping("/barneys")
    @ApiOperation(value = "修改矿车信息", httpMethod = "PUT")
    public GmsResponse updateVehicle(@MultiRequestBody Barney barney) throws GmsException {
        try {
            Barney ba = barneyService.getBarneyById(barney.getVehicleId());
            if(null==ba){
                return new GmsResponse().message("该车辆信息不存在!").badRequest();
            }
            if(WhetherEnum.YES.equals(ba.getVehicleStatus())){
                return new GmsResponse().message("该车辆处于激活状态，不能修改!").badRequest();
            }
            this.barneyService.updateVehicle(barney);
            return new GmsResponse().message("修改矿车信息成功").success();
        } catch (Exception e) {
            String message = "修改矿车信息失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }

    @Log("删除矿车")
    @Mark(value = "删除矿车",markImpl = SyncRedisData.class)
    @DeleteMapping(value = "/barneys/{vehicleId}")
    @ApiOperation(value = "删除矿车", httpMethod = "DELETE")
    public GmsResponse deleteVehicle(@PathVariable(value = "vehicleId") Integer vehicleId) throws GmsException {
        try {
            this.barneyService.deleteVehicle(vehicleId);
            return new GmsResponse().message("删除矿车成功").success();
        } catch (Exception e) {
            String message = "删除矿车失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }


    /*@Log("启用矿车")
    @PutMapping(value = "/barneys/start/{vehicleId}")
    @ApiOperation(value = "启用矿车", httpMethod = "PUT")
    public GmsResponse startVehicle(@PathVariable(value = "vehicleId") Integer vehicleId) throws GmsException {
        try {
            if(!this.barneyService.queryVehicleExistId(vehicleId)){
                return new GmsResponse().message("矿车id不存在").badRequest();
            }
            this.barneyService.updateStatusByNo(vehicleId, WhetherEnum.YES);
            return new GmsResponse().message("启用矿车成功").success();
        } catch (Exception e) {
            String message = "启用矿车失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }

    @Log("停用矿车")
    @PutMapping(value = "/barneys/stop/{vehicleId}")
    @ApiOperation(value = "停用矿车", httpMethod = "PUT")
    public GmsResponse stopVehicle(@PathVariable(value = "vehicleId") Integer vehicleId) throws GmsException {
        try {
            if(!this.barneyService.queryVehicleExistId(vehicleId)){
                return new GmsResponse().message("矿车id不存在").badRequest();
            }
            this.barneyService.updateStatusByNo(vehicleId, WhetherEnum.NO);
            return new GmsResponse().message("停用矿车成功").success();
        } catch (Exception e) {
            String message = "停用矿车失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }*/

    @Log("获取矿车类型")
    @GetMapping("/barneys/types/{barneyTypeId}")
    @ApiOperation(value = "获取矿车列表", httpMethod = "GET")
    public GmsResponse getBarneyType(@PathVariable("barneyTypeId")Integer barneyTypeId) throws GmsException {
        try {
            BarneyType barneyType = barneyTypeService.getBarneyType(barneyTypeId);
            return new GmsResponse().data(barneyType).message("获取矿车类型成功").success();
        } catch (Exception e) {
            String message = "获取矿车类型失败";
            log.error(message, e);
            throw new GmsException(message);
        }
    }
}
