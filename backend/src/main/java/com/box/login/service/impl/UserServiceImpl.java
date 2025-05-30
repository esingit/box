package com.box.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.box.login.entity.User;
import com.box.login.mapper.UserMapper;
import com.box.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 用于签发Token的密钥，实际应用中应保存在安全的地方
    private final long validityInMilliseconds = 3600000; // Token有效期1小时

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

    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername()); // 将用户名放入claims
        // 可以添加其他用户信息到claims

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    @Override
    public String login(String username, String password) {
        User user = findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // 登录成功，记录最后登录时间
            recordLogin(user.getId(), LocalDateTime.now());
            // 登录成功，生成Token
            return generateToken(user); // 返回Token字符串而不是User对象
        }
        return null;
    }

    @Override
    public void recordLogin(Long userId, LocalDateTime lastLoginTime) {
        userMapper.updateLastLoginTime(userId, java.sql.Timestamp.valueOf(lastLoginTime));
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public boolean updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }
}