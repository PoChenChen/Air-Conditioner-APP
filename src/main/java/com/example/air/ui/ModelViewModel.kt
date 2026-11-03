package com.example.air.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.air.data.ImageLayer
import com.example.air.data.ModelData
import com.example.air.data.ModelRepository
import com.example.air.data.ModelWithLayers
import com.example.air.data.OperationHistory
import com.example.air.data.OperationType
import com.example.air.data.CustomImage
import com.example.air.data.LayerGroup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

class ModelViewModel(private val repository: ModelRepository) : ViewModel() {
    private val _models = MutableStateFlow<List<ModelData>>(emptyList())
    val models: StateFlow<List<ModelData>> = _models.asStateFlow()

    private val _currentPhotoPath = MutableStateFlow<String?>(null)
    val currentPhotoPath: StateFlow<String?> = _currentPhotoPath.asStateFlow()

    private val _selectedImagePath = MutableStateFlow<String?>(null) // Changed from selectedGlbFile
    val selectedImagePath: StateFlow<String?> = _selectedImagePath.asStateFlow()

    // 新增：當前編輯的圖層
    private val _currentLayers = MutableStateFlow<List<ImageLayer>>(emptyList())
    val currentLayers: StateFlow<List<ImageLayer>> = _currentLayers.asStateFlow()

    // 修改：支持多选图层
    private val _selectedLayerIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedLayerIds: StateFlow<Set<Long>> = _selectedLayerIds.asStateFlow()

    // 新增：当前模型ID
    private val _currentModelId = MutableStateFlow<Long?>(null)
    val currentModelId: StateFlow<Long?> = _currentModelId.asStateFlow()

    // 新增：背景旋转角度
    private val _backgroundRotation = MutableStateFlow(0f)
    val backgroundRotation: StateFlow<Float> = _backgroundRotation.asStateFlow()

    // 新增：撤销/重做历史记录
    private val _undoStack = MutableStateFlow<List<OperationHistory>>(emptyList())
    val undoStack: StateFlow<List<OperationHistory>> = _undoStack.asStateFlow()

    private val _redoStack = MutableStateFlow<List<OperationHistory>>(emptyList())
    val redoStack: StateFlow<List<OperationHistory>> = _redoStack.asStateFlow()



    // 新增：自动对齐网格大小
    private val _gridSize = MutableStateFlow(20f)
    val gridSize: StateFlow<Float> = _gridSize.asStateFlow()

    // 新增：自定義圖片相關功能
    private val _customImages = MutableStateFlow<List<CustomImage>>(emptyList())
    val customImages: StateFlow<List<CustomImage>> = _customImages.asStateFlow()

    // 新增：圖層分組相關功能
    private val _layerGroups = MutableStateFlow<List<LayerGroup>>(emptyList())
    val layerGroups: StateFlow<List<LayerGroup>> = _layerGroups.asStateFlow()

    init {
        loadModels()
    }

    private fun loadModels() {
        viewModelScope.launch {
            repository.allModels.collect { models ->
                _models.value = models
            }
        }
    }

    fun setPhotoPath(path: String?) {
        _currentPhotoPath.value = path
    }

    fun setSelectedImagePath(imagePath: String?) { // Changed from setSelectedGlbFile
        _selectedImagePath.value = imagePath
    }

    // 新增：設置當前圖層
    fun setCurrentLayers(layers: List<ImageLayer>) {
        _currentLayers.value = layers
    }
    
    // 新增：創建臨時模型
    suspend fun createTempModel(photoPath: String): Long {
        val tempModel = ModelData(
            name = "臨時模型",
            x = 0f,
            y = 0f,
            z = 0f,
            rotationX = 0f,
            rotationY = 0f,
            rotationZ = 0f,
            scale = 1f,
            imagePath = "",
            photoPath = photoPath,
            backgroundRotation = 0f
        )
        return repository.insertModel(tempModel)
    }

    // 修改：支持多选图层
    fun setSelectedLayerIds(layerIds: Set<Long>) {
        _selectedLayerIds.value = layerIds
    }

    // 新增：添加图层到选择
    fun addToSelection(layerId: Long) {
        _selectedLayerIds.value = _selectedLayerIds.value + layerId
    }

    // 新增：从选择中移除图层
    fun removeFromSelection(layerId: Long) {
        _selectedLayerIds.value = _selectedLayerIds.value - layerId
    }

    // 新增：清空选择
    fun clearSelection() {
        _selectedLayerIds.value = emptySet()
    }

    // 新增：设置当前模型ID
    fun setCurrentModelId(modelId: Long?) {
        _currentModelId.value = modelId
    }

    // 新增：设置背景旋转
    fun setBackgroundRotation(rotation: Float) {
        _backgroundRotation.value = rotation
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                repository.updateBackgroundRotation(modelId, rotation)
                addToHistory(OperationHistory(
                    operationType = OperationType.BACKGROUND_ROTATION,
                    layerId = null,
                    oldData = _backgroundRotation.value,
                    newData = rotation
                ))
            }
        }
    }

    // 新增：设置网格大小
    fun setGridSize(size: Float) {
        _gridSize.value = size
    }

    // 新增：图层自动对齐到网格
    fun snapLayersToGrid() {
        val layers = _currentLayers.value.toMutableList()
        val grid = _gridSize.value
        
        layers.forEachIndexed { index, layer ->
            val snappedX = (layer.x / grid).roundToInt() * grid
            val snappedY = (layer.y / grid).roundToInt() * grid
            
            if (abs(layer.x - snappedX) > 1f || abs(layer.y - snappedY) > 1f) {
                layers[index] = layer.copy(x = snappedX, y = snappedY)
            }
        }
        
        _currentLayers.value = layers
        // 保存到数据库
        _currentModelId.value?.let { _ ->
            viewModelScope.launch {
                layers.forEach { layer ->
                    repository.updateLayer(layer)
                }
            }
        }
    }



    // 新增：添加操作历史
    private fun addToHistory(operation: OperationHistory) {
        _undoStack.value = _undoStack.value + operation
        _redoStack.value = emptyList() // 新操作会清空重做栈
    }

    // 新增：撤销操作
    fun undo() {
        val undoStack = _undoStack.value
        if (undoStack.isNotEmpty()) {
            val lastOperation = undoStack.last()
            _undoStack.value = undoStack.dropLast(1)
            
            // 将操作移到重做栈
            _redoStack.value = _redoStack.value + lastOperation
            
            // 执行撤销
            when (lastOperation.operationType) {
                OperationType.ADD_LAYER -> {
                    lastOperation.layerId?.let { layerId ->
                        val layer = _currentLayers.value.find { it.id == layerId }
                        layer?.let { deleteLayer(it) }
                    }
                }
                OperationType.DELETE_LAYER -> {
                    lastOperation.oldData?.let { oldLayer ->
                        if (oldLayer is ImageLayer) {
                            addLayer(
                                modelId = oldLayer.modelId,
                                imagePath = oldLayer.imagePath,
                                x = oldLayer.x,
                                y = oldLayer.y,
                                scale = oldLayer.scale,
                                rotation = oldLayer.rotation,
                                alpha = oldLayer.alpha,
                                name = oldLayer.name
                            )
                        }
                    }
                }
                OperationType.UPDATE_LAYER -> {
                    lastOperation.oldData?.let { oldLayer ->
                        if (oldLayer is ImageLayer) {
                            updateLayer(oldLayer)
                        }
                    }
                }
                OperationType.BACKGROUND_ROTATION -> {
                    lastOperation.oldData?.let { oldRotation ->
                        if (oldRotation is Float) {
                            setBackgroundRotation(oldRotation)
                        }
                    }
                }
                else -> { /* 其他操作类型 */ }
            }
        }
    }

    // 新增：重做操作
    fun redo() {
        val redoStack = _redoStack.value
        if (redoStack.isNotEmpty()) {
            val lastOperation = redoStack.last()
            _redoStack.value = redoStack.dropLast(1)
            
            // 将操作移回撤销栈
            _undoStack.value = _undoStack.value + lastOperation
            
            // 执行重做
            when (lastOperation.operationType) {
                OperationType.ADD_LAYER -> {
                    lastOperation.newData?.let { newLayer ->
                        if (newLayer is ImageLayer) {
                            addLayer(
                                modelId = newLayer.modelId,
                                imagePath = newLayer.imagePath,
                                x = newLayer.x,
                                y = newLayer.y,
                                scale = newLayer.scale,
                                rotation = newLayer.rotation,
                                alpha = newLayer.alpha,
                                name = newLayer.name
                            )
                        }
                    }
                }
                OperationType.DELETE_LAYER -> {
                    lastOperation.newData?.let { newLayer ->
                        if (newLayer is ImageLayer) {
                            deleteLayer(newLayer)
                        }
                    }
                }
                OperationType.UPDATE_LAYER -> {
                    lastOperation.newData?.let { newLayer ->
                        if (newLayer is ImageLayer) {
                            updateLayer(newLayer)
                        }
                    }
                }
                OperationType.BACKGROUND_ROTATION -> {
                    lastOperation.newData?.let { newRotation ->
                        if (newRotation is Float) {
                            setBackgroundRotation(newRotation)
                        }
                    }
                }
                else -> { /* 其他操作类型 */ }
            }
        }
    }

    // 新增：添加圖層
    fun addLayer(
        modelId: Long,
        imagePath: String,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1.0f,
        rotation: Float = 0f,
        alpha: Float = 1.0f,
        name: String = ""
    ) {
        viewModelScope.launch {
            val maxZIndex = repository.getMaxZIndex(modelId)
            
            val newLayer = ImageLayer(
                modelId = modelId,
                imagePath = imagePath,
                x = x,
                y = y,
                scale = scale,
                rotation = rotation,
                alpha = alpha,
                zIndex = maxZIndex + 1,
                name = name
            )
            val layerId = repository.insertLayer(newLayer)
            
            // 添加到历史记录
            addToHistory(OperationHistory(
                operationType = OperationType.ADD_LAYER,
                layerId = layerId,
                oldData = null,
                newData = newLayer.copy(id = layerId)
            ))
            
            loadLayersForModel(modelId)
        }
    }

    // 新增：更新圖層
    fun updateLayer(layer: ImageLayer) {
        viewModelScope.launch {
            val oldLayer = repository.getLayerById(layer.id)
            repository.updateLayer(layer)
            
            // 添加到历史记录
            oldLayer?.let {
                addToHistory(OperationHistory(
                    operationType = OperationType.UPDATE_LAYER,
                    layerId = layer.id,
                    oldData = it,
                    newData = layer
                ))
            }
            
            loadLayersForModel(layer.modelId)
        }
    }

    // 新增：批量更新图层
    fun updateMultipleLayers(layers: List<ImageLayer>) {
        viewModelScope.launch {
            layers.forEach { layer ->
                repository.updateLayer(layer)
            }
            _currentModelId.value?.let { modelId ->
                loadLayersForModel(modelId)
            }
        }
    }

    // 新增：刪除圖層
    fun deleteLayer(layer: ImageLayer) {
        viewModelScope.launch {
            // 添加到历史记录
            addToHistory(OperationHistory(
                operationType = OperationType.DELETE_LAYER,
                layerId = layer.id,
                oldData = layer,
                newData = null
            ))
            
            repository.deleteLayer(layer)
            loadLayersForModel(layer.modelId)
        }
    }

    // 新增：通过ID删除图层
    fun removeLayer(layerId: Long) {
        viewModelScope.launch {
            val layer = _currentLayers.value.find { it.id == layerId }
            layer?.let { deleteLayer(it) }
        }
    }

    // 新增：批量删除图层
    fun deleteMultipleLayers(layerIds: Set<Long>) {
        viewModelScope.launch {
            layerIds.forEach { layerId ->
                val layer = repository.getLayerById(layerId)
                layer?.let { deleteLayer(it) }
            }
            clearSelection()
        }
    }

    // 新增：复制图层
    fun duplicateLayer(layerId: Long) {
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                repository.duplicateLayer(layerId, modelId)
                loadLayersForModel(modelId)
            }
        }
    }

    // 新增：批量复制图层
    fun duplicateMultipleLayers(layerIds: Set<Long>) {
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                layerIds.forEach { layerId ->
                    repository.duplicateLayer(layerId, modelId)
                }
                loadLayersForModel(modelId)
            }
        }
    }

    // 新增：图层顺序调整
    fun moveLayerUp(layerId: Long) {
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                repository.moveLayerUp(layerId, modelId)
                loadLayersForModel(modelId)
            }
        }
    }

    fun moveLayerDown(layerId: Long) {
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                repository.moveLayerDown(layerId, modelId)
                loadLayersForModel(modelId)
            }
        }
    }

    fun moveLayerToTop(layerId: Long) {
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                repository.moveLayerToTop(layerId, modelId)
                loadLayersForModel(modelId)
            }
        }
    }

    fun moveLayerToBottom(layerId: Long) {
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                repository.moveLayerToBottom(layerId, modelId)
                loadLayersForModel(modelId)
            }
        }
    }

    // 新增：图层锁定
    fun toggleLayerLock(layerId: Long) {
        viewModelScope.launch {
            val layer = repository.getLayerById(layerId)
            layer?.let {
                val newLockedState = !it.isLocked
                repository.updateLayerLockStatus(layerId, newLockedState)
                loadLayersForModel(it.modelId)
            }
        }
    }

    // 新增：图层可见性
    fun toggleLayerVisibility(layerId: Long) {
        viewModelScope.launch {
            val layer = repository.getLayerById(layerId)
            layer?.let {
                val newVisibleState = !it.isVisible
                repository.updateLayerVisibility(layerId, newVisibleState)
                loadLayersForModel(it.modelId)
            }
        }
    }

    // 新增：載入模型的圖層
    fun loadLayersForModel(modelId: Long) {
        viewModelScope.launch {
            repository.getLayersByModelId(modelId).collect { layers ->
                _currentLayers.value = layers
            }
        }
    }

    fun saveModel(
        name: String,
        x: Float,
        y: Float,
        z: Float,
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float,
        scale: Float,
        imagePath: String // Changed from glbFilePath
    ) {
        viewModelScope.launch {
            val model = ModelData(
                name = name,
                x = x,
                y = y,
                z = z,
                rotationX = rotationX,
                rotationY = rotationY,
                rotationZ = rotationZ,
                scale = scale,
                imagePath = imagePath, // Changed from glbFilePath
                photoPath = _currentPhotoPath.value,
                backgroundRotation = _backgroundRotation.value
            )
            val modelId = repository.insertModel(model)
            
            // 保存當前圖層
            _currentLayers.value.forEach { layer ->
                val newLayer = layer.copy(modelId = modelId)
                repository.insertLayer(newLayer)
            }
            
            _currentPhotoPath.value = null
            _selectedImagePath.value = null // Changed from selectedGlbFile
            _currentLayers.value = emptyList()
            _selectedLayerIds.value = emptySet()
            _currentModelId.value = null
            _backgroundRotation.value = 0f
            _undoStack.value = emptyList()
            _redoStack.value = emptyList()
        }
    }

    fun deleteModel(model: ModelData) {
        viewModelScope.launch {
            repository.deleteLayersByModelId(model.id)
            repository.deleteModel(model)
        }
    }

    fun updateModel(model: ModelData) {
        viewModelScope.launch {
            repository.updateModel(model)
        }
    }

    suspend fun getModelById(id: Long): ModelData? {
        return repository.getModelById(id)
    }

    // 新增：獲取模型及其圖層
    suspend fun getModelWithLayers(id: Long): ModelWithLayers? {
        val model = repository.getModelById(id) ?: return null
        val layersFlow = repository.getLayersByModelId(id)
        val layers = layersFlow.first()
        
        // 设置当前状态
        _currentModelId.value = id
        _backgroundRotation.value = model.backgroundRotation
        
        return ModelWithLayers(model, layers)
    }

    // 載入自定義圖片
    fun loadCustomImages() {
        viewModelScope.launch {
            repository.getAllCustomImages().collect { images ->
                _customImages.value = images
            }
        }
    }

    // 添加自定義圖片
    fun addCustomImage(name: String, filePath: String) {
        viewModelScope.launch {
            val customImage = CustomImage(
                name = name,
                filePath = filePath
            )
            repository.insertCustomImage(customImage)
        }
    }

    // 更新自定義圖片
    fun updateCustomImage(customImage: CustomImage) {
        viewModelScope.launch {
            repository.updateCustomImage(customImage)
        }
    }

    // 刪除自定義圖片
    fun deleteCustomImage(customImage: CustomImage) {
        viewModelScope.launch {
            repository.deleteCustomImage(customImage)
        }
    }

    // 獲取自定義圖片路徑列表
    fun getCustomImagePaths(): List<String> {
        return _customImages.value.map { "custom://${it.id}" }
    }

    // 新增：圖層分組相關功能
    // 載入圖層分組
    fun loadLayerGroups(modelId: Long) {
        viewModelScope.launch {
            repository.getLayerGroupsByModelId(modelId).collect { groups ->
                _layerGroups.value = groups
            }
        }
    }

    // 創建圖層分組
    fun createLayerGroup(modelId: Long, name: String, layerIds: List<Long>) {
        viewModelScope.launch {
            repository.createLayerGroup(modelId, name, layerIds)
            loadLayerGroups(modelId)
        }
    }

    // 解散圖層分組
    fun dissolveLayerGroup(groupId: Long, modelId: Long) {
        viewModelScope.launch {
            repository.dissolveLayerGroup(groupId)
            loadLayerGroups(modelId)
        }
    }

    // 獲取圖層分組信息
    fun getLayerGroupInfo(layerId: Long): LayerGroup? {
        return _layerGroups.value.find { group ->
            // 這裡需要檢查圖層是否屬於該分組
            // 實際實現中需要從數據庫查詢
            false
        }
    }
} 