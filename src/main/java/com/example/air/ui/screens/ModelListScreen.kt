package com.example.air.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.air.data.ModelData
import com.example.air.ui.ModelViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModelListScreen(
    navController: NavController,
    viewModel: ModelViewModel,
    modifier: Modifier = Modifier
) {
    val models by viewModel.models.collectAsState()

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
                text = "已保存的模型",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(onClick = { navController.navigateUp() }) {
                Text("返回")
            }
        }

        if (models.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "還沒有保存的模型\n點擊拍照或直接放置模型來開始",
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(models) { model ->
                    ModelCard(
                        model = model,
                        onView = { navController.navigate("model_detail/${model.id}") },
                        onDelete = { viewModel.deleteModel(model) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelData,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Model preview image
                model.photoPath?.let { photoPath ->
                    Card(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            update = { imageView ->
                                try {
                                    val uri = android.net.Uri.parse(photoPath)
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                    imageView.setImageBitmap(bitmap)
                                    inputStream?.close()
                                } catch (e: Exception) {
                                    // 顯示錯誤背景
                                    imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "創建時間: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(model.timestamp))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (model.photoPath != null) {
                        Text(
                            text = "包含照片",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column {
                    IconButton(onClick = onView) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "查看模型"
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "刪除模型"
                        )
                    }
                }
            }
        }
    }
} 