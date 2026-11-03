package com.example.air.data

import kotlinx.coroutines.flow.Flow

class ModelRepository(private val modelDao: ModelDao) {
    val allModels: Flow<List<ModelData>> = modelDao.getAllModels()

    suspend fun insertModel(model: ModelData): Long {
        return modelDao.insertModel(model)
    }

    suspend fun updateModel(model: ModelData) {
        modelDao.updateModel(model)
    }

    suspend fun deleteModel(model: ModelData) {
        modelDao.deleteModel(model)
    }

    suspend fun getModelById(id: Long): ModelData? {
        return modelDao.getModelById(id)
    }

    // 新增：圖片層級相關操作
    fun getLayersByModelId(modelId: Long): Flow<List<ImageLayer>> {
        return modelDao.getLayersByModelId(modelId)
    }

    suspend fun insertLayer(layer: ImageLayer): Long {
        return modelDao.insertLayer(layer)
    }

    suspend fun updateLayer(layer: ImageLayer) {
        modelDao.updateLayer(layer)
    }

    suspend fun deleteLayer(layer: ImageLayer) {
        modelDao.deleteLayer(layer)
    }

    suspend fun deleteLayersByModelId(modelId: Long) {
        modelDao.deleteLayersByModelId(modelId)
    }

    suspend fun getMaxZIndex(modelId: Long): Int {
        return modelDao.getMaxZIndex(modelId) ?: -1
    }

    // 新增：图层顺序调整
    suspend fun updateLayerOrder(layerId: Long, newZIndex: Int, modelId: Long) {
        modelDao.updateLayerOrder(layerId, newZIndex, modelId)
    }

    // 新增：获取单个图层
    suspend fun getLayerById(layerId: Long): ImageLayer? {
        return modelDao.getLayerById(layerId)
    }

    // 新增：复制图层
    suspend fun duplicateLayer(layerId: Long, modelId: Long): Long {
        return modelDao.duplicateLayer(layerId, modelId)
    }

    // 新增：更新背景旋转
    suspend fun updateBackgroundRotation(modelId: Long, rotation: Float) {
        modelDao.updateBackgroundRotation(modelId, rotation)
    }

    // 新增：更新图层锁定状态
    suspend fun updateLayerLockStatus(layerId: Long, isLocked: Boolean) {
        modelDao.updateLayerLockStatus(layerId, isLocked)
    }

    // 新增：更新图层可见性
    suspend fun updateLayerVisibility(layerId: Long, isVisible: Boolean) {
        modelDao.updateLayerVisibility(layerId, isVisible)
    }

    // 新增：移动图层到顶部
    suspend fun moveLayerToTop(layerId: Long, modelId: Long) {
        val maxZIndex = getMaxZIndex(modelId)
        if (maxZIndex >= 0) {
            updateLayerOrder(layerId, maxZIndex + 1, modelId)
        }
    }

    // 新增：移动图层到底部
    suspend fun moveLayerToBottom(layerId: Long, modelId: Long) {
        updateLayerOrder(layerId, 0, modelId)
    }

    // 新增：移动图层向上
    suspend fun moveLayerUp(layerId: Long, modelId: Long) {
        val layer = getLayerById(layerId)
        layer?.let {
            if (it.zIndex < getMaxZIndex(modelId)) {
                updateLayerOrder(layerId, it.zIndex + 1, modelId)
            }
        }
    }

    // 新增：移动图层向下
    suspend fun moveLayerDown(layerId: Long, modelId: Long) {
        val layer = getLayerById(layerId)
        layer?.let {
            if (it.zIndex > 0) {
                updateLayerOrder(layerId, it.zIndex - 1, modelId)
            }
        }
    }

    // 新增：自定義圖片相關操作
    fun getAllCustomImages(): Flow<List<CustomImage>> {
        return modelDao.getAllCustomImages()
    }

    suspend fun insertCustomImage(customImage: CustomImage): Long {
        return modelDao.insertCustomImage(customImage)
    }

    suspend fun updateCustomImage(customImage: CustomImage) {
        modelDao.updateCustomImage(customImage)
    }

    suspend fun deleteCustomImage(customImage: CustomImage) {
        modelDao.deleteCustomImage(customImage)
    }

    suspend fun getCustomImageById(id: Long): CustomImage? {
        return modelDao.getCustomImageById(id)
    }

    suspend fun deleteCustomImageByPath(filePath: String) {
        modelDao.deleteCustomImageByPath(filePath)
    }

    // 新增：圖層分組相關操作
    fun getLayerGroupsByModelId(modelId: Long): Flow<List<LayerGroup>> {
        return modelDao.getLayerGroupsByModelId(modelId)
    }

    suspend fun insertLayerGroup(layerGroup: LayerGroup): Long {
        return modelDao.insertLayerGroup(layerGroup)
    }

    suspend fun updateLayerGroup(layerGroup: LayerGroup) {
        modelDao.updateLayerGroup(layerGroup)
    }

    suspend fun deleteLayerGroup(layerGroup: LayerGroup) {
        modelDao.deleteLayerGroup(layerGroup)
    }

    fun getLayerGroupMembers(groupId: Long): Flow<List<LayerGroupMember>> {
        return modelDao.getLayerGroupMembers(groupId)
    }

    suspend fun insertLayerGroupMember(member: LayerGroupMember): Long {
        return modelDao.insertLayerGroupMember(member)
    }

    suspend fun deleteLayerGroupMember(member: LayerGroupMember) {
        modelDao.deleteLayerGroupMember(member)
    }

    suspend fun deleteLayerGroupMembers(groupId: Long) {
        modelDao.deleteLayerGroupMembers(groupId)
    }

    suspend fun getLayerGroupById(groupId: Long): LayerGroup? {
        return modelDao.getLayerGroupById(groupId)
    }

    // 新增：創建圖層分組
    suspend fun createLayerGroup(modelId: Long, name: String, layerIds: List<Long>): Long {
        val layerGroup = LayerGroup(
            modelId = modelId,
            name = name
        )
        val groupId = insertLayerGroup(layerGroup)
        
        // 添加成員
        layerIds.forEach { layerId ->
            insertLayerGroupMember(
                LayerGroupMember(
                    groupId = groupId,
                    layerId = layerId
                )
            )
        }
        
        return groupId
    }

    // 新增：解散圖層分組
    suspend fun dissolveLayerGroup(groupId: Long) {
        deleteLayerGroupMembers(groupId)
        val group = getLayerGroupById(groupId)
        group?.let { deleteLayerGroup(it) }
    }
} 