package prototype.coreapi.global.util;

import prototype.coreapi.domain.auth.dto.SignInPrincipal;
import prototype.coreapi.global.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtil {

    public static boolean isNotAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        return "anonymousUser".equals(auth.getPrincipal());
    }

    public static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + Role.ADMIN.name())
                );
    }

    public static boolean isUser() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + Role.USER.name())
                );
    }

    public static Long getLoginUserId() {
        SignInPrincipal principal = (SignInPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.memberId();
    }
}
