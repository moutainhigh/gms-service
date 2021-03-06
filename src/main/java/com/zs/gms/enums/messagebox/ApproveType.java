package com.zs.gms.enums.messagebox;

import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zs.gms.common.interfaces.Desc;
import com.zs.gms.service.mapmanager.impl.MapInfoServiceImpl;
import com.zs.gms.service.messagebox.ApproveInterface;
import com.zs.gms.service.mineralmanager.impl.AreaMineralServiceImpl;
import com.zs.gms.service.remote.impl.RemoteServiceImpl;

import java.io.Serializable;

public enum ApproveType implements IEnum, Desc {

    MAPPUBLISH("0","申请地图发布", MapInfoServiceImpl.class),
    MAPDELETE("1","申请地图删除", MapInfoServiceImpl.class),
    MAPINACTIVE("2","申请地图取消活跃状态", MapInfoServiceImpl.class),
    MINERALCHANGE("3","申请装载区矿种类型变更", AreaMineralServiceImpl.class),
    REMOTEACCESS("4","申请进入控制台", RemoteServiceImpl.class),
    OBSTACLEDELETE("5","申请删除障碍物", MapInfoServiceImpl.class);

    private String value;

    private String desc;

    /**
     * 审批处理类
     * */
    private Class<? extends ApproveInterface> handler;

    ApproveType(String value,String desc,Class<? extends ApproveInterface> handler){
        this.value=value;
        this.desc=desc;
        this.handler=handler;
    }

    @Override
    public Serializable getValue() {
        return value;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    public Class<? extends ApproveInterface> getHandler() {
        return handler;
    }
}
