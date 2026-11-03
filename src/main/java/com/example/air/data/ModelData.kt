package com.example.air.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_data")
data class ModelData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val scale: Float,
    val imagePath: String, // Changed from glbFilePath to imagePath
    val photoPath: String? = null,
    val backgroundRotation: Float = 0f, // 新增：背景图旋转角度
    val timestamp: Long = System.currentTimeMillis()
)

// 新增：圖片層級數據結構
@Entity(tableName = "image_layers")
data class ImageLayer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelId: Long,
    val imagePath: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val alpha: Float = 1.0f, // 透明度，0.0f = 完全透明，1.0f = 完全不透明
    val zIndex: Int = 0, // 層級順序，數字越大越靠前
    val name: String = "",
    val isLocked: Boolean = false, // 新增：图层锁定状态
    val isVisible: Boolean = true // 新增：图层可见性
)

// 新增：包含多圖層的模型數據
data class ModelWithLayers(
    val model: ModelData,
    val layers: List<ImageLayer> = emptyList()
)

// 新增：操作历史记录，用于撤销/重做功能
data class OperationHistory(
    val operationType: OperationType,
    val layerId: Long?,
    val oldData: Any?,
    val newData: Any?,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OperationType {
    ADD_LAYER,
    DELETE_LAYER,
    UPDATE_LAYER,
    MOVE_LAYER,
    LOCK_LAYER,
    VISIBILITY_LAYER,
    BACKGROUND_ROTATION
}

// 新增：自定義圖片實體
@Entity(tableName = "custom_images")
data class CustomImage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 新增：圖層分組數據結構
@Entity(tableName = "layer_groups")
data class LayerGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelId: Long,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 新增：圖層分組成員
@Entity(tableName = "layer_group_members")
data class LayerGroupMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val layerId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

// 新增：包含分組的圖層數據
data class ImageLayerWithGroup(
    val layer: ImageLayer,
    val groupId: Long? = null,
    val groupName: String? = null
)

// 新增：圖層分組信息
data class LayerGroupWithMembers(
    val group: LayerGroup,
    val members: List<ImageLayer> = emptyList()
) 