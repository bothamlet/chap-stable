package com.budgetapp.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.theme.MockPanelRaised
import com.budgetapp.app.ui.theme.MockPrimarySoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileAvatar(
    name: String,
    avatarUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, avatarUri) {
        value = withContext(Dispatchers.IO) {
            avatarUri
                ?.let(Uri::parse)
                ?.let { uri ->
                    runCatching {
                        context.decodeSampledBitmap(uri)
                    }.getOrNull()
                }
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MockPanelRaised),
        contentAlignment = Alignment.Center,
    ) {
        val resolvedBitmap = bitmap
        if (resolvedBitmap != null) {
            Image(
                bitmap = resolvedBitmap.asImageBitmap(),
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                color = MockPrimarySoft,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Context.decodeSampledBitmap(uri: Uri): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = bounds.sampleSizeFor(MaxAvatarDecodePixels)
    }
    return contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun BitmapFactory.Options.sampleSizeFor(maxSidePixels: Int): Int {
    var sampleSize = 1
    while (outWidth / sampleSize > maxSidePixels || outHeight / sampleSize > maxSidePixels) {
        sampleSize *= 2
    }
    return sampleSize
}

private const val MaxAvatarDecodePixels = 512
