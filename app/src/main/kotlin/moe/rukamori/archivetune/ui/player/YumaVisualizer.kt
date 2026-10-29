@file:Suppress("DEPRECATION")

package moe.rukamori.archivetune.ui.player

import android.content.Context
import android.media.audiofx.Visualizer as AndroidVisualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Four native, theme-aware visualizer styles. */
enum class YumaVisualizerStyle(val title: String) {
    SPECTRUM("Spectrum bars"),
    WAVEFORM("Waveform"),
    CIRCULAR("Circular"),
    PARTICLES("Particles")
}

enum class YumaVisualizerPalette(val title: String) {
    APP_THEME("App theme"),
    MINT("Mint"),
    CYAN("Cyan"),
    VIOLET("Violet"),
    ROSE("Rose"),
    AMBER("Amber")
}

/** Place this button beside the existing Now Playing actions. */
@Composable
fun YumaVisualizerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Open music visualizer"
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Outlined.Equalizer,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Theme-aware visualizer dialog.
 * Pass the audioSessionId from the currently audible ExoPlayer/deck.
 * Preferences are persisted locally so style, color, intensity and height survive restarts.
 */
@Composable
fun YumaVisualizerDialog(
    audioSessionId: Int,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("yuma_visualizer_preferences", Context.MODE_PRIVATE)
    }
    val scheme = MaterialTheme.colorScheme

    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", true)) }
    var style by remember {
        mutableStateOf(runCatching {
            YumaVisualizerStyle.valueOf(prefs.getString("style", YumaVisualizerStyle.SPECTRUM.name)!!)
        }.getOrDefault(YumaVisualizerStyle.SPECTRUM))
    }
    var palette by remember {
        mutableStateOf(runCatching {
            YumaVisualizerPalette.valueOf(prefs.getString("palette", YumaVisualizerPalette.APP_THEME.name)!!)
        }.getOrDefault(YumaVisualizerPalette.APP_THEME))
    }
    var intensity by remember { mutableStateOf(prefs.getFloat("intensity", 1.0f).coerceIn(0.5f, 1.8f)) }
    var visualizerHeight by remember { mutableStateOf(prefs.getFloat("height_dp", 176f).coerceIn(100f, 260f)) }

    fun saveEnabled(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean("enabled", value).apply()
    }
    fun saveStyle(value: YumaVisualizerStyle) {
        style = value
        prefs.edit().putString("style", value.name).apply()
    }
    fun savePalette(value: YumaVisualizerPalette) {
        palette = value
        prefs.edit().putString("palette", value.name).apply()
    }
    fun saveIntensity(value: Float) {
        intensity = value
        prefs.edit().putFloat("intensity", value).apply()
    }
    fun saveHeight(value: Float) {
        visualizerHeight = value
        prefs.edit().putFloat("height_dp", value).apply()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp),
            color = scheme.surface.copy(alpha = 0.98f),
            contentColor = scheme.onSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 5.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Equalizer,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Music visualizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Smooth animations · follows your app theme", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close visualizer")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Visualizer", fontWeight = FontWeight.Medium)
                        Text("Turn the animation on or off", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = enabled, onCheckedChange = ::saveEnabled)
                }

                if (enabled) {
                    YumaVisualizerCanvas(
                        audioSessionId = audioSessionId,
                        style = style,
                        palette = palette,
                        intensity = intensity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(visualizerHeight.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(visualizerHeight.dp),
                        color = scheme.surfaceVariant.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Visualizer is off", color = scheme.onSurfaceVariant)
                        }
                    }
                }

                Text("STYLE", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YumaVisualizerStyle.entries.forEach { item ->
                        FilterChip(
                            selected = style == item,
                            onClick = { saveStyle(item) },
                            label = { Text(item.title) }
                        )
                    }
                }

                Text("COLOR", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YumaVisualizerPalette.entries.forEach { item ->
                        FilterChip(
                            selected = palette == item,
                            onClick = { savePalette(item) },
                            label = { Text(item.title) },
                            leadingIcon = {
                                Surface(
                                    modifier = Modifier.size(12.dp),
                                    shape = CircleShape,
                                    color = yumaPaletteColors(item, scheme).first()
                                ) {}
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Intensity", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("${(intensity * 100).toInt()}%", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(value = intensity, onValueChange = ::saveIntensity, valueRange = 0.5f..1.8f)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Visualizer height", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("${visualizerHeight.toInt()} dp", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(value = visualizerHeight, onValueChange = ::saveHeight, valueRange = 100f..260f)
                }

                Text(
                    "App theme uses Yuma's active colors, including dynamic artwork-based colors when enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                Button(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        }
    }
}

private data class YumaAudioFrame(
    val waveform: FloatArray = FloatArray(128),
    val spectrum: FloatArray = FloatArray(64),
    val timestampNanos: Long = 0L
)

@Composable
private fun YumaVisualizerCanvas(
    audioSessionId: Int,
    style: YumaVisualizerStyle,
    palette: YumaVisualizerPalette,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = yumaPaletteColors(palette, scheme)
    val sharedFrame = remember(audioSessionId) { AtomicReference(YumaAudioFrame()) }
    var frame by remember(audioSessionId) { mutableStateOf(YumaAudioFrame()) }
    var displaySpectrum by remember(audioSessionId) { mutableStateOf(FloatArray(64)) }
    var captureError by remember(audioSessionId) { mutableStateOf<String?>(null) }

    val hasRecordAudioPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(audioSessionId, hasRecordAudioPermission) {
        captureError = null
        if (audioSessionId <= 0) {
            captureError = "Start playback, then reopen the visualizer."
            return@LaunchedEffect
        }
        if (!hasRecordAudioPermission) {
            captureError = "Android audio-capture permission is required."
            return@LaunchedEffect
        }

        var audioVisualizer: AndroidVisualizer? = null
        try {
            audioVisualizer = AndroidVisualizer(audioSessionId)
            val captureRange = AndroidVisualizer.getCaptureSizeRange()
            audioVisualizer.captureSize = captureRange[1].coerceAtMost(1024)
            val captureRate = min(AndroidVisualizer.getMaxCaptureRate(), 20_000)

            audioVisualizer.setDataCaptureListener(object : AndroidVisualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: AndroidVisualizer?, waveform: ByteArray?, samplingRate: Int) {
                    val values = waveform?.let(::waveformToFloats) ?: FloatArray(128)
                    val old = sharedFrame.get()
                    sharedFrame.set(old.copy(waveform = values))
                }

                override fun onFftDataCapture(visualizer: AndroidVisualizer?, fft: ByteArray?, samplingRate: Int) {
                    val incoming = fft?.let(::fftToBands) ?: FloatArray(64)
                    val old = sharedFrame.get()
                    val smooth = FloatArray(64) { i ->
                        val prev = old.spectrum.getOrElse(i) { 0f }
                        (prev * 0.56f + incoming.getOrElse(i) { 0f } * 0.44f).coerceIn(0f, 1f)
                    }
                    sharedFrame.set(old.copy(spectrum = smooth))
                }
            }, captureRate, true, true)
            audioVisualizer.enabled = true

            while (isActive) {
                withFrameNanos { now ->
                    val incoming = sharedFrame.get()
                    frame = incoming.copy(timestampNanos = now)
                    val old = displaySpectrum
                    displaySpectrum = FloatArray(64) { i ->
                        val target = incoming.spectrum.getOrElse(i) { 0f }
                        val previous = old.getOrElse(i) { 0f }
                        previous + (target - previous) * 0.24f
                    }
                }
            }
        } catch (e: SecurityException) {
            captureError = "Android denied audio-session capture."
        } catch (e: IllegalStateException) {
            captureError = "This audio session cannot be visualized on this device."
        } catch (_: RuntimeException) {
            captureError = "Visualizer unavailable for the active audio session."
        } finally {
            runCatching { audioVisualizer?.enabled = false }
            runCatching { audioVisualizer?.release() }
        }
    }

    Canvas(modifier = modifier) {
        drawRoundRect(
            color = scheme.surfaceVariant.copy(alpha = 0.28f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
        )
        val themeBrush = Brush.verticalGradient(
            colors = listOf(colors[0].copy(alpha = 0.96f), colors[1].copy(alpha = 0.90f), colors[2].copy(alpha = 0.96f)),
            startY = size.height,
            endY = 0f
        )
        val energy = intensity.coerceIn(0.5f, 1.8f)
        val spectrum = displaySpectrum

        when (style) {
            YumaVisualizerStyle.SPECTRUM -> {
                val count = 42
                val gap = 3.dp.toPx()
                val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
                for (i in 0 until count) {
                    val source = spectrum[(i * spectrum.size / count).coerceIn(0, spectrum.lastIndex)]
                    val envelope = (0.09f + source * energy * 0.88f).coerceIn(0.06f, 0.98f)
                    val barHeight = size.height * envelope
                    val left = i * (barWidth + gap)
                    drawRoundRect(
                        brush = themeBrush,
                        topLeft = androidx.compose.ui.geometry.Offset(left, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }
            }
            YumaVisualizerStyle.WAVEFORM -> {
                val path = Path()
                val samples = frame.waveform
                val midY = size.height / 2f
                val scale = size.height * 0.42f * energy
                if (samples.isNotEmpty()) {
                    path.moveTo(0f, midY + samples.first() * scale)
                    for (i in 1 until samples.size) {
                        val x = size.width * i / (samples.size - 1).coerceAtLeast(1)
                        val y = midY + samples[i] * scale
                        path.lineTo(x, y)
                    }
                    drawPath(path, brush = themeBrush, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    val mirrored = Path()
                    mirrored.moveTo(0f, midY - samples.first() * scale * 0.42f)
                    for (i in 1 until samples.size) {
                        val x = size.width * i / (samples.size - 1).coerceAtLeast(1)
                        mirrored.lineTo(x, midY - samples[i] * scale * 0.42f)
                    }
                    drawPath(mirrored, color = colors[2].copy(alpha = 0.42f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            YumaVisualizerStyle.CIRCULAR -> {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                val minDimension = min(size.width, size.height)
                val baseRadius = minDimension * 0.25f
                val maxLength = minDimension * 0.19f * energy
                val count = 64
                for (i in 0 until count) {
                    val angle = (2.0 * PI * i / count - PI / 2.0).toFloat()
                    val value = spectrum.getOrElse(i) { 0f }
                    val endRadius = baseRadius + maxLength * (0.13f + value * 0.9f)
                    val start = androidx.compose.ui.geometry.Offset(
                        center.x + cos(angle) * baseRadius,
                        center.y + sin(angle) * baseRadius
                    )
                    val end = androidx.compose.ui.geometry.Offset(
                        center.x + cos(angle) * endRadius,
                        center.y + sin(angle) * endRadius
                    )
                    drawLine(
                        color = colors[i % colors.size].copy(alpha = 0.72f + value * 0.28f),
                        start = start,
                        end = end,
                        strokeWidth = (1.5f + value * 2.5f).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(listOf(colors[0].copy(alpha = 0.24f), colors[1].copy(alpha = 0.06f), Color.Transparent), center, baseRadius * 1.2f),
                    radius = baseRadius * 1.16f,
                    center = center
                )
            }
            YumaVisualizerStyle.PARTICLES -> {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val minDimension = min(size.width, size.height)
                val orbit = minDimension * 0.29f
                val maxDrift = minDimension * 0.17f * energy
                val phase = frame.timestampNanos / 1_000_000_000f
                val count = 58
                for (i in 0 until count) {
                    val baseAngle = (2.0 * PI * i / count).toFloat()
                    val frequency = spectrum[(i * spectrum.size / count).coerceIn(0, spectrum.lastIndex)]
                    val sway = sin(phase * (0.6f + i % 5 * 0.09f) + i * 1.7f).toFloat()
                    val radius = orbit + sway * maxDrift * (0.16f + frequency * 0.74f)
                    val angle = baseAngle + phase * (if (i % 2 == 0) 0.12f else -0.09f)
                    val x = centerX + cos(angle) * radius
                    val y = centerY + sin(angle) * radius
                    val dotRadius = (1.3f + frequency * 3.6f + (sway + 1f) * 0.7f).dp.toPx()
                    drawCircle(colors[i % colors.size].copy(alpha = (0.38f + frequency * 0.58f).coerceIn(0.2f, 0.95f)), dotRadius, androidx.compose.ui.geometry.Offset(x, y))
                    if (frequency > 0.58f) {
                        drawCircle(colors[(i + 1) % colors.size].copy(alpha = 0.13f), dotRadius * 2.6f, androidx.compose.ui.geometry.Offset(x, y))
                    }
                }
                drawCircle(
                    brush = Brush.radialGradient(listOf(colors[0].copy(alpha = 0.18f), colors[1].copy(alpha = 0.04f), Color.Transparent), androidx.compose.ui.geometry.Offset(centerX, centerY), orbit * 1.3f),
                    radius = orbit * 1.3f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )
            }
        }

        if (captureError != null) {
            // Draw a subtle, readable status without changing the surrounding player layout.
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = scheme.onSurfaceVariant.copy(alpha = 0.95f).toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.dp.toPx()
                }
                canvas.nativeCanvas.drawText(captureError!!, size.width / 2f, size.height - 12.dp.toPx(), paint)
            }
        }
    }
}

private fun waveformToFloats(bytes: ByteArray): FloatArray {
    if (bytes.isEmpty()) return FloatArray(128)
    return FloatArray(128) { i ->
        val index = (i * bytes.size / 128).coerceIn(0, bytes.lastIndex)
        ((bytes[index].toInt() and 0xff) - 128) / 128f
    }
}

private fun fftToBands(fft: ByteArray, count: Int = 64): FloatArray {
    if (fft.size < 4) return FloatArray(count)
    val bins = (fft.size - 2) / 2
    if (bins < 2) return FloatArray(count)
    return FloatArray(count) { i ->
        val startRatio = i.toDouble() / count
        val endRatio = (i + 1).toDouble() / count
        val low = (1 + (bins - 1) * startRatio.pow(2.15)).toInt().coerceIn(1, bins - 1)
        val high = max(low + 1, (1 + (bins - 1) * endRatio.pow(2.15)).toInt()).coerceAtMost(bins)
        var strongest = 0f
        for (bin in low..high) {
            val real = fft.getOrElse(bin * 2) { 0 }.toInt()
            val imaginary = fft.getOrElse(bin * 2 + 1) { 0 }.toInt()
            val magnitude = sqrt((real * real + imaginary * imaginary).toFloat()) / 128f
            strongest = max(strongest, magnitude)
        }
        (strongest * 1.65f).coerceIn(0f, 1f).pow(0.72f)
    }
}

private fun yumaPaletteColors(palette: YumaVisualizerPalette, scheme: androidx.compose.material3.ColorScheme): List<Color> = when (palette) {
    YumaVisualizerPalette.APP_THEME -> listOf(scheme.primary, scheme.tertiary, scheme.secondary)
    YumaVisualizerPalette.MINT -> listOf(Color(0xFF6EF0B5), Color(0xFF26C99A), Color(0xFFB0FFE4))
    YumaVisualizerPalette.CYAN -> listOf(Color(0xFF68E8FF), Color(0xFF38A6FF), Color(0xFFB2F5FF))
    YumaVisualizerPalette.VIOLET -> listOf(Color(0xFFC7A7FF), Color(0xFF8B5CF6), Color(0xFFFFA6E8))
    YumaVisualizerPalette.ROSE -> listOf(Color(0xFFFF8EBC), Color(0xFFFF5478), Color(0xFFFFC2D7))
    YumaVisualizerPalette.AMBER -> listOf(Color(0xFFFFD166), Color(0xFFFF8A4C), Color(0xFFFFE9A8))
}
