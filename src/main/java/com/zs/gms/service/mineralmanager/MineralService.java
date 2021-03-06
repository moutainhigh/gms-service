package com.zs.gms.service.mineralmanager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zs.gms.common.entity.QueryRequest;
import com.zs.gms.entity.mineralmanager.Mineral;

import java.util.Collection;
import java.util.List;

public interface MineralService {

    public void addMineral(Mineral mineral);

    void updateActive(Collection<Integer> mineralIds);

    /**
     * 判断矿物名称是否添加
     */
    boolean isMineralExist(String mineralName);

    /**
     * 除mineralId外是否存在
     */
    boolean isMineralExist(Integer mineralId, String mineralName);

    Mineral getMineral(Integer mineralId);

    IPage<Mineral> getMineralList(QueryRequest queryRequest);

    void updateMineral(Mineral mineral);

    void deleteMineral(String mineralIds);
}
