package com.zs.gms.entity.mapmanager;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.*;
import com.zs.gms.common.handler.ObjectTypeHandler;
import com.zs.gms.common.handler.PointTypeHandler;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 地图信息
 */
@Data
@TableName(value = "t_map_info", autoResultMap = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapInfo implements Serializable {
    private static final long serialVersionUID = 8001139130832445615L;

    /**
     * 数据id
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;
    /**
     * 地图id,由地图模块创建
     */
    @TableField(value = "MAPID")
    private Integer mapId;

    @JsonProperty("map_id")
    public void setMapId(Integer mapId) {
        this.mapId = mapId;
    }

    @JsonProperty("mapId")
    public Integer getMapId() {
        return mapId;
    }

    /**
     * 地图名称
     */
    @NotBlank(message = "地图名称不能为空")
    @TableField(value = "NAME")
    private String name;

    /**
     * 坐标原点，utm坐标{x,y,z}
     */
    @NotNull(message = "坐标原点不能为空")
    @TableField(value = "COORDINATEORIGIN", typeHandler = PointTypeHandler.class)
    private Point coordinateOrigin;

    /**
     * 地图限速
     */
    @NotNull(message = "地图限速不能为空")
    @TableField(value = "SPEED")
    private Float speed;

    /**
     * 地图版本
     */
    @TableField(value = "VERSION", typeHandler = ObjectTypeHandler.class)
    private MapVersion version;

    /**
     * 地图缩略图路径
     */
    @TableField(value = "IMAGEPATH")
    private String imagePath;

    /**
     * 靠左/右行驶
     */
    @NotNull(message = "靠左/右行驶不能为空")
    @TableField(value = "LEFTDRING")
    private boolean leftDriving;

    /**
     * 底图文件绝对路径
     */
    @NotBlank(message = "底图文件绝对路径不能为空")
    @TableField(value = "BASEMAPPATH")
    private String baseMapPath;

    //////////////////////////////////////上面为地图模块所需信息/////////////////////////////////////////

    /**
     * 审批id
     */
    @TableField(value = "APPROVEID")
    private Integer approveId;

    /**
     * 地图创建时间
     */
    @TableField(value = "ADDTIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date addTime;

    /**
     * 地图修改时间
     */
    @TableField(value = "UPDATETIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;

    /**
     * 用户名
     */
    @TableField(value = "USERNAME")
    private String userName;

    /**
     * 用户id
     */
    @TableField(value = "USERID")
    private Integer userId;

    /**
     * 状态：0使用中，1未使用，2发布确认，3删除确认
     */
    @TableField(value = "STATUS")
    private Status status;

    /**
     * 备注
     */
    @TableField(value = "REMARK")
    private String remark;

    @TableLogic
    @JsonIgnore
    @TableField(value = "ISDEL", select = false)
    private Integer isDel;

    public enum Status implements IEnum {

        UNUSED("0", "未发布"),
        USING("1", "使用中"),
        PUBLISH("2", "申请发布"),
        DELETE("3", "申请删除"),
        ABANDON("4", "废弃"),
        INACTIVE("5", "申请解除活跃状态");

        private String value;

        private String desc;

        private Status(String value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        ;

        @JsonValue
        public String getValue() {
            return value;
        }


        public String getDesc() {
            return desc;
        }
    }

    @Data
    public static class MapVersion {

        private String prefix = "YMS";

        private Integer bigVersion = 1;

        private Integer smallVersion = 1;

        @JsonValue
        public String toString() {
            return prefix + "." + String.format("%03d", bigVersion) + "." + String.format("%03d", smallVersion);
        }
    }
}

