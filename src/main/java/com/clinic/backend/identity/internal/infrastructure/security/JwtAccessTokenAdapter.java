package com.clinic.backend.identity.internal.infrastructure.security;

import com.clinic.backend.identity.internal.application.port.AccessTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAccessTokenAdapter implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;

    public JwtAccessTokenAdapter(
            JwtEncoder encoder,
            Clock clock,
            @Value("${security.jwt.issuer:clinic-system}") String issuer,
            @Value("${security.jwt.access-token-ttl:PT15M}") Duration ttl
    ) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    @Override
    public IssuedAccessToken issue(UUID userId, Set<String> roles) {
        return issue(userId, roles, false);
    }

    @Override
    public IssuedAccessToken issue(UUID userId, Set<String> roles, boolean mustChangePassword) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("token_type", "access")
                .claim("roles", roles == null ? List.of() : List.copyOf(roles));
        if (mustChangePassword) {
            builder.claim("pwd_change_required", true);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }
}
