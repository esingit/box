package com.box.login.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.box.login.entity.AssetName;
import com.box.login.mapper.AssetNameMapper;
import com.box.login.service.AssetNameService;
import com.box.login.config.UserContextHolder;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDateTime;
import java.io.Serializable;

@Service
public class AssetNameServiceImpl extends ServiceImpl<AssetNameMapper, AssetName> implements AssetNameService {
    
    @Override
    public AssetName createAssetName(AssetName assetName) {
        // 设置创建人
        assetName.setCreateUser(UserContextHolder.getCurrentUsername());
        save(assetName);
        return assetName;
    }

    @Override
    public AssetName updateAssetName(AssetName assetName) {
        // 验证是否是记录创建人
        AssetName existing = getById(assetName.getId());
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限修改此资产名称");
        }
        updateById(assetName);
        return assetName;
    }

    @Override
    public boolean removeById(Serializable id) {
        // 验证是否是记录创建人
        AssetName existing = getById(id);
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限删除此资产名称");
        }
        return super.removeById(id);
    }

    @Override
    public AssetName getById(Serializable id) {
        AssetName assetName = super.getById(id);
        if (assetName != null) {
            String currentUser = UserContextHolder.getCurrentUsername();
            if (!currentUser.equals(assetName.getCreateUser())) {
                throw new RuntimeException("您没有权限查看此资产名称");
            }
        }
        return assetName;
    }

    @Override
    public Page<AssetName> advancedSearch(Integer current, Integer size, String name, 
                                        String description, String startTime, String endTime) {
        Page<AssetName> page = new Page<>(current, size);
        LambdaQueryWrapper<AssetName> wrapper = Wrappers.lambdaQuery(AssetName.class);
        
        // 添加当前用户过滤
        wrapper.eq(AssetName::getCreateUser, UserContextHolder.getCurrentUsername());
        
        // 添加其他查询条件
        if (name != null && !name.isEmpty()) {
            wrapper.like(AssetName::getName, name);
        }
        if (description != null && !description.isEmpty()) {
            wrapper.like(AssetName::getRemark, description);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(AssetName::getCreateTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(AssetName::getCreateTime, LocalDateTime.parse(endTime));
        }
        
        return page(page, wrapper);
    }
}
