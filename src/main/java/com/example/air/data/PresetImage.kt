package com.example.air.data

import com.example.air.R

data class PresetImage(
    val name: String,
    val displayName: String,
    val resourceId: Int,
    val path: String = "drawable://$name"
)

// 預設圖片列表
val presetImages = listOf(
    PresetImage("angle", "角度", R.drawable.angle),
    PresetImage("conner", "角落", R.drawable.conner),
    PresetImage("hide_ac", "隱藏式空調", R.drawable.hide_ac),
    PresetImage("pipe", "管道", R.drawable.pipe),
    PresetImage("wall_ac", "壁掛式空調", R.drawable.wall_ac)
)

