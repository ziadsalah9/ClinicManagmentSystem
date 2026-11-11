package com.clinic.backend.config;
import com.clinic.backend.identity.IdentityFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException; import java.nio.charset.StandardCharsets; import java.util.Set; import java.util.UUID;
@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/api/v1/auth/password/change", "/api/v1/auth/change-password",
            "/api/v1/auth/refresh", "/api/v1/auth/logout");
    private final IdentityFacade identity;
    public PasswordChangeRequiredInterceptor(IdentityFacade identity) { this.identity = identity; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) return true;
        String path = request.getRequestURI();
        for (String p : ALLOWED_PREFIXES) if (path.startsWith(p)) return true;
        Jwt jwt = jwtAuth.getToken();
        if (Boolean.TRUE.equals(jwt.getClaim("pwd_change_required")) || identity.requiresPasswordChange(UUID.fromString(jwt.getSubject()))) {
            response.setStatus(403);
            response.setContentType("application/problem+json");
            String body = "{\"type\":\"https://api.clinic.local/problems/identity/password_change_required\",\"title\":\"Password change required\",\"status\":403,\"detail\":\"You must change your temporary password before accessing this resource\",\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"instance\":\"" + path.replace("\"","") + "\"}";
            response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            return false;
        }
        return true;
    }
}
