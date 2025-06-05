package com.box.login.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;
import com.box.login.mapper.AssetRecordMapper;
import com.box.login.service.AssetRecordService;
import com.box.login.config.UserContextHolder;
import org.springframework.stereotype.Service;
import java.io.Serializable;

/**
 * 资产记录服务实现类
 */
@Service
public class AssetRecordServiceImpl extends ServiceImpl<AssetRecordMapper, AssetRecord> implements AssetRecordService {

    @Override
    public boolean save(AssetRecord entity) {
        entity.setCreateUser(UserContextHolder.getCurrentUsername());
        return super.save(entity);
    }

    @Override
    public boolean updateById(AssetRecord entity) {
        // 验证是否是记录创建人
        AssetRecord existing = getById(entity.getId());
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限修改此资产记录");
        }
        return super.updateById(entity);
    }

    @Override
    public boolean removeById(Serializable id) {
        // 验证是否是记录创建人
        AssetRecord existing = getById(id);
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限删除此资产记录");
        }
        return super.removeById(id);
    }

    @Override
    public AssetRecordDTO getDetailById(Long id) {
        String currentUser = UserContextHolder.getCurrentUsername();
        return baseMapper.selectDetailById(id, currentUser);
    }

    @Override
    public Page<AssetRecordDTO> pageWithDetails(Page<AssetRecord> page, String assetName) {
        String currentUser = UserContextHolder.getCurrentUsername();
        return baseMapper.selectPageWithDetails(page, assetName, currentUser);
    }
}
