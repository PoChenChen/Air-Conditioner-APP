package com.example.air.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Clear
import kotlin.math.roundToInt

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.air.R
import com.example.air.data.ImageLayer
import com.example.air.data.CustomImage
import com.example.air.data.PresetImage
import com.example.air.data.presetImages
import com.example.air.ui.ModelViewModel
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ComposeView
import android.view.View
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 統一的面板狀態管理
data class PanelState(
    val isExpanded: Boolean = false,
    val height: Dp = 50.dp,
    val offsetY: Dp = 50.dp,
    val selectedTab: Int = 0
)

@Composable
fun ModelPlacementScreen(
    navController: NavController,
    viewModel: ModelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showImageSelector by remember { mutableStateOf(false) }


    // 當前編輯的圖層
    val currentLayers by viewModel.currentLayers.collectAsState()
    val selectedLayerIds by viewModel.selectedLayerIds.collectAsState()

    // 为了兼容性，保持单个选择的逻辑
    val selectedLayerId = selectedLayerIds.firstOrNull()

    // 背景旋转角度
    val backgroundRotation by viewModel.backgroundRotation.collectAsState()

    // 撤销/重做状态
    val undoStack by viewModel.undoStack.collectAsState()
    val redoStack by viewModel.redoStack.collectAsState()

    // 当前模型ID
    val currentModelId by viewModel.currentModelId.collectAsState()

    // 当前照片路径
    val currentPhotoPath by viewModel.currentPhotoPath.collectAsState()

    // 多選圖層狀態
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // 自動對齊網格狀態
    var gridSize by remember { mutableStateOf(20f) }


    // 背景縮放狀態
    var backgroundScale by remember { mutableStateOf(1f) }
    
    // 背景位置偏移
    var backgroundOffsetX by remember { mutableStateOf(0f) }
    var backgroundOffsetY by remember { mutableStateOf(0f) }

    // 如果沒有模型ID，創建一個臨時模型
    LaunchedEffect(currentPhotoPath) {
        if (currentPhotoPath != null && currentModelId == null) {
            val tempModelId = viewModel.createTempModel(currentPhotoPath!!)
            viewModel.setCurrentModelId(tempModelId)
        }
    }

    // 當模型ID變化時載入圖層
    LaunchedEffect(currentModelId) {
        currentModelId?.let { modelId ->
            viewModel.loadLayersForModel(modelId)
        }
    }

    // 載入自定義圖片
    LaunchedEffect(Unit) {
        viewModel.loadCustomImages()
    }

    // 統一的面板狀態管理
    var panelState by remember { 
        mutableStateOf(
            PanelState(
                isExpanded = false,
                height = 400.dp,
                offsetY = 50.dp,
                selectedTab = 1
            )
        ) 
    }

    // 計算屏幕中心位置
    val centerX = 200f
    val centerY = 200f
    
    // 自定義圖片選擇器
    val customImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // 處理選擇的自定義圖片 - 保存到應用私有目錄並添加到數據庫
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val fileName = "custom_image_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(context.filesDir, fileName)
                
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                
                // 添加到自定義圖片數據庫
                viewModel.addCustomImage("自定義圖片_${System.currentTimeMillis()}", file.absolutePath)
                
                // 添加圖層 - 使用正確的圖片路徑格式
                viewModel.addLayer(
                    modelId = currentModelId ?: 0L,
                    imagePath = "custom://${file.absolutePath}",
                    x = centerX,
                    y = centerY,
                    scale = 1f,
                    rotation = 0f,
                    alpha = 1f,
                    name = "自定義圖片"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // 顯示錯誤提示
                android.widget.Toast.makeText(context, "圖片載入失敗: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 多選圖層功能
    fun toggleLayerSelection(layerId: Long) {
        if (isMultiSelectMode) {
            if (selectedLayerIds.contains(layerId)) {
                viewModel.removeFromSelection(layerId)
            } else {
                viewModel.addToSelection(layerId)
            }
        } else {
            viewModel.addToSelection(layerId)
        }
    }

    // 自動對齊功能
    fun alignLayersToGrid() {
        currentLayers.forEach { layer ->
            val alignedX = (layer.x / gridSize).roundToInt() * gridSize
            val alignedY = (layer.y / gridSize).roundToInt() * gridSize
            viewModel.updateLayer(layer.copy(x = alignedX, y = alignedY))
        }
    }

    // 批量操作功能
    fun applyToSelectedLayers(operation: (ImageLayer) -> ImageLayer) {
        selectedLayerIds.forEach { layerId ->
            currentLayers.find { it.id == layerId }?.let { layer ->
                val updatedLayer = operation(layer)
                viewModel.updateLayer(updatedLayer)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 背景圖片
        currentPhotoPath?.let { photoPath ->
            when {
                photoPath == "transparent_background" -> {
                    // 透明背景
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    )
                }
                photoPath.startsWith("content://") || photoPath.startsWith("file://") -> {
                    // URI 背景圖片
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(
                                x = with(LocalDensity.current) { backgroundOffsetX.toDp() },
                                y = with(LocalDensity.current) { backgroundOffsetY.toDp() }
                            )
                            .rotate(backgroundRotation)
                            .scale(backgroundScale)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { /* 拖拽開始 */ },
                                    onDragEnd = { /* 拖拽結束 */ }
                                ) { change, dragAmount ->
                                    backgroundOffsetX += dragAmount.x
                                    backgroundOffsetY += dragAmount.y
                                }
                            },
                        update = { imageView ->
                            try {
                                val uri = Uri.parse(photoPath)
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                    if (bitmap != null) {
                                        imageView.setImageBitmap(bitmap)
                                    } else {
                                        imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                    }
                                    inputStream.close()
                                } else {
                                    imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                }
                            } catch (e: Exception) {
                                // 處理錯誤
                                e.printStackTrace()
                                imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        }
                    )
                }
                else -> {
                    // 文件路徑背景圖片
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(
                                x = with(LocalDensity.current) { backgroundOffsetX.toDp() },
                                y = with(LocalDensity.current) { backgroundOffsetY.toDp() }
                            )
                            .rotate(backgroundRotation)
                            .scale(backgroundScale)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { /* 拖拽開始 */ },
                                    onDragEnd = { /* 拖拽結束 */ }
                                ) { change, dragAmount ->
                                    backgroundOffsetX += dragAmount.x
                                    backgroundOffsetY += dragAmount.y
                                }
                            },
                        update = { imageView ->
                            try {
                                val file = java.io.File(photoPath)
                                if (file.exists() && file.canRead()) {
                                    val bitmap = BitmapFactory.decodeFile(photoPath)
                                    if (bitmap != null) {
                                        imageView.setImageBitmap(bitmap)
                                    } else {
                                        imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                    }
                                } else {
                                    imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                }
                            } catch (e: Exception) {
                                // 處理錯誤
                                e.printStackTrace()
                                imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        }
                    )
                }
            }
        } ?: run {
            // 如果沒有背景圖片，顯示透明背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        // 圖層顯示
        currentLayers.forEach { layer ->
            if (layer.isVisible) {
            InteractiveImageLayer(
                layer = layer,
                    isSelected = selectedLayerId == layer.id || selectedLayerIds.contains(layer.id),
                    isMultiSelectMode = isMultiSelectMode,
                    onLayerClick = { toggleLayerSelection(layer.id) },
                onLayerUpdate = { updatedLayer ->
                    viewModel.updateLayer(updatedLayer)
                    }
                )
            }
        }

        // 頂部工具欄
        TopToolbar(
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onSave = {
                // 保存功能
                viewModel.saveModel(
                    name = "模型_${System.currentTimeMillis()}",
                    x = 0f,
                    y = 0f,
                    z = 0f,
                    rotationX = 0f,
                    rotationY = 0f,
                    rotationZ = 0f,
                    scale = 1f,
                    imagePath = ""
                )
            },
            onScreenshot = {
                // 截圖功能 - 保存到模型列表
                takeScreenshotAndSaveToModel(context, viewModel, currentModelId)
            }
        )



        // 統一控制面板
        UnifiedControlPanel(
            selectedLayer = currentLayers.find { it.id == selectedLayerId },
            selectedLayers = currentLayers.filter { selectedLayerIds.contains(it.id) },
            selectedLayerIds = selectedLayerIds,
            isMultiSelectMode = isMultiSelectMode,
            backgroundRotation = backgroundRotation,
            backgroundScale = backgroundScale,
            panelState = panelState,
            onPanelStateChange = { panelState = it },
            onLayerUpdate = { layerId, updatedLayer ->
                viewModel.updateLayer(updatedLayer)
            },
            onBatchOperation = { layers ->
                layers.forEach { layer ->
                    viewModel.updateLayer(layer)
                }
            },
            onRotationChange = { viewModel.setBackgroundRotation(it) },
            onScaleChange = { backgroundScale = it },
            onAddLayer = { imagePath ->
                viewModel.addLayer(
                    modelId = currentModelId ?: 0L,
                    imagePath = imagePath,
                    x = centerX,
                    y = centerY,
                    scale = 1f,
                    rotation = 0f,
                    alpha = 1f,
                    name = "預設圖片"
                )
            },
            onCopyLayer = { layer ->
                viewModel.addLayer(
                    modelId = currentModelId ?: 0L,
                    imagePath = layer.imagePath,
                    x = layer.x,
                    y = layer.y,
                    scale = layer.scale,
                    rotation = layer.rotation,
                    alpha = layer.alpha,
                    name = layer.name
                )
            },
            onAddImage = { showImageSelector = true },
            onAddCustomImage = { customImageLauncher.launch("image/*") },
            onToggleMultiSelect = {
                isMultiSelectMode = !isMultiSelectMode
                if (!isMultiSelectMode) viewModel.clearSelection()
            },
            onClearSelection = { viewModel.clearSelection() },
            selectedCount = selectedLayerIds.size,
            viewModel = viewModel
        )



        // 圖片選擇器對話框
        if (showImageSelector) {
            ImageSelectorDialog(
                onDismiss = { showImageSelector = false },
                onImageSelected = { imagePath ->
                                        viewModel.addLayer(
                        modelId = currentModelId ?: 0L,
                        imagePath = imagePath,
                                            x = centerX,
                                            y = centerY,
                                            scale = 1f,
                                            rotation = 0f,
                                            alpha = 1f,
                        name = "預設圖片"
                                        )
                                        showImageSelector = false
                }
            )
        }
    }
}

// 頂部工具欄組件
@Composable
fun TopToolbar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onSave: () -> Unit,
    onScreenshot: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(
                onClick = onUndo,
                enabled = canUndo
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "撤銷")
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo
            ) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "重做")
            }
        }

        Row {
            IconButton(onClick = onScreenshot) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "截圖")
            }
            
            IconButton(onClick = onSave) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "保存")
            }
        }
    }
}

// 左側圖片選擇器組件
@Composable
fun LeftImageSelector(
    onAddImage: () -> Unit,
    onAddCustomImage: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onClearSelection: () -> Unit,
    isMultiSelectMode: Boolean,
    selectedCount: Int
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "圖片管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = onAddImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加預設圖片")
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加預設圖片")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddCustomImage,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加自定義圖片")
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加自定義圖片")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onToggleMultiSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMultiSelectMode)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.List, contentDescription = "多選模式")
                Spacer(modifier = Modifier.width(8.dp))
                Text("多選模式")
            }

            if (isMultiSelectMode && selectedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "已選擇 $selectedCount 個圖層",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onClearSelection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "清除選擇")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除選擇")
                }
            }
        }
    }
}

// 右側圖層控制面板組件
@Composable
fun RightLayerControlPanel(
    selectedLayer: ImageLayer?,
    selectedLayers: List<ImageLayer>,
    selectedLayerIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onLayerUpdate: (Long, ImageLayer) -> Unit,
    onBatchOperation: (List<ImageLayer>) -> Unit,
    controlPanelHeight: Dp,
    controlPanelOffsetY: Dp,
    isControlPanelExpanded: Boolean,
    onControlPanelDrag: (Dp) -> Unit,
    onControlPanelHeightChange: (Dp) -> Unit,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onAddLayer: (String) -> Unit,
    onCopyLayer: (ImageLayer) -> Unit,
    viewModel: ModelViewModel,
    isExpanded: Boolean = true,
    onToggleExpanded: (Boolean) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .height(controlPanelHeight)
                .offset(x = 0.dp, y = controlPanelOffsetY)
                .padding(16.dp)
                .align(Alignment.TopEnd),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 標題欄
            Row(
                modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "圖層控制",
                style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )
            
                Row {
                // 展開/收起按鈕
                IconButton(
                        onClick = {
                            onToggleExpanded(!isExpanded)
                        }
                ) {
                    Icon(
                            if (isExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            "收起/展開",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                // 高度調整按鈕
                IconButton(
                        onClick = {
                            onControlPanelHeightChange(
                                if (isControlPanelExpanded) 60.dp else 400.dp
                            )
                        }
                ) {
                    Icon(
                            if (isControlPanelExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            "調整高度",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // 分頁標籤
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    item {
                        TabButton(
                            text = "移動",
                            isSelected = selectedTab == 0,
                            onClick = { onTabChange(0) }
                        )
                    }
                    item {
                        TabButton(
                            text = "變形",
                            isSelected = selectedTab == 1,
                            onClick = { onTabChange(1) }
                        )
                    }
                    item {
                        TabButton(
                            text = "動作",
                            isSelected = selectedTab == 2,
                            onClick = { onTabChange(2) }
                        )
                    }
                    item {
                        TabButton(
                            text = "模型圖片",
                            isSelected = selectedTab == 3,
                            onClick = { onTabChange(3) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 分頁內容
                when (selectedTab) {
                    0 -> MovementTab(
                        selectedLayer = selectedLayer,
                        selectedLayers = selectedLayers,
                        isMultiSelectMode = isMultiSelectMode,
                        onLayerUpdate = onLayerUpdate,
                        onBatchOperation = onBatchOperation
                    )
                    1 -> TransformTab(
                        selectedLayer = selectedLayer,
                        selectedLayers = selectedLayers,
                        isMultiSelectMode = isMultiSelectMode,
                        onLayerUpdate = onLayerUpdate,
                        onBatchOperation = onBatchOperation
                    )
                    2 -> ActionTab(
                        selectedLayer = selectedLayer,
                        selectedLayers = selectedLayers,
                        selectedLayerIds = selectedLayerIds,
                        isMultiSelectMode = isMultiSelectMode,
                        onLayerUpdate = onLayerUpdate,
                        onBatchOperation = onBatchOperation,
                        onCopyLayer = onCopyLayer,
                        viewModel = viewModel
                    )
                    3 -> ModelImagesTab(
                        onImageSelected = onAddLayer,
                        viewModel = viewModel
                    )
                }
            }
        }
        }
    }
}

// 分頁按鈕組件
@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            ) {
                Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 移動分頁
@Composable
fun MovementTab(
    selectedLayer: ImageLayer?,
    selectedLayers: List<ImageLayer>,
    isMultiSelectMode: Boolean,
    onLayerUpdate: (Long, ImageLayer) -> Unit,
    onBatchOperation: (List<ImageLayer>) -> Unit
) {
    Column {
                    Text(
            "位置控制",
                        style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
                    )
                    
        Spacer(modifier = Modifier.height(8.dp))

        // 方向控制按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
            IconButton(
                onClick = {
                    if (isMultiSelectMode) {
                        onBatchOperation(selectedLayers.map { it.copy(y = it.y - 10f) })
                    } else {
                        selectedLayer?.let { layer ->
                            onLayerUpdate(layer.id, layer.copy(y = layer.y - 10f))
                        }
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "向上")
            }
        }

            Row(
                modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                onClick = {
                    if (isMultiSelectMode) {
                        onBatchOperation(selectedLayers.map { it.copy(x = it.x - 10f) })
                    } else {
                        selectedLayer?.let { layer ->
                            onLayerUpdate(layer.id, layer.copy(x = layer.x - 10f))
                        }
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "向左")
                }
                
                IconButton(
                onClick = {
                    if (isMultiSelectMode) {
                        onBatchOperation(selectedLayers.map { it.copy(x = it.x + 10f) })
                    } else {
                        selectedLayer?.let { layer ->
                            onLayerUpdate(layer.id, layer.copy(x = layer.x + 10f))
                        }
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "向右")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                onClick = {
                    if (isMultiSelectMode) {
                        onBatchOperation(selectedLayers.map { it.copy(y = it.y + 10f) })
                    } else {
                        selectedLayer?.let { layer ->
                            onLayerUpdate(layer.id, layer.copy(y = layer.y + 10f))
                        }
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "向下")
            }
        }
    }
}

// 變形分頁
@Composable
fun TransformTab(
    selectedLayer: ImageLayer?,
    selectedLayers: List<ImageLayer>,
    isMultiSelectMode: Boolean,
    onLayerUpdate: (Long, ImageLayer) -> Unit,
    onBatchOperation: (List<ImageLayer>) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            "變形控制",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        if (selectedLayer == null && selectedLayers.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "請先選擇圖層",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // 縮放控制
        Text(
            "縮放: ${String.format("%.2f", selectedLayer?.scale ?: 1f)}x",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = selectedLayer?.scale ?: 1f,
            onValueChange = { scale ->
                if (isMultiSelectMode) {
                    onBatchOperation(selectedLayers.map { it.copy(scale = scale) })
                } else {
                    selectedLayer?.let { layer ->
                        onLayerUpdate(layer.id, layer.copy(scale = scale))
                    }
                }
            },
            valueRange = 0.1f..5f,
            enabled = selectedLayer != null || selectedLayers.isNotEmpty()
        )

        // 旋轉控制
        Text(
            "旋轉: ${String.format("%.0f", selectedLayer?.rotation ?: 0f)}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = selectedLayer?.rotation ?: 0f,
            onValueChange = { rotation ->
                if (isMultiSelectMode) {
                    onBatchOperation(selectedLayers.map { it.copy(rotation = rotation) })
                } else {
                    selectedLayer?.let { layer ->
                        onLayerUpdate(layer.id, layer.copy(rotation = rotation))
                    }
                }
            },
            valueRange = 0f..360f,
            enabled = selectedLayer != null || selectedLayers.isNotEmpty()
        )

        // 透明度控制
        Text(
            "透明度: ${String.format("%.2f", selectedLayer?.alpha ?: 1f)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = selectedLayer?.alpha ?: 1f,
            onValueChange = { alpha ->
                if (isMultiSelectMode) {
                    onBatchOperation(selectedLayers.map { it.copy(alpha = alpha) })
                } else {
                    selectedLayer?.let { layer ->
                        onLayerUpdate(layer.id, layer.copy(alpha = alpha))
                    }
                }
            },
            valueRange = 0f..1f,
            enabled = selectedLayer != null || selectedLayers.isNotEmpty()
        )
    }
}

// 動作分頁
@Composable
fun ActionTab(
    selectedLayer: ImageLayer?,
    selectedLayers: List<ImageLayer>,
    selectedLayerIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onLayerUpdate: (Long, ImageLayer) -> Unit,
    onBatchOperation: (List<ImageLayer>) -> Unit,
    onCopyLayer: (ImageLayer) -> Unit,
    viewModel: ModelViewModel
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            "動作控制",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 自動對齊按鈕組
        Column {
            // 對齊到中心
            Button(
                onClick = {
                    if (isMultiSelectMode) {
                        // 批量自動對齊到中心
                        selectedLayers.forEach { layer ->
                            val centerX = 200f // 屏幕中心X
                            val centerY = 300f // 屏幕中心Y
                            val alignedLayer = layer.copy(
                                x = centerX,
                                y = centerY
                            )
                            onLayerUpdate(layer.id, alignedLayer)
                        }
                    } else {
                        selectedLayer?.let { layer ->
                            // 單個圖層自動對齊到中心
                            val centerX = 200f // 屏幕中心X
                            val centerY = 300f // 屏幕中心Y
                            val alignedLayer = layer.copy(
                                x = centerX,
                                y = centerY
                            )
                            onLayerUpdate(layer.id, alignedLayer)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "對齊到中心")
                Spacer(modifier = Modifier.width(8.dp))
                Text("對齊到中心")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 圖層之間對齊
            if (isMultiSelectMode && selectedLayers.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            // 水平對齊 - 以第一個圖層為基準
                            val firstLayer = selectedLayers.first()
                            selectedLayers.forEach { layer ->
                                if (layer.id != firstLayer.id) {
                                    val alignedLayer = layer.copy(x = firstLayer.x)
                                    onLayerUpdate(layer.id, alignedLayer)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("水平對齊", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            // 垂直對齊 - 以第一個圖層為基準
                            val firstLayer = selectedLayers.first()
                            selectedLayers.forEach { layer ->
                                if (layer.id != firstLayer.id) {
                                    val alignedLayer = layer.copy(y = firstLayer.y)
                                    onLayerUpdate(layer.id, alignedLayer)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("垂直對齊", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            // 等距分布 - 水平
                            val sortedLayers = selectedLayers.sortedBy { it.x }
                            val minX = sortedLayers.first().x
                            val maxX = sortedLayers.last().x
                            val spacing = if (sortedLayers.size > 1) (maxX - minX) / (sortedLayers.size - 1) else 0f
                            
                            sortedLayers.forEachIndexed { index, layer ->
                                val alignedLayer = layer.copy(x = minX + index * spacing)
                                onLayerUpdate(layer.id, alignedLayer)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("等距分布", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            // 等距分布 - 垂直
                            val sortedLayers = selectedLayers.sortedBy { it.y }
                            val minY = sortedLayers.first().y
                            val maxY = sortedLayers.last().y
                            val spacing = if (sortedLayers.size > 1) (maxY - minY) / (sortedLayers.size - 1) else 0f
                            
                            sortedLayers.forEachIndexed { index, layer ->
                                val alignedLayer = layer.copy(y = minY + index * spacing)
                                onLayerUpdate(layer.id, alignedLayer)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("垂直分布", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 複製按鈕
        Button(
            onClick = {
                if (isMultiSelectMode) {
                    // 批量複製 - 使用ViewModel的duplicateMultipleLayers方法
                    viewModel.duplicateMultipleLayers(selectedLayerIds)
                } else {
                    selectedLayer?.let { layer ->
                        // 單個複製 - 使用ViewModel的duplicateLayer方法
                        viewModel.duplicateLayer(layer.id)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedLayer != null || selectedLayers.isNotEmpty()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "複製")
            Spacer(modifier = Modifier.width(8.dp))
            Text("複製")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 多選模式控制
        if (isMultiSelectMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "多選模式控制",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            "已選擇: ${selectedLayerIds.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 批量操作按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                selectedLayers.forEach { layer ->
                                    val updatedLayer = layer.copy(
                                        scale = (layer.scale * 1.1f).coerceAtMost(5f)
                                    )
                                    onLayerUpdate(layer.id, updatedLayer)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("放大", fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                selectedLayers.forEach { layer ->
                                    val updatedLayer = layer.copy(
                                        scale = (layer.scale * 0.9f).coerceAtLeast(0.1f)
                                    )
                                    onLayerUpdate(layer.id, updatedLayer)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("縮小", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                selectedLayers.forEach { layer ->
                                    val updatedLayer = layer.copy(
                                        rotation = (layer.rotation + 15f) % 360f
                                    )
                                    onLayerUpdate(layer.id, updatedLayer)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("旋轉+", fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                selectedLayers.forEach { layer ->
                                    val updatedLayer = layer.copy(
                                        alpha = (layer.alpha - 0.1f).coerceAtLeast(0f)
                                    )
                                    onLayerUpdate(layer.id, updatedLayer)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("透明-", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.clearSelection() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清除選擇",
                    modifier = Modifier.size(16.dp)
                )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除選擇")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 刪除按鈕
        Button(
            onClick = {
                if (isMultiSelectMode) {
                    // 批量刪除 - 使用ViewModel的deleteMultipleLayers方法
                    viewModel.deleteMultipleLayers(selectedLayerIds)
                } else {
                    selectedLayer?.let { layer ->
                        // 單個刪除 - 使用ViewModel的deleteLayer方法
                        viewModel.deleteLayer(layer)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            enabled = selectedLayer != null || selectedLayers.isNotEmpty()
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "刪除")
            Spacer(modifier = Modifier.width(8.dp))
            Text("刪除")
        }
    }
}

// 左側背景控制面板組件
@Composable
fun LeftBackgroundControlPanel(
    backgroundRotation: Float,
    backgroundScale: Float,
    onRotationChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "背景控制",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = { onToggleExpanded(!isExpanded) }
                ) {
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展開",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 背景旋轉控制
                    Text(
                        "背景旋轉",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { onRotationChange(0f) },
        modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (backgroundRotation == 0f)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "0度",
                                tint = if (backgroundRotation == 0f)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRotationChange(90f) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (backgroundRotation == 90f)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "90度",
                                tint = if (backgroundRotation == 90f)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRotationChange(180f) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (backgroundRotation == 180f)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "180度",
                                tint = if (backgroundRotation == 180f)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRotationChange(270f) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (backgroundRotation == 270f)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "270度",
                                tint = if (backgroundRotation == 270f)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 背景縮放控制
                    Text(
                        "背景縮放 (0.1x - 3x)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Slider(
                        value = backgroundScale,
                        onValueChange = { onScaleChange(it) },
                        valueRange = 0.1f..3f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "縮放比例: ${String.format("%.1f", backgroundScale)}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 圖片選擇器對話框組件
@Composable
fun ImageSelectorDialog(
    onDismiss: () -> Unit,
    onImageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "選擇圖片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetImages) { presetImage ->
                    Card(
                        modifier = Modifier
                            .clickable { onImageSelected(presetImage.path) }
                            .padding(4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Image(
                                painter = painterResource(presetImage.resourceId),
                                contentDescription = presetImage.displayName,
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = presetImage.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 互動式圖片圖層組件
@Composable
fun InteractiveImageLayer(
    layer: ImageLayer,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onLayerClick: () -> Unit,
    onLayerUpdate: (ImageLayer) -> Unit
) {
    val context = LocalContext.current
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // 當圖層位置改變時重置偏移
    LaunchedEffect(layer.x, layer.y) {
        offsetX = 0f
        offsetY = 0f
    }
    
    Box(
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) { (layer.x + offsetX).toDp() },
                y = with(LocalDensity.current) { (layer.y + offsetY).toDp() }
            )
            .size(100.dp)
            .clickable { onLayerClick() }
            .pointerInput(layer.id) {
                detectDragGestures(
                    onDragStart = { /* 拖拽開始 */ },
                    onDragEnd = { 
                        // 拖拽結束時更新圖層位置
                        onLayerUpdate(layer.copy(
                            x = layer.x + offsetX,
                            y = layer.y + offsetY
                        ))
                        offsetX = 0f
                        offsetY = 0f
                    }
                ) { change, dragAmount ->
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .zIndex(layer.zIndex.toFloat())
    ) {
        when {
            layer.imagePath.startsWith("drawable://") -> {
                // 顯示預設圖片
                val imageName = layer.imagePath.removePrefix("drawable://")
                val resourceId = when (imageName) {
                    "angle" -> R.drawable.angle
                    "conner" -> R.drawable.conner
                    "hide_ac" -> R.drawable.hide_ac
                    "pipe" -> R.drawable.pipe
                    "wall_ac" -> R.drawable.wall_ac
                    else -> R.drawable.angle
                }

                Image(
                    painter = painterResource(resourceId),
                    contentDescription = layer.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(layer.scale)
                        .rotate(layer.rotation)
                        .alpha(layer.alpha)
                        .border(
                            if (isSelected) 2.dp else 0.dp,
                            if (isSelected)
                                if (isMultiSelectMode) Color.Blue
                                else MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        ),
                    contentScale = ContentScale.Fit
                )
            }
            layer.imagePath.startsWith("custom://") -> {
                // 顯示自定義圖片（從應用私有目錄）
                val filePath = layer.imagePath.removePrefix("custom://")
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(layer.scale)
                        .rotate(layer.rotation)
                        .alpha(layer.alpha)
                        .border(
                            if (isSelected) 2.dp else 0.dp,
                            if (isSelected)
                                if (isMultiSelectMode) Color.Blue
                                else MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        ),
                    update = { imageView ->
                        try {
                            val file = java.io.File(filePath)
                            if (file.exists() && file.canRead()) {
                                val bitmap = BitmapFactory.decodeFile(filePath)
                                if (bitmap != null) {
                                    imageView.setImageBitmap(bitmap)
                                } else {
                                    // 處理錯誤 - 顯示錯誤指示器
                                    imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                }
                            } else {
                                // 文件不存在或無法讀取
                                imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        } catch (e: Exception) {
                            // 處理錯誤 - 顯示錯誤指示器
                            e.printStackTrace()
                            imageView.setBackgroundColor(AndroidColor.LTGRAY)
                            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                        }
                    }
                )
            }
            else -> {
                // 顯示其他類型的圖片（URI等）
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(layer.scale)
                        .rotate(layer.rotation)
                        .alpha(layer.alpha)
                        .border(
                            if (isSelected) 2.dp else 0.dp,
                            if (isSelected)
                                if (isMultiSelectMode) Color.Blue
                                else MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        ),
                    update = { imageView ->
                        try {
                            val uri = Uri.parse(layer.imagePath)
                            val inputStream = context.contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                if (bitmap != null) {
                                    imageView.setImageBitmap(bitmap)
                                } else {
                                    imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                }
                                inputStream.close()
                            } else {
                                imageView.setBackgroundColor(AndroidColor.LTGRAY)
                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                            }
                        } catch (e: Exception) {
                            // 處理錯誤 - 顯示錯誤指示器
                            e.printStackTrace()
                            imageView.setBackgroundColor(AndroidColor.LTGRAY)
                            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                        }
                    }
                )
            }
        }

        // 選擇指示器
        if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    .border(
                        2.dp,
                        if (isMultiSelectMode) Color.Blue else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// 模型圖片分頁組件
@Composable
fun ModelImagesTab(
    onImageSelected: (String) -> Unit,
    viewModel: ModelViewModel
) {
    val customImages by viewModel.customImages.collectAsState()
    
    Column {
        Text(
            "模型圖片",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 預設圖片網格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(presetImages) { presetImage ->
                Card(
                    modifier = Modifier
                        .clickable { onImageSelected(presetImage.path) }
                        .padding(4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            painter = painterResource(presetImage.resourceId),
                            contentDescription = presetImage.displayName,
                            modifier = Modifier
                                .size(60.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = presetImage.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // 自定義圖片
            items(customImages) { customImage ->
                Card(
                    modifier = Modifier
                        .clickable { onImageSelected("custom://${customImage.filePath}") }
                        .padding(4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            val bitmap = remember(customImage.filePath) {
                                try {
                                    val file = java.io.File(customImage.filePath)
                                    if (file.exists() && file.canRead()) {
                                        BitmapFactory.decodeFile(customImage.filePath)
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }
                            }
                            
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = customImage.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "圖片載入失敗",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = customImage.name,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// 多選模式控制面板組件
@Composable
fun MultiSelectControlPanel(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onBatchOperation: ((ImageLayer) -> ImageLayer) -> Unit,
    selectedLayers: List<ImageLayer>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "多選模式控制",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    "已選擇: $selectedCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 批量操作按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onBatchOperation { layer ->
                            layer.copy(
                                scale = (layer.scale * 1.1f).coerceAtMost(5f)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("放大", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = {
                        onBatchOperation { layer ->
                            layer.copy(
                                scale = (layer.scale * 0.9f).coerceAtLeast(0.1f)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("縮小", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onBatchOperation { layer ->
                            layer.copy(
                                rotation = (layer.rotation + 15f) % 360f
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("旋轉+", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = {
                        onBatchOperation { layer ->
                            layer.copy(
                                alpha = (layer.alpha - 0.1f).coerceAtLeast(0f)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("透明-", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClearSelection,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清除選擇",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除選擇")
            }
        }
    }
}

// 截圖功能
fun takeScreenshot(context: Context) {
    try {
        // 獲取根視圖
        val activity = context as? androidx.activity.ComponentActivity
        val rootView = activity?.window?.decorView?.rootView
        
        if (rootView != null) {
            // 創建截圖
            val bitmap = Bitmap.createBitmap(
                rootView.width,
                rootView.height,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)
            
            // 保存截圖到相冊
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Air_AR_Screenshot_$timestamp.jpg"
            
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val airDir = File(picturesDir, "Air_AR")
            if (!airDir.exists()) {
                airDir.mkdirs()
            }
            
            val file = File(airDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // 通知媒體掃描器
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null,
                null
            )
            
            // 顯示成功提示
            android.widget.Toast.makeText(
                context,
                "截圖已保存到相冊: $fileName",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "截圖失敗: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

// 截圖並保存到模型列表功能
fun takeScreenshotAndSaveToModel(context: Context, viewModel: ModelViewModel, currentModelId: Long?) {
    try {
        // 獲取根視圖
        val activity = context as? androidx.activity.ComponentActivity
        val rootView = activity?.window?.decorView?.rootView
        
        if (rootView != null) {
            // 創建截圖
            val bitmap = Bitmap.createBitmap(
                rootView.width,
                rootView.height,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)
            
            // 保存截圖到應用私有目錄
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "model_screenshot_$timestamp.jpg"
            
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // 保存模型到數據庫
            val modelName = "模型截圖_$timestamp"
            viewModel.saveModel(
                name = modelName,
                x = 0f,
                y = 0f,
                z = 0f,
                rotationX = 0f,
                rotationY = 0f,
                rotationZ = 0f,
                scale = 1f,
                imagePath = file.absolutePath
            )
            
            // 顯示成功提示
            android.widget.Toast.makeText(
                context,
                "截圖已保存到模型列表: $modelName",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "截圖保存失敗: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

// 統一控制面板組件
@Composable
fun UnifiedControlPanel(
    selectedLayer: ImageLayer?,
    selectedLayers: List<ImageLayer>,
    selectedLayerIds: Set<Long>,
    isMultiSelectMode: Boolean,
    backgroundRotation: Float,
    backgroundScale: Float,
    panelState: PanelState,
    onPanelStateChange: (PanelState) -> Unit,
    onLayerUpdate: (Long, ImageLayer) -> Unit,
    onBatchOperation: (List<ImageLayer>) -> Unit,
    onRotationChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onAddLayer: (String) -> Unit,
    onCopyLayer: (ImageLayer) -> Unit,
    onAddImage: () -> Unit,
    onAddCustomImage: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onClearSelection: () -> Unit,
    selectedCount: Int,
    viewModel: ModelViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .height(if (panelState.isExpanded) panelState.height else 80.dp)
                .offset(x = 0.dp, y = panelState.offsetY)
                .padding(16.dp)
                .align(Alignment.TopEnd),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 標題欄 - 始終顯示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "統一控制面板",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        // 展開/收起按鈕 - 始終顯示
                        IconButton(
                            onClick = {
                                onPanelStateChange(panelState.copy(isExpanded = !panelState.isExpanded))
                            }
                        ) {
                            Icon(
                                if (panelState.isExpanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                "收起/展開",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // 高度調整按鈕 - 只在展開時顯示
                        if (panelState.isExpanded) {
                            IconButton(
                                onClick = {
                                    onPanelStateChange(
                                        panelState.copy(
                                            height = if (panelState.height == 400.dp) 60.dp else 400.dp
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    if (panelState.height == 400.dp)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,
                                    "調整高度",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                if (panelState.isExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 分頁標籤
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        item {
                            TabButton(
                                text = "移動",
                                isSelected = panelState.selectedTab == 0,
                                onClick = { onPanelStateChange(panelState.copy(selectedTab = 0)) }
                            )
                        }
                        item {
                            TabButton(
                                text = "變形",
                                isSelected = panelState.selectedTab == 1,
                                onClick = { onPanelStateChange(panelState.copy(selectedTab = 1)) }
                            )
                        }
                        item {
                            TabButton(
                                text = "動作",
                                isSelected = panelState.selectedTab == 2,
                                onClick = { onPanelStateChange(panelState.copy(selectedTab = 2)) }
                            )
                        }
                        item {
                            TabButton(
                                text = "背景",
                                isSelected = panelState.selectedTab == 3,
                                onClick = { onPanelStateChange(panelState.copy(selectedTab = 3)) }
                            )
                        }
                        item {
                            TabButton(
                                text = "圖片管理",
                                isSelected = panelState.selectedTab == 4,
                                onClick = { onPanelStateChange(panelState.copy(selectedTab = 4)) }
                            )
                        }
                        item {
                            TabButton(
                                text = "模型圖片",
                                isSelected = panelState.selectedTab == 5,
                                onClick = { onPanelStateChange(panelState.copy(selectedTab = 5)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 分頁內容
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // 固定高度確保內容可見
                    ) {
                        when (panelState.selectedTab) {
                            0 -> MovementTab(
                                selectedLayer = selectedLayer,
                                selectedLayers = selectedLayers,
                                isMultiSelectMode = isMultiSelectMode,
                                onLayerUpdate = onLayerUpdate,
                                onBatchOperation = onBatchOperation
                            )
                            1 -> TransformTab(
                                selectedLayer = selectedLayer,
                                selectedLayers = selectedLayers,
                                isMultiSelectMode = isMultiSelectMode,
                                onLayerUpdate = onLayerUpdate,
                                onBatchOperation = onBatchOperation
                            )
                            2 -> ActionTab(
                                selectedLayer = selectedLayer,
                                selectedLayers = selectedLayers,
                                selectedLayerIds = selectedLayerIds,
                                isMultiSelectMode = isMultiSelectMode,
                                onLayerUpdate = onLayerUpdate,
                                onBatchOperation = onBatchOperation,
                                onCopyLayer = onCopyLayer,
                                viewModel = viewModel
                            )
                            3 -> BackgroundControlTab(
                                backgroundRotation = backgroundRotation,
                                backgroundScale = backgroundScale,
                                onRotationChange = onRotationChange,
                                onScaleChange = onScaleChange
                            )
                            4 -> ImageManagementTab(
                                onAddImage = onAddImage,
                                onAddCustomImage = onAddCustomImage,
                                onToggleMultiSelect = onToggleMultiSelect,
                                onClearSelection = onClearSelection,
                                isMultiSelectMode = isMultiSelectMode,
                                selectedCount = selectedCount
                            )
                            5 -> ModelImagesTab(
                                onImageSelected = onAddLayer,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

// 背景控制分頁組件
@Composable
fun BackgroundControlTab(
    backgroundRotation: Float,
    backgroundScale: Float,
    onRotationChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit
) {
    Column {
        Text(
            "背景控制",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 背景旋轉控制
        Text(
            "背景旋轉",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { onRotationChange(0f) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (backgroundRotation == 0f)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "0度",
                    tint = if (backgroundRotation == 0f)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { onRotationChange(90f) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (backgroundRotation == 90f)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "90度",
                    tint = if (backgroundRotation == 90f)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { onRotationChange(180f) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (backgroundRotation == 180f)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "180度",
                    tint = if (backgroundRotation == 180f)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { onRotationChange(270f) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (backgroundRotation == 270f)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "270度",
                    tint = if (backgroundRotation == 270f)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 背景縮放控制
        Text(
            "背景縮放 (0.1x - 3x)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Slider(
            value = backgroundScale,
            onValueChange = { onScaleChange(it) },
            valueRange = 0.1f..3f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "縮放比例: ${String.format("%.1f", backgroundScale)}x",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 圖片管理分頁組件
@Composable
fun ImageManagementTab(
    onAddImage: () -> Unit,
    onAddCustomImage: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onClearSelection: () -> Unit,
    isMultiSelectMode: Boolean,
    selectedCount: Int
) {
    Column {
        Text(
            "圖片管理",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 添加圖片按鈕
        Button(
            onClick = onAddImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加預設圖片",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加預設圖片")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onAddCustomImage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加自定義圖片",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加自定義圖片")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 多選模式控制 - 始終顯示，增強視覺效果
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMultiSelectMode)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Button(
                onClick = onToggleMultiSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(
                    imageVector = if (isMultiSelectMode)
                        Icons.Default.List
                    else
                        Icons.Default.Add,
                    contentDescription = "多選模式",
                    modifier = Modifier.size(20.dp),
                    tint = if (isMultiSelectMode)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (isMultiSelectMode) "退出多選模式" else "啟用多選模式",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isMultiSelectMode)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isMultiSelectMode) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "已選擇: $selectedCount 個圖層",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClearSelection,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清除選擇",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除選擇")
            }
        }
    }
}
