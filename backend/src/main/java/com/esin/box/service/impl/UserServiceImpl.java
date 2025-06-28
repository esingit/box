package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.esin.box.entity.User;
import com.esin.box.filter.JwtTokenProvider;
import com.esin.box.mapper.UserMapper;
import com.esin.box.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    @Override
    public boolean register(User user) {
        if (findByUsername(user.getUsername()) != null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userMapper.insert(user) > 0;
    }

    /**
     * 生成登录令牌
     * 注意：此方法不进行密码验证，调用前请确保已验证用户身份
     */
    @Override
    public Map<String, String> generateLoginTokens(User user) {
        // 记录登录时间
        recordLogin(user.getId(), LocalDateTime.now());

        // 生成一对token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    @Override
    public void recordLogin(Long userId, LocalDateTime lastLoginTime) {
        userMapper.updateLastLoginTime(userId, java.sql.Timestamp.valueOf(lastLoginTime));
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return !passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public boolean updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }

}