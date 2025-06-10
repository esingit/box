package com.box.login.filter;

import com.box.login.config.UserContextHolder;
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
            "/api/user/verify-token",
            "/api/captcha"
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
            } catch (Exception e) {
                handleTokenError(response, e);
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
        sendUnauthorizedError(response, "未登录或Token已过期");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void handleValidToken(String token, 
                                HttpServletRequest request,
                                HttpServletResponse response, 
                                FilterChain filterChain) throws IOException, ServletException {
        String path = request.getRequestURI();
        try {
            // 验证token
            boolean isValidToken = jwtTokenProvider.validateToken(token);
            // 特殊处理verify-token接口，不需要设置认证信息
            if (path.equals("/api/user/verify-token")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            if (isValidToken) {
                String username = jwtTokenProvider.getUsernameFromJWT(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (userDetails != null) {
                    setAuthentication(request, userDetails);
                    setUserContext(username);
                    
                    // 检查是否需要刷新token
                    if (jwtTokenProvider.shouldRefreshToken(token)) {
                        String newToken = jwtTokenProvider.refreshToken(token);
                        if (newToken != null) {
                            response.setHeader("Authorization", "Bearer " + newToken);
                        }
                    }
                    
                    filterChain.doFilter(request, response);
                    return;
                }
            }
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
        
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        throw new JwtException("Token验证失败");
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

    private void handleTokenError(HttpServletResponse response, Exception e) throws IOException {
        SecurityContextHolder.clearContext();
        String message = e instanceof JwtException ? e.getMessage() : "Token已过期或无效";
        sendUnauthorizedError(response, message);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"success\":false,\"message\":\"%s\"}", message));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
