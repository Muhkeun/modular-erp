package com.modularerp.ai.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AiProperties::class)
@ComponentScan(basePackages = ["com.modularerp.ai"])
class AiAutoConfiguration
