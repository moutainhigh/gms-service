package com.zs.gms.service.init;

import com.zs.gms.common.entity.GmsConstant;
import com.zs.gms.common.entity.RedisKeyPool;
import com.zs.gms.common.entity.StaticConfig;
import com.zs.gms.common.interfaces.MarkInterface;
import com.zs.gms.common.message.MessageEntry;
import com.zs.gms.common.message.MessageFactory;
import com.zs.gms.common.service.DelayedService;
import com.zs.gms.common.service.RedisService;
import com.zs.gms.common.utils.GmsUtil;
import com.zs.gms.entity.mapmanager.Point;
import com.zs.gms.entity.vehiclemanager.Excavator;
import com.zs.gms.entity.terminalmanager.Gps;
import com.zs.gms.entity.vehiclemanager.Barney;
import com.zs.gms.service.vehiclemanager.ExcavatorService;
import com.zs.gms.service.terminalmanager.GpsService;
import com.zs.gms.service.vehiclemanager.BarneyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据同步
 */
@Slf4j
public class SyncRedisData implements MarkInterface {

    @Autowired
    @Lazy
    private BarneyService barneyService;

    @Autowired
    @Lazy
    private ExcavatorService excavatorService;

    @Autowired
    @Lazy
    private GpsService gpsService;

    /**
     * 同步车辆数据到redis
     */
    public void syncBarneys() {
        List<Barney> vehicles = barneyService.getAllVehicles();
        Map<String, Object> valueMap = new HashMap<>();
        for (Barney barney : vehicles) {
            valueMap.put(barney.getVehicleNo().toString(), barney.getIp());
        }
        execute(valueMap, RedisKeyPool.VEH_ID_IP);
        //触发通信节点重新加载车辆ip
        MessageEntry entry = MessageFactory.createMessageEntry(GmsConstant.VAP);
        entry.setHttp(false);
        entry.setAfterHandle(() -> {
            String returnData = entry.getReturnData();
            log.debug("触发通信节点重新加载车辆ip:{}", returnData);
        });
        MessageFactory.getVapMessage().sendMessageNoResWithID(entry.getMessageId(), "ReLoadVehIp", "");
    }

    /**
     * 同步车辆基本信息到redis
     */
    public void syncBarneyBaseInfos() {
        List<Map<String, Object>> barneyBaseInfos = barneyService.getBarneyBaseInfos();
        Map<String, Object> valueMap = new HashMap<>();
        for (Map<String, Object> baseInfo : barneyBaseInfos) {
            valueMap.put(baseInfo.getOrDefault("VEHICLENO", "").toString(), GmsUtil.toJson(baseInfo));
        }
        execute(valueMap, RedisKeyPool.VEH_BASE_INFO);
    }

    /**
     * 同步挖掘机数据到redis
     */
    public void syncExcavators() {
        List<Excavator> excavators = excavatorService.getExcavators();
        Map<String, Object> valueMap = new HashMap<>();
        for (Excavator excavator : excavators) {
            valueMap.put(excavator.getExcavatorNo().toString(), excavator.getIp1());
        }
        execute(valueMap, RedisKeyPool.EXC_ID_IP);
    }

    /**
     * 同步终端数据到redis
     */
    public void syncTerminals() {
        List<Gps> allGps = gpsService.getAllGps();
        Map<String, Object> valueMap = new HashMap<>();
        for (Gps gps : allGps) {
            valueMap.put(gps.getGpsNo().toString(), gps.getIp());
        }
        execute(valueMap, RedisKeyPool.GPS_ID_IP);
    }

    private static void execute(Map<String, Object> valueMap, String key) {
        RedisTemplate<String, Object> template = RedisService.getTemplate(StaticConfig.KEEP_DB);
        template.delete(key);
        HashOperations<String, String, Object> hashOperations = template.opsForHash();
        hashOperations.putAll(key, valueMap);
    }

    @PostConstruct
    @Override
    public void execute() {
        DelayedService.addTask(() -> {
            syncBarneys();
            syncBarneyBaseInfos();
            syncExcavators();
            syncTerminals();
        }, 3000).withDesc("执行redis数据同步");
    }
}
