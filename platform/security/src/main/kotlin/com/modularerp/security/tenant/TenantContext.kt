package com.modularerp.security.tenant

object TenantContext {
    private val currentTenant = ThreadLocal<String>()
    private val currentUserId = ThreadLocal<String>()
    private val currentLocale = ThreadLocal<String>()

    fun getTenantId(): String =
        currentTenant.get() ?: throw IllegalStateException("Tenant not set")

    fun setTenantId(tenantId: String) = currentTenant.set(tenantId)

    fun getUserId(): String? = currentUserId.get()
    fun setUserId(userId: String) = currentUserId.set(userId)

    fun getLocale(): String = currentLocale.get() ?: "ko"
    fun setLocale(locale: String) = currentLocale.set(locale)

    fun clear() {
        currentTenant.remove()
        currentUserId.remove()
        currentLocale.remove()
    }
}
