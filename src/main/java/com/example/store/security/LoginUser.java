package com.example.store.security;

import com.example.store.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 用户主体
 *
 * 包装 User 实体，实现 UserDetails 接口，
 * 让 Security 能识别用户身份和权限。
 *
 * 角色映射：
 *   type=1 → ROLE_SUPER_ADMIN（超级管理员）
 *   type=2 → ROLE_SHOP_ADMIN（信息管理员/店铺管理员）
 *   type=3 → ROLE_USER（普通买家）
 */
@Data
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = switch (user.getType()) {
            case 1 -> "ROLE_SUPER_ADMIN";
            case 2 -> "ROLE_SHOP_ADMIN";
            default -> "ROLE_USER";
        };
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /** 账号是否未过期 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** 账号是否未锁定 */
    @Override
    public boolean isAccountNonLocked() {
        return user.getUseful() == null || user.getUseful() == 1;
    }

    /** 凭证是否未过期 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号是否启用 */
    @Override
    public boolean isEnabled() {
        return user.getUseful() == null || user.getUseful() == 1;
    }
}
