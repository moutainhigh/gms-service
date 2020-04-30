package com.zs.gms.service.init;

import com.zs.gms.entity.init.GmsGlobalConfig;

public interface GmsConfigService {

    void addGmsConfig(GmsGlobalConfig gmsGlobalConfig);

    GmsGlobalConfig getGmsConfig(String key);

    void updateGmsGlobalConfig(GmsGlobalConfig gmsGlobalConfig);

    boolean isExistKey(String key);
}
