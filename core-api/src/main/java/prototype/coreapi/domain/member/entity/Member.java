package prototype.coreapi.domain.member.entity;


import prototype.coreapi.domain.BaseEntity;
import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import prototype.coreapi.global.enums.Role;
import prototype.coreapi.global.enums.Status;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Table("members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Member extends BaseEntity implements UserDetails {

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("role")
    @Builder.Default
    private Role role = Role.USER;

    @Column("status")
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    public void encodePassword(String rawPassword, PasswordEncoder encoder) {
        this.password = encoder.encode(rawPassword);
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.status == Status.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != Status.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != Status.DELETED;
    }
}