package com.modularerp.app

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * SPA fallback — React Router의 클라이언트 사이드 라우팅을 지원.
 * /api, /swagger-ui, /v3 등이 아닌 모든 경로를 index.html로 포워딩한다.
 */
@Controller
class SpaController {

    @GetMapping(value = [
        "/",
        "/dashboard",
        "/master-data/**",
        "/purchase/**",
        "/production/**",
        "/logistics/**",
        "/planning/**",
        "/sales/**",
        "/account/**",
        "/hr/**",
        "/quality/**",
        "/login",
        "/settings/**",
        "/admin/**",
        "/finance/**",
        "/costing/**",
        "/crm/**",
        "/notifications/**"
    ])
    fun forward(): String = "forward:/index.html"
}
