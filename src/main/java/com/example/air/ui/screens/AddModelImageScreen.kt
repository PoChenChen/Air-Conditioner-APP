package com.example.air.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.example.air.data.CustomImage
import com.example.air.ui.ModelViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun AddModelImageScreen(
    navController: NavController,
    viewModel: ModelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customImages by viewModel.customImages.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<CustomImage?>(null) }
    var newImageName by remember { mutableStateOf("") }
    
    // 圖片選擇器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // 保存圖片到應用私有目錄
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val fileName = "custom_image_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                
                // 直接添加到數據庫，不需要對話框
                val imageName = "自定義圖片_${System.currentTimeMillis()}"
                viewModel.addCustomImage(imageName, file.absolutePath)
                
                // 顯示成功提示
                android.widget.Toast.makeText(context, "圖片已添加: $imageName", android.widget.Toast.LENGTH_SHORT).show()
                
            } catch (e: IOException) {
                e.printStackTrace()
                // 顯示錯誤提示
                android.widget.Toast.makeText(context, "圖片保存失敗: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadCustomImages()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 標題和返回按鈕
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "自定義圖片管理",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 ${customImages.size} 張圖片",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(
                onClick = { navController.navigateUp() }
            ) {
                Text("返回")
            }
        }
        
        // 添加圖片按鈕
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加圖片")
            Spacer(modifier = Modifier.width(8.dp))
            Text("從相簿選擇圖片")
        }
        
        // 圖片網格
        if (customImages.isEmpty()) {
            // 空狀態提示
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "沒有圖片",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "還沒有自定義圖片",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "點擊上方按鈕從相簿選擇圖片",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(customImages) { customImage ->
                    CustomImageCard(
                        customImage = customImage,
                        onEdit = {
                            selectedImage = customImage
                            newImageName = customImage.name
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.deleteCustomImage(customImage)
                        }
                    )
                }
            }
        }
    }
    

    
    // 編輯圖片對話框
    if (showEditDialog && selectedImage != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("編輯圖片名稱") },
            text = {
                OutlinedTextField(
                    value = newImageName,
                    onValueChange = { newImageName = it },
                    label = { Text("圖片名稱") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newImageName.isNotBlank()) {
                            selectedImage?.let { image ->
                                viewModel.updateCustomImage(image.copy(name = newImageName))
                            }
                            showEditDialog = false
                            newImageName = ""
                            selectedImage = null
                        }
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CustomImageCard(
    customImage: CustomImage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // 圖片預覽
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            ) {
                val bitmap = remember(customImage.filePath) {
                    try {
                        val file = File(customImage.filePath)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 圖片加載失敗，顯示佔位符
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "圖片載入失敗",
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "圖片載入失敗",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            // 圖片信息和控制按鈕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customImage.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "編輯",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "刪除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
