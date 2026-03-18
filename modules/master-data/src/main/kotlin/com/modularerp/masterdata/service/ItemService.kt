package com.modularerp.masterdata.service

import com.modularerp.core.exception.DuplicateEntityException
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.core.port.ItemInfo
import com.modularerp.core.port.ItemQueryPort
import com.modularerp.masterdata.domain.Item
import com.modularerp.masterdata.domain.ItemType
import com.modularerp.masterdata.dto.*
import com.modularerp.masterdata.repository.ItemRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ItemService(
    private val itemRepository: ItemRepository
) : ItemQueryPort {

    fun getById(id: Long): ItemResponse {
        val item = findItem(id)
        return item.toResponse(TenantContext.getLocale())
    }

    fun search(
        code: String? = null,
        itemType: ItemType? = null,
        itemGroup: String? = null,
        pageable: Pageable
    ): Page<ItemResponse> {
        val tenantId = TenantContext.getTenantId()
        val locale = TenantContext.getLocale()
        return itemRepository.search(tenantId, code, itemType, itemGroup, pageable)
            .map { it.toResponse(locale) }
    }

    @Transactional
    fun create(request: CreateItemRequest): ItemResponse {
        val tenantId = TenantContext.getTenantId()

        if (itemRepository.existsByTenantIdAndCode(tenantId, request.code)) {
            throw DuplicateEntityException("Item", "code", request.code)
        }

        val item = Item(
            code = request.code,
            itemType = request.itemType,
            itemGroup = request.itemGroup,
            unitOfMeasure = request.unitOfMeasure,
            specification = request.specification,
            weight = request.weight,
            volume = request.volume,
            makerName = request.makerName,
            makerItemNo = request.makerItemNo,
            qualityInspectionRequired = request.qualityInspectionRequired,
            phantomBom = request.phantomBom
        ).apply {
            assignTenant(tenantId)
        }

        val saved = itemRepository.save(item)

        request.translations.forEach { t ->
            saved.addTranslation(t.locale, t.name, t.description)
        }
        val result = itemRepository.save(saved)
        return result.toResponse(TenantContext.getLocale())
    }

    @Transactional
    fun update(id: Long, request: UpdateItemRequest): ItemResponse {
        val item = findItem(id)

        request.itemType?.let { item.itemType = it }
        request.itemGroup?.let { item.itemGroup = it }
        request.unitOfMeasure?.let { item.unitOfMeasure = it }
        request.specification?.let { item.specification = it }
        request.weight?.let { item.weight = it }
        request.volume?.let { item.volume = it }
        request.makerName?.let { item.makerName = it }
        request.makerItemNo?.let { item.makerItemNo = it }
        request.qualityInspectionRequired?.let { item.qualityInspectionRequired = it }
        request.phantomBom?.let { item.phantomBom = it }

        request.translations?.forEach { t ->
            item.addTranslation(t.locale, t.name, t.description)
        }

        return itemRepository.save(item).toResponse(TenantContext.getLocale())
    }

    @Transactional
    fun delete(id: Long) {
        val item = findItem(id)
        item.deactivate()
        itemRepository.save(item)
    }

    private fun findItem(id: Long): Item {
        val tenantId = TenantContext.getTenantId()
        return itemRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow { EntityNotFoundException("Item", id) }
    }

    // Port implementation for cross-module queries
    override fun findById(tenantId: String, id: Long): ItemInfo? =
        itemRepository.findByTenantIdAndId(tenantId, id).orElse(null)?.toItemInfo()

    override fun findByCode(tenantId: String, code: String): ItemInfo? =
        itemRepository.findByTenantIdAndCode(tenantId, code).orElse(null)?.toItemInfo()

    private fun Item.toItemInfo() = ItemInfo(
        id = id, code = code, name = getName("ko"),
        unitOfMeasure = unitOfMeasure, itemType = itemType.name, itemGroup = itemGroup
    )
}
