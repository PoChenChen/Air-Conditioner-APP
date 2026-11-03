package com.example.air.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM model_data ORDER BY timestamp DESC")
    fun getAllModels(): Flow<List<ModelData>>

    @Query("SELECT * FROM model_data WHERE id = :id")
    suspend fun getModelById(id: Long): ModelData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelData): Long

    @Update
    suspend fun updateModel(model: ModelData)

    @Delete
    suspend fun deleteModel(model: ModelData)

    // 新增：圖片層級相關操作
    @Query("SELECT * FROM image_layers WHERE modelId = :modelId ORDER BY zIndex ASC")
    fun getLayersByModelId(modelId: Long): Flow<List<ImageLayer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: ImageLayer): Long

    @Update
    suspend fun updateLayer(layer: ImageLayer)

    @Delete
    suspend fun deleteLayer(layer: ImageLayer)

    @Query("DELETE FROM image_layers WHERE modelId = :modelId")
    suspend fun deleteLayersByModelId(modelId: Long)

    @Query("SELECT MAX(zIndex) FROM image_layers WHERE modelId = :modelId")
    suspend fun getMaxZIndex(modelId: Long): Int?

    // 新增：图层顺序调整
    @Query("UPDATE image_layers SET zIndex = :newZIndex WHERE id = :layerId")
    suspend fun updateLayerZIndex(layerId: Long, newZIndex: Int)

    // 新增：批量更新图层顺序 - 向上移动
    @Query("UPDATE image_layers SET zIndex = zIndex - 1 WHERE modelId = :modelId AND zIndex > :currentZIndex AND zIndex <= :newZIndex")
    suspend fun moveLayersDown(modelId: Long, currentZIndex: Int, newZIndex: Int)

    // 新增：批量更新图层顺序 - 向下移动
    @Query("UPDATE image_layers SET zIndex = zIndex + 1 WHERE modelId = :modelId AND zIndex >= :newZIndex AND zIndex < :currentZIndex")
    suspend fun moveLayersUp(modelId: Long, newZIndex: Int, currentZIndex: Int)

    // 新增：批量更新图层顺序
    @Transaction
    suspend fun updateLayerOrder(layerId: Long, newZIndex: Int, modelId: Long) {
        val currentLayer = getLayerById(layerId)
        currentLayer?.let { layer ->
            val currentZIndex = layer.zIndex
            if (newZIndex > currentZIndex) {
                // 向上移动：将中间图层向下移动
                moveLayersDown(modelId, currentZIndex, newZIndex)
            } else if (newZIndex < currentZIndex) {
                // 向下移动：将中间图层向上移动
                moveLayersUp(modelId, newZIndex, currentZIndex)
            }
            updateLayerZIndex(layerId, newZIndex)
        }
    }

    // 新增：获取单个图层
    @Query("SELECT * FROM image_layers WHERE id = :layerId")
    suspend fun getLayerById(layerId: Long): ImageLayer?

    // 新增：复制图层
    @Transaction
    suspend fun duplicateLayer(layerId: Long, modelId: Long): Long {
        val originalLayer = getLayerById(layerId)
        return originalLayer?.let { layer ->
            val maxZIndex = getMaxZIndex(modelId) ?: 0
            val newLayer = layer.copy(
                id = 0, // 新ID将由数据库自动生成
                zIndex = maxZIndex + 1,
                name = "${layer.name} (複製)"
            )
            insertLayer(newLayer)
        } ?: 0L
    }

    // 新增：更新背景旋转
    @Query("UPDATE model_data SET backgroundRotation = :rotation WHERE id = :modelId")
    suspend fun updateBackgroundRotation(modelId: Long, rotation: Float)

    // 新增：更新图层锁定状态
    @Query("UPDATE image_layers SET isLocked = :isLocked WHERE id = :layerId")
    suspend fun updateLayerLockStatus(layerId: Long, isLocked: Boolean)

    // 新增：更新图层可见性
    @Query("UPDATE image_layers SET isVisible = :isVisible WHERE id = :layerId")
    suspend fun updateLayerVisibility(layerId: Long, isVisible: Boolean)

    // 新增：自定義圖片相關操作
    @Query("SELECT * FROM custom_images ORDER BY timestamp DESC")
    fun getAllCustomImages(): Flow<List<CustomImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomImage(customImage: CustomImage): Long

    @Update
    suspend fun updateCustomImage(customImage: CustomImage)

    @Delete
    suspend fun deleteCustomImage(customImage: CustomImage)

    @Query("SELECT * FROM custom_images WHERE id = :id")
    suspend fun getCustomImageById(id: Long): CustomImage?

    @Query("DELETE FROM custom_images WHERE filePath = :filePath")
    suspend fun deleteCustomImageByPath(filePath: String)

    // 新增：圖層分組相關操作
    @Query("SELECT * FROM layer_groups WHERE modelId = :modelId ORDER BY timestamp DESC")
    fun getLayerGroupsByModelId(modelId: Long): Flow<List<LayerGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayerGroup(layerGroup: LayerGroup): Long

    @Update
    suspend fun updateLayerGroup(layerGroup: LayerGroup)

    @Delete
    suspend fun deleteLayerGroup(layerGroup: LayerGroup)

    @Query("SELECT * FROM layer_group_members WHERE groupId = :groupId")
    fun getLayerGroupMembers(groupId: Long): Flow<List<LayerGroupMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayerGroupMember(member: LayerGroupMember): Long

    @Delete
    suspend fun deleteLayerGroupMember(member: LayerGroupMember)

    @Query("DELETE FROM layer_group_members WHERE groupId = :groupId")
    suspend fun deleteLayerGroupMembers(groupId: Long)

    @Query("SELECT * FROM layer_groups WHERE id = :groupId")
    suspend fun getLayerGroupById(groupId: Long): LayerGroup?
} 