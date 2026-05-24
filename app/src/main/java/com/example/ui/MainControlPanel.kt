package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CircleOverlay
import kotlin.math.roundToInt

private var activeToast: Toast? = null

private fun showSingleToast(context: Context, text: String, duration: Int = Toast.LENGTH_SHORT) {
    activeToast?.cancel()
    activeToast = Toast.makeText(context.applicationContext, text, duration).apply {
        show()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainControlPanel(
    viewModel: CircleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val circles by viewModel.circles.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
    val isPositioningMode by viewModel.isPositioningMode.collectAsStateWithLifecycle()
    val isDelayEnabled by viewModel.isDelayEnabled.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = التحكم, 1 = الإعدادات, 2 = التقارير

    // Professional Polish Color Palette (M3 Light Lavender/Purple Theme)
    val bgColor = Color(0xFFFEF7FF) // Main background light lavender
    val cardBgContainer = Color(0xFFF3EDF7) // Soft lavender gray
    val borderColor = Color(0xFFEADDFF) // M3 violet separator border
    val primaryPurple = Color(0xFF6750A4) // Primary action violet
    val darkCharcoal = Color(0xFF1D1B20) // Primary high contrast text
    val mutedPurpleText = Color(0xFF49454F) // Subtitle and support text
    val activePillColor = Color(0xFFE8DEF8) // Tab and chip backgrounds
    val activePillText = Color(0xFF1D192B)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(primaryPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◍", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TouchGuard",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = darkCharcoal,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = darkCharcoal
                ),
                modifier = Modifier.shadow(1.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main scrollable control list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Header Active Banner
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(activePillColor)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isServiceActive) Color(0xFF38A169) else Color(0xFFEF4444), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceActive) "خدمة الخلفية: مفعلة" else "خدمة الخلفية: غير مفعلة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activePillText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when(selectedTab) {
                                0 -> "مؤخر اللمس التخصيصي"
                                1 -> "إعدادات وتراخيص النظام"
                                else -> "تحليلات الأداء والتقارير"
                            },
                            color = darkCharcoal,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = when(selectedTab) {
                                0 -> "تحكم كامل بهدوئك التقني عن طريق وضع دوائر شفافة على الشاشة لتأخير مدة الاستجابة للنقرات في الخلفية."
                                1 -> "قم بإدارة وتنشيط صلاحيات لوحة الضبط والتراجع السريع لتأمين عمل التطبيق بنجاح."
                                else -> "اعرض إحصائيات وعمليات تشغيل مؤخر اللمس ونسب الاستجابة الدقيقة للدوائر."
                            },
                            color = mutedPurpleText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    // High-Fidelity Interactive Visual Preview Canvas Room
                    item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(cardBgContainer)
                            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معاينة توزيع نقاط اللمس",
                                color = darkCharcoal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(primaryPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${circles.filter { it.isEnabled }.size} دوائر نشطة",
                                    color = primaryPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Scaled Interactive Mini Phone Display Canvas
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .border(1.5.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                        ) {
                            val parentWidth = maxWidth
                            val parentHeight = maxHeight

                            // Overlay Grid Representation
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (i in 1..2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // Dynamic Placed Simulated Dots
                            circles.forEach { circle ->
                                if (circle.isEnabled) {
                                    val colAccent = try {
                                        Color(android.graphics.Color.parseColor(circle.colorHex))
                                    } catch (e: Exception) {
                                        primaryPurple
                                    }

                                    // Let's assume standard density bounds. Map database position relative percents.
                                    // Use stable offset scaling based on standard design layouts
                                    val safeX = ((circle.x % 1100).toFloat() / 1100f).coerceIn(0f, 0.9f)
                                    val safeY = ((circle.y % 2200).toFloat() / 2200f).coerceIn(0f, 0.9f)
                                    
                                    val dotX = parentWidth * safeX
                                    val dotY = parentHeight * safeY

                                    Box(
                                        modifier = Modifier
                                            .offset(x = dotX, y = dotY)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(colAccent.copy(alpha = 0.25f))
                                            .border(1.5.dp, colAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${circle.delayMs}ms",
                                            color = colAccent,
                                            fontSize = 6.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "فعل وضع المواقع بالأسفل لتتمكن من سحب الدوائر بإصبعك ومركبتها بالمليمتر على شاشتك في الخلفية",
                            color = mutedPurpleText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (selectedTab == 1) {
                    // Permissions Card (Light polished theme style with M3 accents)
                    item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "تراخيص التشغيل والتحكم بحرية",
                                color = darkCharcoal,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Draw over applications overlay permission
                            PermissionLightRow(
                                title = "العرض وتغطية الشاشة",
                                description = "مطلوب لوضع الدوائر الشفافة في أي مكان تريده على شاشتك.",
                                isGranted = isOverlayGranted,
                                primaryPurple = primaryPurple,
                                onGrantClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                }
                            )

                            HorizontalDivider(
                                color = borderColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Accessibility service interaction permission
                            PermissionLightRow(
                                title = "خدمة تسهيل الوصول للرصد والتوجيه",
                                description = "مطلوب لتأخير ومعالجة نقراتك على مساحة الدوائر وتوجيهها بدقة.",
                                isGranted = isServiceActive,
                                primaryPurple = primaryPurple,
                                onGrantClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                    showSingleToast(
                                        context,
                                        "ابحث عن [Touch Delayer] وقم بتنشيطه",
                                        Toast.LENGTH_LONG
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // ALWAYS show Permissions Card at the top of Control tab with dynamic state styling
                val hasAllPermissions = isOverlayGranted && isServiceActive
                val cardBorderColor = if (hasAllPermissions) Color(0xFFC6E7D2) else Color(0xFFFDC8C8)
                val cardBgColor = if (hasAllPermissions) Color(0xFFF6FBF7) else Color(0xFFFFF5F5)
                val cardTitleText = if (hasAllPermissions) "✅ جميع تراخيص التشغيل نشطة" else "⚠️ تراخيص التشغيل مطلوبة لتشغيل وظهور الدوائر"
                val cardTitleColor = if (hasAllPermissions) Color(0xFF2F855A) else Color(0xFFC53030)
                val dividerColor = if (hasAllPermissions) Color(0xFFC6E7D2).copy(alpha = 0.5f) else Color(0xFFFDC8C8).copy(alpha = 0.5f)

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cardTitleText,
                                    color = cardTitleColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Draw over applications overlay permission
                            PermissionLightRow(
                                title = "العرض وتغطية الشاشة (Overlay Permission)",
                                description = "مطلوب لتغطية ومحاذاة الدوائر الشفافة في أي مكان تريده على شاشتك.",
                                isGranted = isOverlayGranted,
                                primaryPurple = primaryPurple,
                                onGrantClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                }
                            )

                            HorizontalDivider(
                                color = dividerColor,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Accessibility service interaction permission
                            PermissionLightRow(
                                title = "خدمة تسهيل الوصول (Accessibility Service)",
                                description = "يجب تنشيط الخدمة ليتمكن التطبيق من تأخير النقرات وتوجيهها بدقة وإظهار الدوائر بالخلفية.",
                                isGranted = isServiceActive,
                                primaryPurple = primaryPurple,
                                onGrantClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                    showSingleToast(
                                        context,
                                        "ابحث عن [Touch Delayer] وقم بتنشيطه لتظهر الدوائر فوراً!",
                                        Toast.LENGTH_LONG
                                    )
                                }
                            )
                        }
                    }
                }

                // Global Enable/Disable Interception Switch Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (isDelayEnabled) Color(0xFFF1EEFA) else Color(0xFFF1F3F5)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isDelayEnabled) primaryPurple.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isDelayEnabled) "⏳" else "⚡",
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isDelayEnabled) "تأخير اللمس: مفعّل بالكامل ⏳" else "تأخير اللمس: معطل (النقرات مباشرة) ⚡",
                                            color = darkCharcoal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isDelayEnabled) "النقرات على الدوائر تتأخر بالخلفية." else "النقرات تمر مباشرة فوراً دون تأخير.",
                                            color = mutedPurpleText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = isDelayEnabled,
                                    onCheckedChange = { viewModel.toggleDelayEnabled() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryPurple,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.LightGray
                                    )
                                )
                            }
                        }
                    }
                }

                // Positioning Switch Controller (Sleek light amber/lavender mode design)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (isPositioningMode) Color(0xFFFFF9E6) else Color(0xFFF2F9F3)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isPositioningMode) Color(0xFFE5A100).copy(alpha = 0.15f) else Color(0xFF38A169).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPositioningMode) Icons.Default.Warning else Icons.Default.Lock,
                                        contentDescription = "Fix Status Icon",
                                        tint = if (isPositioningMode) Color(0xFFD69E2E) else Color(0xFF38A169)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPositioningMode) "حالة التعديل: الدوائر حرة الحركة 🔓" else "حالة التثبيت: الدوائر مثبتة ومحمية 🔒",
                                        color = darkCharcoal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isPositioningMode) "الدوائر غير ثابتة، يمكنك سحبها وتعديل مواقعها على شاشتك وتثبيتها الآن." else "الدوائر مثبتة ومحلتة في مكانها تماماً بالمليمتر ولا تؤثر على بقية أجهزة الشاشة.",
                                        color = mutedPurpleText,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // The dedicated robust FIX BUTTON
                            Button(
                                onClick = {
                                    viewModel.togglePositioningMode()
                                    if (!isPositioningMode) {
                                        showSingleToast(context, "تم تثبيت وحفظ الدوائر بالخلفية وتفعيل تأخير اللمس 🔒")
                                    } else {
                                        showSingleToast(context, "تم إلغاء التثبيت! اسحب الدوائر لوضعها وتعديلها 🔓")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPositioningMode) Color(0xFF38A169) else Color(0xFF6750A4)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositioningMode) Icons.Default.CheckCircle else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPositioningMode) "تثبيت الدوائر وحفظ المواقع (Fix Button) 🔒" else "إلغاء التثبيت وتعديل المواقع 🔓",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                // Undo Button Config and Action Card
                    item {
                    val additionHistory by viewModel.additionHistory.collectAsStateWithLifecycle()
                    val undoDelaySeconds by viewModel.undoDelaySeconds.collectAsStateWithLifecycle()
                    val isUndoCountingDown by viewModel.isUndoCountingDown.collectAsStateWithLifecycle()
                    val undoCountdownLeftMs by viewModel.undoCountdownLeftMs.collectAsStateWithLifecycle()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(primaryPurple.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Undo Icon",
                                        tint = primaryPurple
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "زر التراجع (Undo Button)",
                                        color = darkCharcoal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "تراجع عن إضافة آخر مجموعة من الدوائر تمت إضافتها لشاشتك بنقرة واحدة.",
                                        color = mutedPurpleText,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Configurable self-delay option (From 0.25 second up to 1 minute, i.e., 60 seconds)
                            val formattedDelay = String.format("%.2f", undoDelaySeconds)
                            Text(
                                text = "مدة تأخير تنفيذ التراجع: (${formattedDelay} ثانية)",
                                color = darkCharcoal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("0.25ث", fontSize = 11.sp, color = mutedPurpleText)
                                Slider(
                                    value = undoDelaySeconds,
                                    onValueChange = { viewModel.setUndoDelaySeconds(it) },
                                    valueRange = 0.25f..60f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = primaryPurple,
                                        activeTrackColor = primaryPurple
                                    )
                                )
                                Text("60ث", fontSize = 11.sp, color = mutedPurpleText)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Trigger Buttons
                            if (isUndoCountingDown) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFFD97706),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        val timeLeftSec = String.format("%.2f", undoCountdownLeftMs / 1000f)
                                        Text(
                                            text = "جاري التراجع خلال: ${timeLeftSec} ثانية...",
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.cancelUndo() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("إلغاء", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (additionHistory.isEmpty()) {
                                            showSingleToast(context, "لا توجد حركات إضافة يمكن التراجع عنها مؤخرًا!")
                                        } else {
                                            viewModel.triggerUndo()
                                        }
                                    },
                                    enabled = additionHistory.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (additionHistory.isNotEmpty()) primaryPurple else Color(0xFFE2E8F0),
                                        contentColor = if (additionHistory.isNotEmpty()) Color.White else Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(100.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Undo")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (additionHistory.isNotEmpty()) "اضغط للتراجع عن الإضافة الأخيرة" else "لا توجد إضافات سابقة للتراجع",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Configurations Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "لوحة التحكم بالدوائر (${circles.size})",
                            color = darkCharcoal,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        if (circles.isNotEmpty()) {
                            Text(
                                text = "مسح الكل",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.clearAllCircles()
                                        showSingleToast(context, "تم مسح جميع الدوائر التخصيصية")
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // Empty state or Dynamic Item cards
                if (circles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(primaryPurple.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Empty Settings",
                                        tint = primaryPurple,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لا توجد دوااير مصممة بعد",
                                    color = darkCharcoal,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط على الزر الزهري بالأسفل لإنشاء دائرتك الأولى وإدارتها وتعيين مدة تأخير متباينة لها بكل سهولة.",
                                    color = mutedPurpleText,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(circles, key = { it.id }) { circle ->
                        PolishedCircleCard(
                            circle = circle,
                            onUpdate = { updated -> viewModel.updateCircle(updated) },
                            onDelete = { viewModel.deleteCircle(circle) },
                            primaryPurple = primaryPurple,
                            darkCharcoal = darkCharcoal,
                            mutedPurpleText = mutedPurpleText,
                            borderColor = borderColor
                        )
                    }
                }
            }
        }

        if (selectedTab == 2) {
            // TAB 2: REPORTS & ANALYSIS (التقارير)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .background(primaryPurple.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Analysis",
                                        tint = primaryPurple
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "إحصائيات توجيه وتأخير اللمس الذاتية",
                                        color = darkCharcoal,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "مراقبة حية للأداء ومعدلات الاستجابة بالدوائر النشطة.",
                                        color = mutedPurpleText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simple dynamic metric visualizer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF3EDF7))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "إجمالي الدوائر", fontSize = 11.sp, color = mutedPurpleText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${circles.size}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = primaryPurple)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF3EDF7))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "الدوائر النشطة", fontSize = 11.sp, color = mutedPurpleText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${circles.filter { it.isEnabled }.size}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38A169))
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF3EDF7))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "متوسط التأخير", fontSize = 11.sp, color = mutedPurpleText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val avgDelay = if (circles.isNotEmpty()) circles.map { it.delayMs }.average() else 0.0
                                    Text(text = String.format("%.0fms", avgDelay), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = darkCharcoal)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "توزيع التغطية ومؤقتات التأخير النشطة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = darkCharcoal
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Visual progress bar representations
                            circles.forEach { circle ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${circle.name} (المعرف: ${circle.id})", fontSize = 11.sp, color = darkCharcoal)
                                        Text(text = "${circle.delayMs}ms", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryPurple)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { (circle.delayMs / 2000f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)),
                                        color = primaryPurple,
                                        trackColor = borderColor.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            // Bottom Navigation & Actions Layout Block
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (selectedTab == 0) {
                        // Big CTA Action Button: Add New Delay Zone
                        Button(
                            onClick = {
                                val widthPx = context.resources.displayMetrics.widthPixels
                                viewModel.addNewCircle(widthPx)
                                showSingleToast(context, "تم إضافة دائرة جديدة لتخصيصها!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "أضف", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إضافة دائرة تأخير جديدة",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Simulated 3-tab modern M3 bottom options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavBarTab(iconLabel = "🏠", title = "التحكم", isActive = selectedTab == 0) {
                            selectedTab = 0
                        }
                        BottomNavBarTab(iconLabel = "⚙️", title = "الإعدادات", isActive = selectedTab == 1) {
                            selectedTab = 1
                        }
                        BottomNavBarTab(iconLabel = "📊", title = "التقارير", isActive = selectedTab == 2) {
                            selectedTab = 2
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBarTab(iconLabel: String, title: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(if (isActive) 1.0f else 0.45f)
    ) {
        Text(
            text = iconLabel,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20)
        )
    }
}

@Composable
fun PermissionLightRow(
    title: String,
    description: String,
    isGranted: Boolean,
    primaryPurple: Color,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color(0xFF1D1B20),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFF49454F),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else primaryPurple,
                contentColor = if (isGranted) Color(0xFF10B981) else Color.White
            ),
            shape = RoundedCornerShape(100.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (isGranted) "مفعّل ✓" else "تفعيل الآن",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PolishedCircleCard(
    circle: CircleOverlay,
    onUpdate: (CircleOverlay) -> Unit,
    onDelete: () -> Unit,
    primaryPurple: Color,
    darkCharcoal: Color,
    mutedPurpleText: Color,
    borderColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val colorAccent = try {
        Color(android.graphics.Color.parseColor(circle.colorHex))
    } catch (e: Exception) {
        primaryPurple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header information block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Outer circle design accent + Name editor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorAccent, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        var textName by remember { mutableStateOf(circle.name) }
                        BasicTextField(
                            value = textName,
                            onValueChange = {
                                textName = it
                                onUpdate(circle.copy(name = it))
                            },
                            textStyle = TextStyle(
                                color = darkCharcoal,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            modifier = Modifier.width(140.dp)
                        )
                        Text(
                            text = "الموقع: (${circle.x} X, ${circle.y} Y)",
                            color = mutedPurpleText,
                            fontSize = 11.sp
                        )
                    }
                }

                // Switch + drop-settings control
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = circle.isEnabled,
                        onCheckedChange = { onUpdate(circle.copy(isEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorAccent,
                            checkedTrackColor = colorAccent.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الدائرة",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.Menu else Icons.Default.Settings,
                            contentDescription = "تخصيص الإعدادات",
                            tint = Color(0xFF79747E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Short readable details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val delayWord = when (circle.delayMs) {
                    250L -> "ربع ثانية (0.25s)"
                    500L -> "نصف ثانية (0.50s)"
                    750L -> "ثلاثة أرباع ثانية (0.75s)"
                    1000L -> "ثانية كاملة (1s)"
                    else -> "${circle.delayMs / 1000.0} ثانية"
                }
                Text(
                    text = "التأخير: $delayWord",
                    color = primaryPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الحجم: ${circle.radius}dp | شفافية: ${(circle.alpha * 100).roundToInt()}%",
                    color = mutedPurpleText,
                    fontSize = 12.sp
                )
            }

            // Expose detailed customized parameters
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

                    // 1. Precise delay slider calibration
                    Text(
                        text = "مدة التأخير بعد اللمس:",
                        color = darkCharcoal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = circle.delayMs.toFloat(),
                            onValueChange = { onUpdate(circle.copy(delayMs = it.roundToInt().toLong())) },
                            valueRange = 10f..2000f,
                            colors = SliderDefaults.colors(
                                thumbColor = colorAccent,
                                activeTrackColor = colorAccent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${circle.delayMs}ms",
                            color = darkCharcoal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(55.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    // Quick Select Presets row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            15L to "15 ملي ثانية",
                            50L to "50 ملي ثانية",
                            250L to "250 ملي ثانية",
                            1000L to "1.00ثانية"
                        )
                        presets.forEach { (ms, label) ->
                            val isSelected = circle.delayMs == ms
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colorAccent.copy(alpha = 0.15f) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isSelected) colorAccent else Color(0xFFCAC4D0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onUpdate(circle.copy(delayMs = ms)) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) colorAccent else darkCharcoal,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Size diameter slider calibration
                    Text(
                        text = "قطر الدائرة وحجم التغطية للّمِس:",
                        color = darkCharcoal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = circle.radius.toFloat(),
                            onValueChange = { onUpdate(circle.copy(radius = it.roundToInt())) },
                            valueRange = 30f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = colorAccent,
                                activeTrackColor = colorAccent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${circle.radius}dp",
                            color = darkCharcoal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(45.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Circle Alpha opacity transparency calibration
                    Text(
                        text = "مستوى شفافية الدائرة الملأت ومحيطها:",
                        color = darkCharcoal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = circle.alpha,
                            onValueChange = { onUpdate(circle.copy(alpha = it)) },
                            valueRange = 0.05f..0.90f,
                            colors = SliderDefaults.colors(
                                thumbColor = colorAccent,
                                activeTrackColor = colorAccent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(circle.alpha * 100).roundToInt()}%",
                            color = darkCharcoal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(45.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Custom Inside Display Label text
                    Text(
                        text = "نص مخصص يظهر داخل الدائرة الملونة:",
                        color = darkCharcoal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = circle.labelText,
                        onValueChange = { onUpdate(circle.copy(labelText = it)) },
                        placeholder = { Text("مثال: 0.25ثانية أو زر الرجوع", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorAccent,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = darkCharcoal,
                            unfocusedTextColor = darkCharcoal
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Select unique aesthetic color dot
                    Text(
                        text = "اختر اللون التعبيري لهذه الدائرة:",
                        color = darkCharcoal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val colorHexes = listOf(
                        "#6750A4", // M3 Royal Purple
                        "#38A169", // Vibrant Emerald
                        "#DD6B20", // Warm Amber
                        "#E53E3E", // Elegant Ruby Red
                        "#00838F", // Dark Cyan/Teal
                        "#D53F8C", // Clean Rose
                        "#3182CE"  // Soft Ocean Blue
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorHexes.forEach { hex ->
                            val dotColor = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = circle.colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdate(circle.copy(colorHex = hex)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
