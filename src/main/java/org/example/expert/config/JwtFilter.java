package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final String[] whiteList = {"/auth"};

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilterInternal(
            HttpServletRequest httpRequest,
            @NonNull HttpServletResponse httpResponse,
            @NonNull FilterChain chain
    ) throws IOException, ServletException {
        String requestURI = httpRequest.getRequestURI();

        for(String list : whiteList){
            if(requestURI.startsWith(list)){
                chain.doFilter(httpRequest, httpResponse);
                return;
            }
        }

        String authorizationHeader = httpRequest.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "유효하지 않는 JWT 서명입니다.");
        }

        String jwt = jwtUtil.substringToken(authorizationHeader);
        if(!processAuthentication(jwt, httpRequest, httpResponse)){
            return;
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private boolean processAuthentication(String jwt, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        try {
            // JWT 유효성 검사와 claims 추출
            Claims claims = jwtUtil.extractClaims(jwt);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                setAuthentication(claims);
            }
            return true;

        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.", e);
            sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "유효하지 않는 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT token 입니다.", e);
            sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.", e);
            sendErrorResponse(httpResponse, HttpStatus.BAD_REQUEST, "지원되지 않는 JWT 토큰입니다.");
        } catch (Exception e) {
            log.error("Internal server error", e);
            sendErrorResponse(httpResponse, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
        return false;
    }

    private void setAuthentication(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);
        UserRole userRole = UserRole.valueOf(claims.get("userRole", String.class));
        AuthUser authUser = new AuthUser(userId, email, nickname, userRole);

        Authentication authenticationToken = new JwtAuthenticationToken(authUser);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private void sendErrorResponse(HttpServletResponse httpResponse, HttpStatus status, String message) throws IOException {
        httpResponse.setStatus(status.value());
        httpResponse.setContentType("application/json;charset=UTF-8");
        Map<String,Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.name());
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        httpResponse.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
