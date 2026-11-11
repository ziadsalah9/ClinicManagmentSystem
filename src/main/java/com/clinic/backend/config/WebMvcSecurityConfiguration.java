package com.clinic.backend.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebMvcSecurityConfiguration implements WebMvcConfigurer {
    private final PasswordChangeRequiredInterceptor interceptor;
    public WebMvcSecurityConfiguration(PasswordChangeRequiredInterceptor interceptor) { this.interceptor = interceptor; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**").excludePathPatterns("/api/v1/auth/otp/**");
    }
}
