package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esin.box.entity.AssetName;
import com.esin.box.mapper.AssetNameMapper;
import com.esin.box.service.AssetNameService;
import com.esin.box.config.UserContextHolder;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class AssetNameServiceImpl extends ServiceImpl<AssetNameMapper, AssetName> implements AssetNameService {
    
    @Override
    public AssetName addAssetName(AssetName assetName) {
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
    public IPage<AssetName> listRecords(Page<AssetName> pageObj, String name, String description, String startTime, String endTime) {
        QueryWrapper<AssetName> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            query.like("name", name);
        }
        if (StringUtils.isNotBlank(description)) {
            query.like("description", description);
        }
        if (StringUtils.isNotBlank(startTime)) {
            query.ge("create_time", startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            query.le("create_time", endTime);
        }
        return this.page(pageObj, query);
    }

}
