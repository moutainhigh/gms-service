package com.zs.gms.service.monitor.schdeule;

import com.zs.gms.common.entity.GmsConstant;
import com.zs.gms.common.entity.GmsResponse;
import com.zs.gms.common.entity.RedisKey;
import com.zs.gms.common.message.MessageEntry;
import com.zs.gms.service.vehiclemanager.VehicleService;
import com.zs.gms.common.configure.EventPublisher;
import com.zs.gms.common.entity.MessageEvent;
import com.zs.gms.common.interfaces.RedisListener;
import com.zs.gms.common.message.EventType;
import com.zs.gms.common.message.MessageFactory;
import com.zs.gms.common.properties.StateProperties;
import com.zs.gms.common.service.websocket.FunctionEnum;
import com.zs.gms.common.service.RedisService;
import com.zs.gms.common.service.websocket.WsUtil;
import com.zs.gms.common.utils.GmsUtil;
import com.zs.gms.common.utils.SpringContextUtil;
import com.zs.gms.entity.monitor.LiveInfo;
import com.zs.gms.entity.monitor.Vertex;
import com.zs.gms.service.monitor.LiveInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.http.HttpStatus;

import java.util.*;

/**
 * 实时数据处理
 */
@Slf4j
public class LiveVapHandle implements RedisListener {

    private static StateProperties stateProperties;

    private static ListOperations<String, Object> listOperations;

    private static VehicleService vehicleService;

    private static LiveInfoService liveInfoService;

    private static LiveVapHandle instance = new LiveVapHandle();

    static {
        stateProperties = SpringContextUtil.getBean(StateProperties.class);
        vehicleService = SpringContextUtil.getBean(VehicleService.class);
        liveInfoService = SpringContextUtil.getBean(LiveInfoService.class);
        listOperations = RedisService.listOperations(RedisService.getTemplate(GmsConstant.MONITOR_DB));
    }

    public static LiveVapHandle getInstance() {
        return instance;
    }


    /**
     * 处理所有车辆监听数据
     */
    public static void handleVehMessage(String key) {
        String prefix = key.substring(0, key.lastIndexOf("_") + 1);
        String vehicleNo = subVehicleNo(key);
        Integer userId = vehicleService.getUserIdByVehicleNo(Integer.valueOf(vehicleNo));
        if (userId == null) {
            log.error("不存在的车辆编号或者车辆没有分配");
            return;
        }
        switch (prefix) {
            case RedisKey.VAP_BASE_PREFIX:
                //车辆基本信息
                LiveInfo liveInfo = GmsUtil.getMessage(key,LiveInfo.class);
                StatusMonitor.getInstance().delegateStatus(liveInfo);
                WsUtil.sendMessage(userId.toString(), GmsUtil.toJsonIEnum(liveInfo), FunctionEnum.console, Integer.valueOf(vehicleNo));
                WsUtil.sendMessage(userId.toString(), GmsUtil.toJsonIEnum(liveInfo), FunctionEnum.vehicle);
                break;
            case RedisKey.VAP_PATH_PREFIX:
                //交互式路径请求
                if (MessageFactory.containMessageEntry(vehicleNo)) {
                    log.debug("交互式路径请求数据解析");
                    Map<String, Object> globalPath = getGlobalPath(key);
                    EventPublisher.publish(new MessageEvent(new Object(), GmsUtil.toJson(globalPath), vehicleNo, EventType.httpRedis));
                }
                //全局路径
                if (WsUtil.isNeed(FunctionEnum.globalPath)) {
                    Map<String, Object> globalPath = getGlobalPath(key);
                    WsUtil.sendMessage(userId.toString(), GmsUtil.toJson(globalPath), FunctionEnum.globalPath);
                }
                break;
            case RedisKey.VAP_LIST_PREFIX:
                if (WsUtil.isNeed(FunctionEnum.collectMap, vehicleNo)) {
                    List points = collectMap(key);
                    WsUtil.sendMessage(userId.toString(), GmsUtil.toJson(points), FunctionEnum.collectMap, Integer.valueOf(vehicleNo));
                }
                break;
            case RedisKey.VAP_TRAIL_PREFIX:
                if (WsUtil.isNeed(FunctionEnum.trail, vehicleNo)) {
                    Map<String, Object> trailPath = getTrailPath(key);
                    WsUtil.sendMessage(userId.toString(), GmsUtil.toJson(trailPath), FunctionEnum.trail, Integer.valueOf(vehicleNo));
                }
                break;
            default:
                break;
        }
    }

    public static Map<String, Object> getTrailPath(String key) {
        return getGlobalPath(key);
    }

    /**
     * 获取全局路径
     */
    public static Map<String, Object> getGlobalPath(String key) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            String[] strs = RedisService.get(GmsConstant.MONITOR_DB, key).toString().split(",");
            if (strs.length < 4) {
                return resultMap;
            }
            int len = convertToInt(strs[3]);
            int strLen = 4;
            List<Vertex> list = new ArrayList<>(len);
            Vertex vertex;
            for (int i = 0; i < len; i++) {
                vertex = new Vertex();
                vertex.setX(convertToDouble(strs[strLen]));
                vertex.setY(convertToDouble(strs[strLen + 1]));
                vertex.setZ(convertToDouble(strs[strLen + 2]));
                vertex.setType(convertToInt(strs[strLen + 3]));
                vertex.setDirection(convertToFloat(strs[strLen + 4]));
                vertex.setSlope(convertToFloat(strs[strLen + 5]));
                vertex.setCurvature(convertToFloat(strs[strLen + 6]));
                vertex.setLeftDistance(convertToDouble(strs[strLen + 7]));
                vertex.setRightDistance(convertToDouble(strs[strLen + 8]));
                vertex.setMaxSpeed(convertToDouble(strs[strLen + 9]));
                vertex.setSpeed(convertToDouble(strs[strLen + 10]));
                vertex.setS(convertToDouble(strs[strLen + 11]));
                vertex.setReverse(convertToBool(strs[strLen + 12]));
                list.add(vertex);
                strLen += 13;
            }
            resultMap.put("no", convertToInt(strs[0]));
            resultMap.put("vehicleId", convertToInt(strs[1]));
            resultMap.put("status", convertToInt(strs[2]));//1超时，0正常，负数为不能规划该路径
            resultMap.put("vertex_num", convertToInt(strs[3]));
            resultMap.put("data", list);
            handResult(convertToInt(strs[1]),convertToInt(strs[2]));
        } catch (Exception e) {
            log.error("全局路径解析失败", e);
        }
        return resultMap;
    }

    public static void handResult(int vehicleId, int status) {
        MessageEntry entry = MessageFactory.getMessageEntry(String.valueOf(vehicleId));
        if (null != entry) {
            GmsResponse gmsResponse = entry.getMessage().getGmsResponse();//503
            switch (status) {
                case 0:
                    break;
                case 1:
                    gmsResponse.message("调度请求超时");
                default:
                    gmsResponse.code(HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
    }


    /**
     * 截取车辆编号
     */
    private static String subVehicleNo(String key) {
        if (key.contains("_")) {
            return key.substring(key.lastIndexOf("_") + 1);
        }
        return "";
    }

    /**
     * 地图采集
     */
    public static List collectMap(String key) {
        List<Object> Jsons = listOperations.range(key, 0, -1);//获取所有元素
        if (!CollectionUtils.isEmpty(Jsons)) {
            listOperations.trim(key, 1, 0);//清空列表
            return Jsons;
        }
        return null;
    }


    private static int convertToInt(String str) {
        return Integer.valueOf(str);
    }

    private static double convertToDouble(String str) {
        return Double.valueOf(str);
    }

    private static Float convertToFloat(String str) {
        return Float.valueOf(str);
    }

    private static Boolean convertToBool(String str) {
        return Boolean.valueOf(str);
    }

    @Override
    public void listener(String key) {
        handleVehMessage(key);
    }
}