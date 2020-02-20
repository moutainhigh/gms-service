package com.zs.gms.service.monitor.schdeule;

import com.zs.gms.common.utils.GmsUtil;
import com.zs.gms.enums.monitor.Desc;
import com.zs.gms.enums.monitor.DispatchStateEnum;
import com.zs.gms.service.vehiclemanager.VehicleService;
import com.zs.gms.service.vehiclemanager.impl.VehicleServiceImpl;
import com.zs.gms.common.utils.LimitQueue;
import com.zs.gms.common.utils.SpringContextUtil;
import com.zs.gms.entity.monitor.DispatchStatus;
import com.zs.gms.entity.monitor.VehicleStatus;
import com.zs.gms.service.monitor.DispatchStatusService;
import com.zs.gms.service.monitor.impl.DispatchStatusServiceImpl;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆调度状态处理
 */
@Slf4j
public class VehicleDispatchStatusHandle extends AbstractVehicleStatusHandle {

    private final static Integer LIMIT_QUEUE_SIZE = 10;
    private Map<Integer, LimitQueue<DispatchStatus>> historyStatusMap;
    private DispatchStatusService dispatchStatusService;
    private VehicleService vehicleService;

    public VehicleDispatchStatusHandle() {
        super();
        historyStatusMap = new ConcurrentHashMap<>();
        dispatchStatusService = SpringContextUtil.getBean(DispatchStatusServiceImpl.class);
        vehicleService = SpringContextUtil.getBean(VehicleServiceImpl.class);
    }

    @Override
    public void handleStatus(VehicleStatus vehicleStatus) {
        super.handleStatus(vehicleStatus);
    }


    @Override
    public void changed(VehicleStatus vehicleStatus) {
        log.debug("{}车辆调度状态改变:{}", vehicleStatus.getVehicleId(), ((Desc)(vehicleStatus.getStatus())).getDesc());
        super.changed(vehicleStatus);
    }

    @Override
    public void save(VehicleStatus vehicleStatus) {
        DispatchStatus dispatchStatus = getBean(vehicleStatus);
        if(addToQueue(dispatchStatus)){//第一次数据不保存
            dispatchStatusService.addDispatchStatus(dispatchStatus);
        }
    }

    private DispatchStatus getBean(VehicleStatus vehicleStatus){
        DispatchStatus dispatchStatus = new DispatchStatus();
        Integer vehicleId = vehicleStatus.getVehicleId();
        Integer userId = vehicleService.getUserIdByVehicleNo(vehicleId);
        dispatchStatus.setCreateTime(vehicleStatus.getCreateTime());
        dispatchStatus.setStatus((DispatchStateEnum)(vehicleStatus.getStatus()));
        dispatchStatus.setVehicleId(vehicleId);
        dispatchStatus.setUserId(userId);
        return dispatchStatus;
    }

    @Override
    public void overtime(VehicleStatus vehicleStatus) {
        addToQueue(getBean(vehicleStatus));
    }

    /**
     * 添加数据到队列
     */
    public boolean addToQueue(DispatchStatus dispatchStatus) {
        boolean result=false;
        Integer vehicleId = dispatchStatus.getVehicleId();
        if (historyStatusMap.containsKey(vehicleId)) {
            historyStatusMap.get(vehicleId).add(dispatchStatus);
            result= true;
        } else {
            LimitQueue<DispatchStatus> limitQueue = new LimitQueue<DispatchStatus>(LIMIT_QUEUE_SIZE);
            limitQueue.add(dispatchStatus);
            historyStatusMap.put(vehicleId, limitQueue);

        }
        push(vehicleId);
        return result;
    }

    /**
     * 数据推送
     * */
    @Override
    public String push(Integer vehicleId) {
        if(historyStatusMap.containsKey(vehicleId)){
            LimitQueue<DispatchStatus> queues = historyStatusMap.get(vehicleId);
            return GmsUtil.toJson(queues);
        }
        return "";
    }
}