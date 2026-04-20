package com.flowpay.app.features.signal_check.presentation

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import com.flowpay.app.FlowPayApplication
import com.flowpay.app.R
import com.flowpay.app.data.SettingsRepository
import com.flowpay.app.features.signal_check.domain.CellularSignalData
import com.flowpay.app.features.signal_check.domain.SignalComparisonResult
import com.flowpay.app.features.signal_check.domain.SignalStrengthAnalyzer
import com.flowpay.app.features.signal_check.domain.WifiSignalData
import com.flowpay.app.ui.theme.BlueAccentTheme
import com.flowpay.app.ui.theme.FlowPayAccentTheme
import com.flowpay.app.ui.theme.FlowPayTheme
import com.flowpay.app.ui.theme.LocalFlowPayAccentTheme
import com.flowpay.app.ui.theme.RedAccentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ──────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────

class SignalComparisonViewModel(application: Application) : AndroidViewModel(application) {
    private val analyzer = SignalStrengthAnalyzer(application)

    var result by mutableStateOf<SignalComparisonResult?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = analyzer.analyze()
            withContext(Dispatchers.Main) {
                result = data
                isLoading = false
            }
        }
    }
}

// ──────────────────────────────────────────────
// Activity
// ──────────────────────────────────────────────

class SignalComparisonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        val app = application as? FlowPayApplication
        val settingsRepository = app?.settingsRepository ?: SettingsRepository(applicationContext)
        val accentTheme = settingsRepository.settingsFlow.value.accentTheme

        setTheme(if (accentTheme == "red") R.style.Theme_FlowPay_Red else R.style.Theme_FlowPay)

        setContent {
            val accent = if (accentTheme == "red") RedAccentTheme else BlueAccentTheme
            CompositionLocalProvider(LocalFlowPayAccentTheme provides accent) {
                FlowPayTheme {
                    SignalComparisonScreen(onBackPressed = { finish() })
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Screen
// ──────────────────────────────────────────────

@Composable
fun SignalComparisonScreen(onBackPressed: () -> Unit) {
    val viewModel: SignalComparisonViewModel = viewModel()
    val result = viewModel.result
    val isLoading = viewModel.isLoading
    val accent = LocalFlowPayAccentTheme.current

    var wifiArcTarget by remember { mutableStateOf(0f) }
    var cellArcTarget by remember { mutableStateOf(0f) }

    val wifiArcProgress by animateFloatAsState(
        targetValue = wifiArcTarget,
        animationSpec = tween(durationMillis = 900, easing = EaseOutQuart),
        label = "wifiArc"
    )
    val cellArcProgress by animateFloatAsState(
        targetValue = cellArcTarget,
        animationSpec = tween(durationMillis = 900, easing = EaseOutQuart),
        label = "cellArc"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    // Auto-refresh every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            delay(2000)
        }
    }

    LaunchedEffect(result) {
        result?.let {
            wifiArcTarget = it.wifi.qualityScore / 100f
            cellArcTarget = it.cellular.qualityScore / 100f
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Top bar ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.headerGradientStart, accent.headerGradientEnd)
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Signal Comparison",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    // Live pulse dot
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = if (!isLoading) pulseAlpha else 1f }
                                .background(
                                    color = if (!isLoading) Color(0xFF4CAF50) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Gauge row ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SignalGaugeCard(
                    modifier = Modifier.weight(1f),
                    label = "WIFI",
                    dbmText = result?.wifi?.rssiDbm?.let { "$it dBm" } ?: "—",
                    qualityLabel = result?.wifi?.qualityLabel ?: "...",
                    progress = wifiArcProgress,
                    accent = accent
                )
                SignalGaugeCard(
                    modifier = Modifier.weight(1f),
                    label = "CELLULAR",
                    dbmText = result?.cellular?.rssiDbm?.let { "$it dBm" } ?: "N/A",
                    qualityLabel = result?.cellular?.qualityLabel ?: "...",
                    progress = cellArcProgress,
                    accent = accent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Band info row ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BandInfoChip(
                    label = result?.wifi?.bandLabel ?: "—",
                    icon = Icons.Default.Wifi
                )
                BandInfoChip(
                    label = result?.cellular?.bandLabel ?: "—",
                    icon = Icons.Default.SignalCellularAlt
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Summary card ─────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, Color(0xFF2A2A2A))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ComparisonBar(
                        wifiScore = result?.wifiPenetrationScore ?: 0f,
                        cellScore = result?.cellularPenetrationScore ?: 0f,
                        accentColor = accent.accent
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = result?.comparisonHeadline ?: "Measuring signal...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result?.comparisonExplanation ?: "",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────
// Gauge card
// ──────────────────────────────────────────────

@Composable
fun SignalGaugeCard(
    modifier: Modifier = Modifier,
    label: String,
    dbmText: String,
    qualityLabel: String,
    progress: Float,
    accent: FlowPayAccentTheme
) {
    val arcColor = arcColorForProgress(progress)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSignalArc(progress, arcColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(32.dp)) // push text down into gauge bowl
                    Text(
                        text = dbmText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = qualityLabel,
                        fontSize = 12.sp,
                        color = arcColor,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun arcColorForProgress(progress: Float): Color {
    return when {
        progress < 0.33f -> lerp(Color(0xFFDC3545), Color(0xFFDC3545), 1f)
        progress < 0.66f -> lerp(Color(0xFFDC3545), Color(0xFFFFB300), (progress - 0.33f) / 0.33f)
        else             -> lerp(Color(0xFFFFB300), Color(0xFF4CAF50), (progress - 0.66f) / 0.34f)
    }
}

private fun DrawScope.drawSignalArc(progress: Float, arcColor: Color) {
    val strokeWidth = 14.dp.toPx()
    val padding = strokeWidth
    val diameter = min(size.width, size.height) - padding * 2
    val topLeft = Offset(
        x = (size.width - diameter) / 2,
        y = (size.height - diameter) / 2
    )
    val arcSize = Size(diameter, diameter)

    // Background track
    drawArc(
        color = Color(0xFF2A2A2A),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Filled arc
    if (progress > 0f) {
        drawArc(
            color = arcColor,
            startAngle = 180f,
            sweepAngle = progress * 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Needle dot at arc tip
        val angleRad = Math.toRadians((180.0 + progress * 180.0))
        val radius = diameter / 2
        val cx = size.width / 2 + radius * cos(angleRad).toFloat()
        val cy = size.height / 2 + radius * sin(angleRad).toFloat()
        drawCircle(
            color = arcColor,
            radius = strokeWidth / 2,
            center = Offset(cx, cy)
        )
    }
}

// ──────────────────────────────────────────────
// Comparison bar
// ──────────────────────────────────────────────

@Composable
fun ComparisonBar(wifiScore: Float, cellScore: Float, accentColor: Color) {
    val total = (wifiScore + cellScore).coerceAtLeast(1f)
    val wifiWeight = (wifiScore / total).coerceAtLeast(0.01f)
    val cellWeight = (cellScore / total).coerceAtLeast(0.01f)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(wifiWeight)
                    .fillMaxHeight()
                    .background(Color(0xFF4A90E2))
            )
            Box(
                modifier = Modifier
                    .weight(cellWeight)
                    .fillMaxHeight()
                    .background(accentColor)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4A90E2), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "WiFi  ${wifiScore.toInt()}",
                    fontSize = 11.sp,
                    color = Color(0xFF4A90E2),
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cellular  ${cellScore.toInt()}",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Band chip
// ──────────────────────────────────────────────

@Composable
fun BandInfoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFFCCCCCC),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

