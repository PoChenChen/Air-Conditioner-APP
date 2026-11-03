package com.example.air.ui.screens

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.air.R
import com.example.air.data.ImageLayer
import com.example.air.ui.ModelViewModel
import java.io.File
import java.io.FileOutputStream

// 创建合成图片
fun createCompositeImage(
    context: Context,
    photoPath: String?,
    layers: List<ImageLayer>,
    backgroundRotation: Float,
    backgroundScale: Float
): Bitmap {
    // 创建画布
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    
    // 绘制背景
    photoPath?.let { path ->
        when (path) {
            "transparent_background" -> {
                // 透明背景，绘制白色
                canvas.drawColor(android.graphics.Color.WHITE)
            }
            else -> {
                try {
                    val uri = Uri.parse(path)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val backgroundBitmap = BitmapFactory.decodeStream(inputStream)
                        if (backgroundBitmap != null) {
                            // 保存画布状态
                            canvas.save()
                            // 移动到中心位置
                            canvas.translate(width / 2f, height / 2f)
                            // 应用旋转
                            canvas.rotate(backgroundRotation)
                            // 应用缩放
                            canvas.scale(backgroundScale, backgroundScale)
                            // 绘制背景图片（从中心点开始）
                            canvas.drawBitmap(backgroundBitmap, -backgroundBitmap.width / 2f, -backgroundBitmap.height / 2f, null)
                            // 恢复画布状态
                            canvas.restore()
                        }
                        inputStream.close()
                    }
                } catch (e: Exception) {
                    // 如果背景加载失败，绘制白色背景
                    canvas.drawColor(android.graphics.Color.WHITE)
                }
            }
        }
    }
    
    // 绘制图层
    val visibleLayers = layers.filter { it.isVisible }.sortedBy { it.zIndex }
    visibleLayers.forEach { layer ->
        try {
            val layerBitmap = when {
                layer.imagePath.startsWith("drawable://") -> {
                    // 预设图片
                    val imageName = layer.imagePath.removePrefix("drawable://")
                    val resourceId = when (imageName) {
                        "angle" -> R.drawable.angle
                        "conner" -> R.drawable.conner
                        "hide_ac" -> R.drawable.hide_ac
                        "pipe" -> R.drawable.pipe
                        "wall_ac" -> R.drawable.wall_ac
                        else -> null
                    }
                    resourceId?.let {
                        BitmapFactory.decodeResource(context.resources, it)
                    }
                }
                else -> {
                    // 自定义图片（URI）
                    val uri = Uri.parse(layer.imagePath)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            }
            
            layerBitmap?.let { bitmap ->
                // 应用图层的变换
                val paint = Paint()
                paint.alpha = (layer.alpha * 255).toInt()
                
                // 应用变换并绘制
                canvas.save()
                // 移动到图层位置
                canvas.translate(layer.x, layer.y)
                // 应用旋转
                canvas.rotate(layer.rotation)
                // 应用缩放
                canvas.scale(layer.scale, layer.scale)
                // 绘制图层（从中心点开始）
                canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, paint)
                // 恢复画布状态
                canvas.restore()
            }
        } catch (e: Exception) {
            // 忽略绘制失败的图层
            e.printStackTrace()
        }
    }
    
    return bitmap
}

// 保存圖層到應用目錄
fun saveLayerToAppGallery(context: Context, bitmap: Bitmap): File {
    val userModelDir = File(context.getExternalFilesDir(null), "models")
    if (!userModelDir.exists()) {
        userModelDir.mkdirs()
    }
    
    val file = File(userModelDir, "layer_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}

// 保存圖片到相冊
fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${fileName}_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Air AR")
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let { imageUri ->
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 统一读取「内建 assets + 使用者新增」
fun loadAllModels(context: Context): List<File> {
    val models = mutableListOf<File>()

    // 1. 读取 assets/models (预设内建)
    val assetManager = context.assets
    try {
        val assetFiles = assetManager.list("models") ?: arrayOf()
        assetFiles.forEach { name ->
            // assets 无法直接 File，用 InputStream -> 暂存复制
            val input = assetManager.open("models/$name")
            val cacheFile = File(context.cacheDir, name)
            input.use { it.copyTo(FileOutputStream(cacheFile)) }
            models.add(cacheFile)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // 2. 读取使用者自定 models
    val userModelDir = File(context.getExternalFilesDir(null), "models")
    if (userModelDir.exists()) {
        models.addAll(userModelDir.listFiles() ?: emptyArray())
    }

    return models
}

// 创建截图，排除控制面板
fun createScreenshotWithoutPanels(
    context: Context,
    photoPath: String?,
    layers: List<ImageLayer>,
    backgroundRotation: Float,
    backgroundScale: Float
): Bitmap {
    // 创建画布 - 使用较小的尺寸，排除控制面板区域
    val width = 1080
    val height = 1440  // 减少高度，排除控制面板
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    
    // 绘制背景
    photoPath?.let { path ->
        when (path) {
            "transparent_background" -> {
                // 透明背景，绘制白色
                canvas.drawColor(android.graphics.Color.WHITE)
            }
            else -> {
                try {
                    val uri = Uri.parse(path)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val backgroundBitmap = BitmapFactory.decodeStream(inputStream)
                        if (backgroundBitmap != null) {
                            // 保存画布状态
                            canvas.save()
                            // 移动到中心位置
                            canvas.translate(width / 2f, height / 2f)
                            // 应用旋转
                            canvas.rotate(backgroundRotation)
                            // 应用缩放
                            canvas.scale(backgroundScale, backgroundScale)
                            // 绘制背景图片（从中心点开始）
                            canvas.drawBitmap(backgroundBitmap, -backgroundBitmap.width / 2f, -backgroundBitmap.height / 2f, null)
                            // 恢复画布状态
                            canvas.restore()
                        }
                        inputStream.close()
                    }
                } catch (e: Exception) {
                    // 如果背景加载失败，绘制白色背景
                    canvas.drawColor(android.graphics.Color.WHITE)
                }
            }
        }
    }
    
    // 绘制图层 - 只绘制在截图区域内的图层
    val visibleLayers = layers.filter { it.isVisible }.sortedBy { it.zIndex }
    visibleLayers.forEach { layer ->
        try {
            val layerBitmap = when {
                layer.imagePath.startsWith("drawable://") -> {
                    // 预设图片
                    val imageName = layer.imagePath.removePrefix("drawable://")
                    val resourceId = when (imageName) {
                        "angle" -> R.drawable.angle
                        "conner" -> R.drawable.conner
                        "hide_ac" -> R.drawable.hide_ac
                        "pipe" -> R.drawable.pipe
                        "wall_ac" -> R.drawable.wall_ac
                        else -> null
                    }
                    resourceId?.let {
                        BitmapFactory.decodeResource(context.resources, it)
                    }
                }
                else -> {
                    // 自定义图片（URI）
                    val uri = Uri.parse(layer.imagePath)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            }
            
            layerBitmap?.let { bitmap ->
                // 应用图层的变换
                val paint = Paint()
                paint.alpha = (layer.alpha * 255).toInt()
                
                // 应用变换并绘制
                canvas.save()
                // 移动到图层位置
                canvas.translate(layer.x, layer.y)
                // 应用旋转
                canvas.rotate(layer.rotation)
                // 应用缩放
                canvas.scale(layer.scale, layer.scale)
                // 绘制图层（从中心点开始）
                canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, paint)
                // 恢复画布状态
                canvas.restore()
            }
        } catch (e: Exception) {
            // 忽略绘制失败的图层
            e.printStackTrace()
        }
    }

    return bitmap
}
