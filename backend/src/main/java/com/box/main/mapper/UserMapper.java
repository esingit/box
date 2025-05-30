package com.box.main.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.box.main.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}