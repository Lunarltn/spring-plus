package org.example.expert.domain.common.dto;

import lombok.Getter;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthUser {
    private final Long id;
    private final String nickname;
    private final String email;
    private final UserRole userRole;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUser(Long id, String nickname, String email, UserRole userRole) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.userRole = userRole;
        this.authorities= List.of(new SimpleGrantedAuthority(userRole.getName()));
    }
}
