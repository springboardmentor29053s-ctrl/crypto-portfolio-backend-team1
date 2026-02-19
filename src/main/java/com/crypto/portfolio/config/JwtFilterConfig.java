package com.crypto.portfolio.config;

/*
import com.crypto.portfolio.security.JwtFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtFilterConfig {

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilter() {

        FilterRegistrationBean<JwtFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(new JwtFilter());

        // 🔐 protect only these endpoints
        registrationBean.addUrlPatterns("/api/*");

        return registrationBean;
    }
}  */



import com.crypto.portfolio.security.JwtFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtFilterConfig {

    private final JwtFilter jwtFilter;

    public JwtFilterConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration() {

        FilterRegistrationBean<JwtFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(jwtFilter);  // <-- NO new JwtFilter()

        registrationBean.addUrlPatterns("/api/*");

        return registrationBean;
    }
}
