package com.example.ams.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.ams.domain.user.UserRole;

public class AuthenticatedUser implements UserDetails {

	private final JwtPrincipal principal;

	public AuthenticatedUser(JwtPrincipal principal) {
		this.principal = principal;
	}

	public JwtPrincipal principal() {
		return principal;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return String.valueOf(principal.userId());
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return !"SUSPENDED".equals(principal.status());
	}

	public boolean isAcademyAdmin() {
		return principal.role() == UserRole.ACADEMY_ADMIN;
	}
}
