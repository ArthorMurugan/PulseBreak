package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

fun exercisePhotoUrl(mediaUrl: String): String {
    return mediaUrl
        .replace("/videos/", "/images/")
        .replace(Regex("\\.gif$", RegexOption.IGNORE_CASE), ".jpg")
}

@Composable
fun ExerciseMedia(
    mediaUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var usePhotoFallback by remember(mediaUrl) { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLoader = remember(context) {
        coil.ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val source = if (usePhotoFallback) exercisePhotoUrl(mediaUrl) else mediaUrl

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (source.isBlank()) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context).data(source).crossfade(false).build(),
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error && !usePhotoFallback) {
                        usePhotoFallback = true
                    }
                }
            )
        }
    }
}

fun exerciseAccentColor(bodyPart: String, colorScheme: androidx.compose.material3.ColorScheme): Color {
    return when (bodyPart.trim().lowercase()) {
        "chest" -> colorScheme.primary
        "back" -> colorScheme.secondary
        "shoulders", "arms" -> colorScheme.tertiary
        "legs" -> colorScheme.tertiary
        "waist", "cardio" -> colorScheme.secondary
        else -> colorScheme.primary
    }
}
