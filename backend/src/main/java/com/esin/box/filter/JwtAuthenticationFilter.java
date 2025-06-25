package com.esin.box.filter;

import com.esin.box.config.UserContextHolder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/user/login",
            "/api/user/register",
            "/api/captcha",
            "/api/user/verify-token",
            "/api/user/refresh-token"
    );

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            if (!StringUtils.hasText(token)) {
                handleNoToken(request, response, filterChain);
                return;
            }

            try {
                handleValidToken(token, request, response, filterChain);
            } catch (ExpiredJwtException e) {
                // 特殊处理：token过期
                if (!isPublicPath(request.getRequestURI())) {
                    handleExpiredToken(response);
                    return;
                }
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                if (!isPublicPath(request.getRequestURI())) {
                    handleTokenError(response, e);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        } finally {
            UserContextHolder.clear();
        }
    }

    private void handleNoToken(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) throws IOException, ServletException {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        sendUnauthorizedError(response, "NO_TOKEN", "未登录或Token缺失");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void handleValidToken(String token,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws IOException, ServletException {
        String path = request.getRequestURI();

        // 先检查token类型
        try {
            String tokenType = jwtTokenProvider.getTokenType(token);
            if (!"access".equals(tokenType)) {
                if (!isPublicPath(path)) {
                    sendUnauthorizedError(response, "INVALID_TOKEN_TYPE", "无效的Token类型");
                    return;
                }
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            logger.warn("获取Token类型失败: {}", e.getMessage());
            if (!isPublicPath(path)) {
                sendUnauthorizedError(response, "INVALID_TOKEN", "无效的Token");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 特殊处理verify-token接口
        if (path.equals("/api/user/verify-token")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 验证token - 这里可能抛出 ExpiredJwtException
            boolean isValidToken = jwtTokenProvider.validateToken(token);

            if (isValidToken) {
                String username = jwtTokenProvider.getUsernameFromJWT(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails != null) {
                    setAuthentication(request, userDetails);
                    setUserContext(username);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 如果token无效且不是公开路径，返回错误
            if (!isPublicPath(path)) {
                sendUnauthorizedError(response, "INVALID_TOKEN", "Token验证失败");
                return;
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            logger.warn("Token已过期: {}", e.getMessage());
            // 抛出异常让外层处理
            throw e;
        } catch (JwtException e) {
            logger.warn("Token验证失败: {}", e.getMessage());
            if (isPublicPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }
            throw e;
        } catch (Exception e) {
            logger.error("处理Token时发生错误", e);
            if (isPublicPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }
            throw new JwtException("Token处理失败", e);
        }
    }

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setUserContext(String username) {
        UserContextHolder.setCurrentUser(username);
    }

    private void handleExpiredToken(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        sendUnauthorizedError(response, "TOKEN_EXPIRED", "Token已过期");
    }

    private void handleTokenError(HttpServletResponse response, Exception e) throws IOException {
        SecurityContextHolder.clearContext();
        String code = "INVALID_TOKEN";
        String message = "Token无效";

        // 检查是否是过期异常
        if (e instanceof ExpiredJwtException) {
            code = "TOKEN_EXPIRED";
            message = "Token已过期";
        } else if (e instanceof JwtException) {
            // 检查异常消息中是否包含"过期"相关信息
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("过期") || errorMsg.contains("expired"))) {
                code = "TOKEN_EXPIRED";
                message = "Token已过期";
            } else {
                message = errorMsg != null ? errorMsg : "Token验证失败";
            }
        }

        sendUnauthorizedError(response, code, message);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"code\":\"%s\",\"message\":\"%s\"}",
                code,
                message
        ));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}