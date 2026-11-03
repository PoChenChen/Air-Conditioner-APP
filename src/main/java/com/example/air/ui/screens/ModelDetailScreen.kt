package com.example.air.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.air.R
import com.example.air.data.ImageLayer
import com.example.air.data.ModelData
import com.example.air.ui.ModelViewModel
import kotlinx.coroutines.launch
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.File
import android.content.Intent
import androidx.core.content.FileProvider

@Composable
fun ModelDetailScreen(
    navController: NavController,
    viewModel: ModelViewModel,
    modelId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var modelWithLayers by remember { mutableStateOf<com.example.air.data.ModelWithLayers?>(null) }
    
    LaunchedEffect(modelId) {
        modelWithLayers = viewModel.getModelWithLayers(modelId)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = modelWithLayers?.model?.name ?: "模型詳情",
                fontSize = 24.sp
            )
            
            // Share button
            modelWithLayers?.let { data ->
                Button(
                    onClick = {
                        // 實現分享功能
                        modelWithLayers?.let { data ->
                            // 創建合成圖片並分享
                            val composite = createCompositeImage(
                                context = context,
                                photoPath = data.model.photoPath,
                                layers = data.layers,
                                backgroundRotation = data.model.backgroundRotation,
                                backgroundScale = 1f
                            )
                            saveBitmapToGallery(context, composite, "${data.model.name}_分享")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("分享", fontSize = 14.sp)
                }
            }
        }

        modelWithLayers?.let { data ->
            // Model info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "模型信息",
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("圖層數量: ${data.layers.size}")
                    
                    if (data.model.photoPath != null) {
                        Text("照片: 已拍攝")
                    }
                }
            }

            // Photo preview (if available)
            data.model.photoPath?.let { photoPath ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
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
                                        imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                    }
                                    inputStream.close()
                                } else {
                                    imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                }
                            } catch (e: Exception) {
                                imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Image Preview with overlay
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background photo if available
                    data.model.photoPath?.let { photoPath ->
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
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
                                            imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                        }
                                        inputStream.close()
                                    } else {
                                        imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                    }
                                } catch (e: Exception) {
                                    imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Overlay layers
                    data.layers.sortedBy { it.zIndex }.forEach { layer ->
                        when {
                            layer.imagePath.startsWith("drawable://") -> {
                                val imageName = layer.imagePath.substringAfter("drawable://")
                                val presetImage = when (imageName) {
                                    "angle" -> R.drawable.angle
                                    "conner" -> R.drawable.conner
                                    "hide_ac" -> R.drawable.hide_ac
                                    "pipe" -> R.drawable.pipe
                                    "wall_ac" -> R.drawable.wall_ac
                                    else -> null
                                }
                                presetImage?.let { resourceId ->
                                    Image(
                                        painter = painterResource(id = resourceId),
                                        contentDescription = "Layer Image",
                                        modifier = Modifier
                                            .offset(
                                                x = layer.x.dp,
                                                y = layer.y.dp
                                            )
                                            .scale(layer.scale)
                                            .rotate(layer.rotation)
                                            .alpha(layer.alpha)
                                            .size(100.dp)
                                            .border(2.dp, Color.Blue, RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                            .zIndex(layer.zIndex.toFloat()),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            else -> {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                        }
                                    },
                                    update = { imageView ->
                                        try {
                                            val uri = Uri.parse(layer.imagePath)
                                            val inputStream = context.contentResolver.openInputStream(uri)
                                            if (inputStream != null) {
                                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                                if (bitmap != null) {
                                                    imageView.setImageBitmap(bitmap)
                                                } else {
                                                    imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                                }
                                                inputStream.close()
                                            } else {
                                                imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                            }
                                        } catch (e: Exception) {
                                            imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                        }
                                    },
                                    modifier = Modifier
                                        .offset(
                                            x = layer.x.dp,
                                            y = layer.y.dp
                                        )
                                        .scale(layer.scale)
                                        .rotate(layer.rotation)
                                        .alpha(layer.alpha)
                                        .size(100.dp)
                                        .border(2.dp, Color.Blue, RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .zIndex(layer.zIndex.toFloat())
                                )
                            }
                        }
                    }
                }
            }

            // Layers list
            if (data.layers.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "圖層列表",
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn {
                            items(data.layers.sortedBy { it.zIndex }) { layer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = layer.name.ifEmpty { "未命名" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Text(
                                        text = "縮放: ${String.format("%.2f", layer.scale)}",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    
                                    Text(
                                        text = "透明度: ${String.format("%.2f", layer.alpha)}",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            // Loading or not found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("載入中...")
            }
        }
    }
} 