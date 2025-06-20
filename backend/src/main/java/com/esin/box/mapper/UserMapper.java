package com.esin.box.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esin.box.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    void updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") Date lastLoginTime);
}