package com.zs.gms.common.entity;

public class RedisKeyPool {

    /**
     * ==================service====================
     */

    public final static String REDIS_MONITOR = "redis_monitor";//redis监听

    public final static String REDIS_INCR = "redis_incr";//自增键

    public final static String REDIS_INCR_USERID = "redis_incr_userid";//自增用户id

    public final static String DELAY_TASK_PREFIX = "delay_task_";//延时任务参数

    public final static String REDIS_SCRIPT_PREFIX = "redis_script_";//redis执行脚本

    public final static String METHOD_INVOKE_INTERVAL_PREFIX = "method_";//方法执行间隔

    public final static String ACTIVATE_USERS = "activate_users";//在线用户,Map

    public final static String SERVICE_DISCOVER_PREFIX = "service_discover_";//服务心跳

    /**
     * ==================dispatch====================
     */
    public final static String VAP_BASE_PREFIX = "vap_base_";//车辆基础信息，包括障碍物信息、异常信息

    public final static String VAP_COLLECTION_PREFIX = "vap_collection_";//地图点集

    public final static String VAP_TRAIL_PREFIX = "vap_trail_";//车辆轨迹

    public final static String VAP_PATH_PREFIX = "vap_path_";//车辆全局路径

    public final static String VAP_PREFIX = "vap";//车辆推送前缀

    public final static String DISPATCH_AREA_PREFIX = "dispatch_task_area_";//任务区状态

    public final static String DISPATCH_PREFIX = "dispatch";//其他调度服务前缀

    public final static String DISPATCH_UNIT = "dispatch_unit_";//调度单元状态

    public final static String DISPATCH_SERVER_INIT = "dispatch_server_init";//监听调度初始化键

    public final static String DISPATCH_SERVER_HEARTBEAT = "dispatch_server_heartbeat";//监听调度心跳

    public final static String GPS_ID_IP = "gps_id_ip";//GPS id、ip关联MAP，在0库中

    public final static String VEH_ID_IP = "veh_id_ip";//车辆id、ip关联MAP，在0库中

    public final static String EXC_ID_IP = "exc_id_ip";//挖掘机id、ip关联MAP，在0库中

    public final static String VEH_BASE_INFO = "veh_base_info";//矿车基本信息MAP，在0库中

    /**
     * ==================map====================
     */
    public final static String ACTIVITY_MAP = "activity_map_id";//活动地图

    public final static String SEMI_STATIC_DATA = "semi_static_data_";//半静态层数据

    public final static String MAP_EDIT_LOCK = "map_edit_lock_";//地图编辑锁定

    public final static String MAP_SERVER_HEARTBEAT = "map_server_heartbeat";//监听地图心跳

    public final static String MAP_COLLECTION_PREFIX = "map_collection_";//地图采集点集，在0库中


    /**
     * ==================lock====================
     * */
    public final static String DISPATCH_INIT_LOCK = "dispatch_init_lock";//调度初始化锁键

    public final static String DISPATCH_STATUS_LOCK = "dispatch_status_lock";//调度状态锁键

    public final static String VEHICLE_LIVE_INFO_LOCK = "vehicle_live_info_lock";//车辆上报基础数据入库锁

    public final static String MAP_SERVER_LOCK = "map_server_lock";//地图编辑锁键

    public final static String MAP_COLLECTION_LOCK = "map_collection_lock";//地图采集锁键

    public final static String WS_NETTY_LOCK = "ws_netty_lock";//ws数据发送
}
