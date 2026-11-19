package com.shiyin.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** 卡片 16 / 按钮 14 / 封面 24 映射到 Material3 三级形状。 */
val ShiyinShapes = Shapes(
    small = RoundedCornerShape(ShiyinDimens.ButtonCorner),
    medium = RoundedCornerShape(ShiyinDimens.CardCorner),
    large = RoundedCornerShape(ShiyinDimens.CoverCorner)
)

/** 胶囊导航 36dp，供 Tab Bar / 筛选标签单独使用。 */
val ShiyinCapsuleShape = RoundedCornerShape(ShiyinDimens.CapsuleCorner)
