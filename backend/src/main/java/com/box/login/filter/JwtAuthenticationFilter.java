package com.box.login.filter;

import com.box.login.config.UserContextHolder;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);
            
            // 如果请求中没有token，并且不是登录、注册、验证码等公开接口
            if (!StringUtils.hasText(token)) {
                String path = request.getRequestURI();
                if (path.startsWith("/api/user/login") || 
                    path.startsWith("/api/user/register") || 
                    path.startsWith("/api/captcha")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"未登录或Token已过期\"}");
                return;
            }
            
            try {
                // 先验证token
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsernameFromJWT(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 设置当前用户到 UserContextHolder
                        UserContextHolder.setCurrentUser(username);
                        
                        // 检查是否需要刷新token
                        if (jwtTokenProvider.shouldRefreshToken(token)) {
                            String newToken = jwtTokenProvider.refreshToken(token);
                            response.setHeader("Authorization", "Bearer " + newToken);
                        }
                        
                        // 继续处理请求
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
                throw new JwtException("Token验证失败");
            } catch (Exception e) {
                // token验证失败，返回401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                String message = e instanceof JwtException ? e.getMessage() : "Token已过期或无效";
                response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
                // 清除安全上下文
                SecurityContextHolder.clearContext();
            }
        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            UserContextHolder.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
