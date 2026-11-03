package com.example.air.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.air.R
import com.example.air.ui.ModelViewModel

data class BackgroundOption(
    val name: String,
    val displayName: String,
    val resourceId: Int? = null,
    val uri: String? = null
)

@Composable
fun DirectPlacementScreen(
    navController: NavController,
    viewModel: ModelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedBackground by remember { mutableStateOf<BackgroundOption?>(null) }

    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedBackground = BackgroundOption(
                name = "自定義背景",
                displayName = "自定義背景",
                uri = it.toString()
            )
        }
    }

    val presetBackgrounds = remember {
        listOf(
            BackgroundOption("transparent", "透明背景", null, null)
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            
            Text(
                text = "選擇背景",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Background selection
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preset backgrounds
            items(presetBackgrounds) { background ->
                BackgroundOptionCard(
                    background = background,
                    isSelected = selectedBackground?.name == background.name,
                    onClick = { selectedBackground = background }
                )
            }
            
            // Custom background option
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable {
                            launcher.launch("image/*")
                        }
                        .border(
                            width = if (selectedBackground?.name == "自定義背景") 3.dp else 1.dp,
                            color = if (selectedBackground?.name == "自定義背景") Color.Blue else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp)
                ) {
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
                                contentDescription = "添加自定義背景",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("選擇自定義圖片")
                        }
                    }
                }
            }
        }

        // Selected background preview
        selectedBackground?.let { background ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "已選擇背景: ${background.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Background preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                when (background.name) {
                                    "transparent" -> Color.Transparent
                                    else -> Color.LightGray
                                }
                            )
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (background.name) {
                            "transparent" -> Text("透明背景", color = Color.Gray)
                            else -> {
                                // Custom image preview
                                if (background.uri != null) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.widget.ImageView(ctx).apply {
                                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                            }
                                        },
                                        update = { imageView ->
                                            try {
                                                val uri = Uri.parse(background.uri)
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                if (inputStream != null) {
                                                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                                    if (bitmap != null) {
                                                        imageView.setImageBitmap(bitmap)
                                                    } else {
                                                        imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                                    }
                                                    inputStream.close()
                                                } else {
                                                    imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                imageView.setBackgroundColor(android.graphics.Color.LTGRAY)
                                                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                                                println("Error loading image: ${e.message}")
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("自定義背景", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Start placement button
        Button(
            onClick = {
                selectedBackground?.let { background ->
                    // Set the background path
                    when (background.name) {
                        "transparent" -> viewModel.setPhotoPath("transparent_background")
                        else -> viewModel.setPhotoPath(background.uri)
                    }
                    navController.navigate("model_placement")
                }
            },
            enabled = selectedBackground != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("開始放置", fontSize = 18.sp)
        }
    }
}

@Composable
fun BackgroundOptionCard(
    background: BackgroundOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Blue else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when (background.name) {
                        "transparent" -> Color.Transparent
                        else -> Color.LightGray
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = background.displayName,
                color = when (background.name) {
                    "transparent" -> Color.Gray
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}
