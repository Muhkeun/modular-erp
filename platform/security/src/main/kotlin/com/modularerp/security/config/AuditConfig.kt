package com.modularerp.security.config

import com.modularerp.security.tenant.TenantContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class AuditConfig {

    @Bean
    fun auditorAware(): AuditorAware<String> = AuditorAware {
        Optional.ofNullable(TenantContext.getUserId())
    }
}
