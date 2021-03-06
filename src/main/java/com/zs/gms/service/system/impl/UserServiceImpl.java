package com.zs.gms.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.gms.common.authentication.ShiroHelper;
import com.zs.gms.common.authentication.ShiroRealm;
import com.zs.gms.common.entity.GmsConstant;
import com.zs.gms.common.entity.QueryRequest;
import com.zs.gms.common.exception.GmsException;
import com.zs.gms.common.utils.GmsUtil;
import com.zs.gms.common.utils.MD5Util;
import com.zs.gms.common.utils.PropertyUtil;
import com.zs.gms.common.utils.SortUtil;
import com.zs.gms.entity.system.Role;
import com.zs.gms.entity.system.User;
import com.zs.gms.entity.system.UserRole;
import com.zs.gms.mapper.system.UserMapper;
import com.zs.gms.service.system.RoleService;
import com.zs.gms.service.system.UserRoleService;
import com.zs.gms.service.system.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private ShiroRealm shiroRealm;

    @Autowired
    private ShiroHelper shiroHelper;


    /**
     * 通过用户名查找用户
     */
    @Override
    //@Cacheable(cacheNames = "users",key = "targetClass +#p0", unless = "#result==null")
    @Transactional
    //p0表示第一个参数，可以指定参数名称
    //redis不能缓存值为null的键值
    //#result表示方法返回结果,使用unless = "#result==null"判断是否缓存，条件成立时不缓存，如果返回集合使用unless = "#result==null||#result.size()==0"
    public User findByName(String userName) {
        return this.baseMapper.findUserByName(userName);
    }

    @Override
    public Integer getMaxId() {
        return this.baseMapper.getMaxId();
    }

    @Override
    @Transactional
    public void updateRetry(Integer userId,int num) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId);
        updateWrapper.set(User::getRetry, num);
        this.update(updateWrapper);
    }

    @Override
    public void updateLock(Integer userId, boolean isLock) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId);
        updateWrapper.set(User::isUserLock, isLock);
        this.update(updateWrapper);
    }

    @Override
    @Transactional
    public void clearLock(Integer userId) {
        updateLock(userId,false);
        updateRetry(userId,0);
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Integer userId, Date lastLoginTime) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId);
        updateWrapper.set(User::getLastLoginTime, lastLoginTime);
        this.update(updateWrapper);
    }


    @Override
    public List<User> getUsersByRoleId(Integer roleId) {
        return this.baseMapper.getUsersByRoleId(roleId);
    }

    @Override
    @Transactional
    @Cacheable(cacheNames = "users", key = "'findUserById_' +#p0", unless = "#result==null")
    public User findUserById(Integer userId) {
        return this.baseMapper.getUserById(userId);
    }

    @Override
    @Transactional
    public List<User> getUsersByRoleSign(String roleSign) {
        return this.baseMapper.getUsersByRoleSign(roleSign);
    }


    /**
     * 添加用户
     */
    @Override
    @Transactional
    public void addUser(User user) {
        user.setPassword(MD5Util.encrypt(user.getUserName(), user.getPassword()));
        user.setCreateTime(new Date());
        this.save(user);
        if (null != user.getRoleId()) {
            batchSaveUserRole(user.getUserId(), new String[]{user.getRoleId().toString()});
        }
    }

    /**
     * 分页查询
     */
    @Override
    @Transactional
    public IPage<User> findUserListPage(User user, QueryRequest request) {
        Page page = new Page(request.getPageNo(), request.getPageSize());
        List<User> users = this.baseMapper.findUserList(user);
        for (User record : users) {
            Integer userId = record.getUserId();
            if (ShiroHelper.isActivate(userId)) {
                record.setActivate(true);
            }
        }
        Map<String,String> sortMap=new HashMap<>();
        sortMap.put("userName",GmsConstant.SORT_ASC);
        sortMap.put("activate",GmsConstant.SORT_DESC);
        List<User> pageSort = SortUtil.memoryPageSort(request.getPageNo(),request.getPageSize(),sortMap, users, User.class);
        page.setRecords(pageSort);
        page.setTotal(users.size());
        return page;
    }

    @Override
    @Transactional
    public List<Map<String,Object>> findUnitUserList() {
        return this.baseMapper.findUnitUserList();
    }


    /**
     * 注册时添加默认项
     */
    @Override
    @Transactional
    public void register(String userName, String password, String phone) {
        User user = new User();
        user.setUserName(userName);
        user.setTheme(User.THEAM_WHITE);
        user.setAvatar(User.DEFAULT_ICON);
        user.setPassword(MD5Util.encrypt(userName, password));
        user.setPhone(phone);
        user.setCreateTime(new Date());
        this.save(user);

        //可以设置默认角色
        Role normalRole = roleService.getRoleByRoleName(Role.RoleSign.NORMAL_ROLE.getValue());
        if (normalRole != null) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getUserId());
            ur.setRoleId(normalRole.getRoleId());
            userRoleService.save(ur);
        }
    }

    /**
     * 删除用户
     */
    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        this.removeById(userId);
        userRoleService.removeUserRole(userId);
    }

    /**
     * 批量删除用户
     */
    @Override
    @Transactional
    public void deleteUsers(String[] userIds) {
        List<String> ids = Arrays.asList(userIds);
        for (String id : ids) {
            deleteUser(Integer.valueOf(id));
        }
    }

    @Override
    @Transactional
    public boolean isExist(String[] userIds) {
        if (ArrayUtils.isNotEmpty(userIds)) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(User::getUserId, userIds);
            List<User> list = this.list(queryWrapper);
            if (!CollectionUtils.isEmpty(list)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新用户
     */
    @Override
    @Transactional
    public void updateUser(User user) throws GmsException {
        if (null != user.getRoleId()) {
            Integer roleId = user.getRoleId();
            Role role = roleService.getRole(roleId);
            if (null != role) {
                userRoleService.addUserRole(user.getUserId(), roleId);
            }
        }
        if (GmsUtil.allObjNotNull(user.getPassword())) {
            shiroHelper.clearSession(user.getUserId());//修改密码，清除session
            User u = findUserById(user.getUserId());
            user.setPassword(MD5Util.encrypt(u.getUserName(), user.getPassword()));
        }
        user.setUserName(null);
        user.setRoleId(null);
        if (PropertyUtil.isAllFieldNull(user, "userId", "serialVersionUID")) {
            log.debug("对象可修改属性都为空，不能更新");
            return;
        }
        this.updateById(user);
    }

    @Override
    @Transactional
    public void updateUserName(Integer userId, String userName) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId,userId);
        updateWrapper.set(User::getUserName,userName);
        this.update(updateWrapper);
    }

    @Override
    @Transactional
    public void updateUserRole(Integer userId, Integer roleId) {
        userRoleService.addUserRole(userId,roleId);
        shiroRealm.clearCache();
    }

    /**
     * 批量写入用户角色关系
     */
    @Transactional
    public void batchSaveUserRole(Integer userId, String[] roleIds) {
        List<UserRole> userRoles = new ArrayList<>();
        Arrays.stream(roleIds).forEach(roleId -> {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(Integer.valueOf(roleId));
            userRoles.add(userRole);
        });
        userRoleService.saveBatch(userRoles);
    }

    /**
     * 修改密码
     */
    @Transactional
    //@CacheEvict(cacheNames = "users",key = "targetClass+#p0")
    public void updatePassword(String userName, String password) {
        shiroHelper.clearSession(userName);
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(User::getPassword, MD5Util.encrypt(userName, password));
        updateWrapper.eq(User::getUserName, userName);
        this.update(updateWrapper);
    }
}
