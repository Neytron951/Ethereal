package com.neytron.ethereal.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neytron.ethereal.R
import com.neytron.ethereal.data.AppInfo
import com.neytron.ethereal.ui.theme.AllPalettes
import com.neytron.ethereal.ui.theme.EtherealFont
import com.neytron.ethereal.ui.theme.EtherealPalette
import com.neytron.ethereal.ui.theme.fontFamilyFor
import com.neytron.ethereal.utils.toImageBitmap
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class OnboardingPhase {
    WIZARD,
    EXPAND_AND_HOLD
}

@Composable
fun OnboardingOverlay(
    visible: Boolean,
    apps: List<AppInfo>,
    palette: EtherealPalette,
    fontFamilyName: String,
    dockPackages: Set<String>,
    onPaletteChange: (String) -> Unit,
    onFontChange: (String) -> Unit,
    onDockPackagesChange: (Set<String>) -> Unit,
    onComplete: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    var currentPhase by remember { mutableStateOf(OnboardingPhase.WIZARD) }
    var isFinishing by remember { mutableStateOf(false) }

    OnboardingAudioPlayer(context = context, isFinishing = isFinishing)

    val handleComplete = {
        if (!isFinishing) {
            isFinishing = true
            onComplete()
        }
    }

    AnimatedVisibility(
        visible = visible && !isFinishing,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EtherealBackgroundAnimation(palette = palette)

            when (currentPhase) {
                OnboardingPhase.WIZARD -> {
                    OnboardingWizard(
                        apps = apps,
                        palette = palette,
                        fontFamilyName = fontFamilyName,
                        dockPackages = dockPackages,
                        onPaletteChange = onPaletteChange,
                        onFontChange = onFontChange,
                        onDockPackagesChange = onDockPackagesChange,
                        onWizardFinish = {
                            currentPhase = OnboardingPhase.EXPAND_AND_HOLD
                        }
                    )
                }
                OnboardingPhase.EXPAND_AND_HOLD -> {
                    SeamlessExpandAndHoldScene(
                        palette = palette,
                        onComplete = handleComplete
                    )
                }
            }
        }
    }
}

// === АНИМИРОВАННЫЙ ЖИВОЙ ФОН С АУРОЙ ===
@Composable
private fun EtherealBackgroundAnimation(palette: EtherealPalette) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t1"
    )

    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF0A0A0C))

        val x1 = w * 0.35f + sin(t1) * w * 0.22f
        val y1 = h * 0.32f + cos(t1 * 0.8f) * h * 0.18f
        val r1 = w.coerceAtLeast(h) * 0.48f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = 0.22f),
                    palette.primary.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(x1, y1),
                radius = r1
            ),
            center = Offset(x1, y1),
            radius = r1
        )

        val x2 = w * 0.65f + cos(t2) * w * 0.22f
        val y2 = h * 0.68f + sin(t2 * 0.9f) * h * 0.18f
        val r2 = w.coerceAtLeast(h) * 0.52f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.secondary.copy(alpha = 0.18f),
                    palette.secondary.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(x2, y2),
                radius = r2
            ),
            center = Offset(x2, y2),
            radius = r2
        )
    }
}

// === ПЛЕЕР ЗВУКОВОГО СОПРОВОЖДЕНИЯ ===
@Composable
private fun OnboardingAudioPlayer(
    context: Context,
    isFinishing: Boolean
) {
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        try {
            var resId = context.resources.getIdentifier("onboarding_ambient", "raw", context.packageName)
            if (resId == 0) {
                resId = context.resources.getIdentifier("onboarding_bg", "raw", context.packageName)
            }
            if (resId == 0) {
                resId = context.resources.getIdentifier("onboarding", "raw", context.packageName)
            }

            if (resId != 0) {
                val player = MediaPlayer.create(context, resId)?.apply {
                    isLooping = true
                    setVolume(0.7f, 0.7f)
                    start()
                }
                mediaPlayerRef.value = player
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                val player = mediaPlayerRef.value
                mediaPlayerRef.value = null
                if (player != null) {
                    if (player.isPlaying) player.stop()
                    player.release()
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isFinishing) {
        if (isFinishing) {
            val player = mediaPlayerRef.value
            if (player != null && player.isPlaying) {
                val steps = 30
                val fadeDurationMs = 1200L
                val delayMs = fadeDurationMs / steps

                for (i in steps downTo 0) {
                    val vol = i.toFloat() / steps * 0.7f
                    try {
                        player.setVolume(vol, vol)
                    } catch (_: Exception) {}
                    delay(delayMs)
                }

                try {
                    player.stop()
                    player.release()
                } catch (_: Exception) {}
                mediaPlayerRef.value = null
            }
        }
    }
}

// === МОДЕЛЬ ЗАДАЧИ ОНБОРДИНГА ===
private enum class OnboardingTaskStatus {
    DONE,
    NOT_DONE,
    UNAVAILABLE
}

private data class OnboardingTask(
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val status: OnboardingTaskStatus,
    val onAction: () -> Unit
)

// === ПРОВЕРКИ СТАТУСА ===

private fun isDefaultLauncher(context: Context): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == context.packageName
    } catch (_: Exception) {
        false
    }
}

private fun isRoleAvailable(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        rm?.isRoleAvailable(RoleManager.ROLE_HOME) == true
    } catch (_: Exception) {
        false
    }
}

// === МАСТЕР НАСТРОЙКИ ===
@Composable
private fun OnboardingWizard(
    apps: List<AppInfo>,
    palette: EtherealPalette,
    fontFamilyName: String,
    dockPackages: Set<String>,
    onPaletteChange: (String) -> Unit,
    onFontChange: (String) -> Unit,
    onDockPackagesChange: (Set<String>) -> Unit,
    onWizardFinish: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val totalSteps = 4
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    var isLauncherDefault by remember { mutableStateOf(isDefaultLauncher(context)) }
    var roleAvailable by remember { mutableStateOf(isRoleAvailable(context)) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isLauncherDefault = isDefaultLauncher(context)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLauncherDefault = isDefaultLauncher(context)
                roleAvailable = isRoleAvailable(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Строки для задач — локализованы через stringResource
    val taskLauncherTitle = stringResource(R.string.onboarding_task_launcher_title)
    val taskLauncherSubtitle = stringResource(R.string.onboarding_task_launcher_subtitle)
    val taskLauncherButton = stringResource(R.string.onboarding_task_launcher_button)

    val taskDoneTitle = stringResource(R.string.onboarding_task_done_title)
    val taskDoneSubtitle = stringResource(R.string.onboarding_task_done_subtitle)

    val tasks = remember(
        isLauncherDefault,
        roleAvailable,
        taskLauncherTitle,
        taskLauncherSubtitle,
        taskLauncherButton,
        taskDoneTitle,
        taskDoneSubtitle
    ) {
        buildList {
            if (!isLauncherDefault) {
                add(
                    OnboardingTask(
                        title = taskLauncherTitle,
                        subtitle = taskLauncherSubtitle,
                        buttonText = taskLauncherButton,
                        status = OnboardingTaskStatus.NOT_DONE,
                        onAction = {
                            if (roleAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                try {
                                    val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                                    val intent = rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
                                    roleLauncher.launch(intent)
                                } catch (_: Exception) {
                                    openDefaultAppsSettings(context)
                                }
                            } else {
                                openDefaultAppsSettings(context)
                            }
                        }
                    )
                )
            }

            if (isEmpty()) {
                add(
                    OnboardingTask(
                        title = taskDoneTitle,
                        subtitle = taskDoneSubtitle,
                        buttonText = "",
                        status = OnboardingTaskStatus.DONE,
                        onAction = {}
                    )
                )
            }
        }
    }

    val allTasksDone = isLauncherDefault

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(28.dp),
        color = palette.background.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, palette.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_of, currentStep, totalSteps),
                    color = palette.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (step in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .width(if (step == currentStep) 24.dp else 10.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step <= currentStep) palette.primary
                                    else palette.surfaceVariant
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { w -> w } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally { w -> -w / 2 } + fadeOut(tween(250))
                        )
                    } else {
                        (slideInHorizontally { w -> -w } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally { w -> w / 2 } + fadeOut(tween(250))
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> StepTheme(
                        palette = palette,
                        onPaletteChange = onPaletteChange
                    )
                    2 -> StepFont(
                        fontFamilyName = fontFamilyName,
                        palette = palette,
                        onFontChange = onFontChange
                    )
                    3 -> StepSystemSetup(
                        palette = palette,
                        tasks = tasks
                    )
                    4 -> StepDock(
                        apps = apps,
                        palette = palette,
                        selectedDockApps = dockPackages,
                        onDockPackagesChange = onDockPackagesChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val nextEnabled = true

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentStep--
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .alpha(if (nextEnabled) 1f else 0.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (nextEnabled) palette.primary
                            else palette.surfaceVariant
                        )
                        .clickable(enabled = nextEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                onWizardFinish()
                            }
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentStep < totalSteps)
                            stringResource(R.string.onboarding_next)
                        else
                            stringResource(R.string.onboarding_finish),
                        color = if (nextEnabled) palette.background
                        else palette.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// === ШАГ 3: СИСТЕМНЫЕ НАСТРОЙКИ ===
@Composable
private fun StepSystemSetup(
    palette: EtherealPalette,
    tasks: List<OnboardingTask>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_system_title),
            color = palette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_system_subtitle),
            color = palette.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks) { task ->
                TaskCard(
                    palette = palette,
                    task = task
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    palette: EtherealPalette,
    task: OnboardingTask
) {
    val haptic = LocalHapticFeedback.current
    val isDone = task.status == OnboardingTaskStatus.DONE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDone) palette.primary.copy(alpha = 0.12f)
                else palette.surfaceVariant.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = if (isDone) palette.primary.copy(alpha = 0.5f) else palette.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isDone) "✓" else "!",
                    color = palette.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.title,
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.subtitle,
                color = palette.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (task.buttonText.isNotEmpty() && !isDone) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.primary)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            task.onAction()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = task.buttonText,
                        color = palette.background,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isDone) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_task_done_label),
                    color = palette.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// === ОТКРЫТИЕ СИСТЕМНЫХ ЭКРАНОВ ===

private fun openDefaultAppsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// === ШАГ 1: ТЕМА ===
@Composable
private fun StepTheme(
    palette: EtherealPalette,
    onPaletteChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_theme_title),
            color = palette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_theme_subtitle),
            color = palette.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(AllPalettes) { p ->
                val isSelected = p.id == palette.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) palette.primary.copy(alpha = 0.2f)
                            else palette.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) palette.primary else palette.surfaceVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPaletteChange(p.id)
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(p.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = p.name,
                                color = if (isSelected) palette.primary else palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = stringResource(R.string.onboarding_theme_selected),
                                color = palette.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// === ШАГ 2: ШРИФТ ===
@Composable
private fun StepFont(
    fontFamilyName: String,
    palette: EtherealPalette,
    onFontChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_font_title),
            color = palette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_font_subtitle),
            color = palette.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(EtherealFont.entries) { fontEnum ->
                val isSelected = fontEnum.name == fontFamilyName
                val ownFontFamily = remember(fontEnum) { fontFamilyFor(fontEnum) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) palette.primary.copy(alpha = 0.2f)
                            else palette.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) palette.primary else palette.surfaceVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFontChange(fontEnum.name)
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = fontEnum.displayName,
                                fontFamily = ownFontFamily,
                                color = if (isSelected) palette.primary else palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ethereal Launcher 123",
                                fontFamily = ownFontFamily,
                                color = palette.textSecondary,
                                fontSize = 13.sp
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = palette.primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// === ШАГ 4: DOCK ===
@Composable
private fun StepDock(
    apps: List<AppInfo>,
    palette: EtherealPalette,
    selectedDockApps: Set<String>,
    onDockPackagesChange: (Set<String>) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.onboarding_dock_title),
            color = palette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_dock_subtitle, selectedDockApps.size),
            color = palette.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                val isSelected = app.packageName in selectedDockApps
                val bmp = remember(app.packageName) { app.icon.toImageBitmap() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) palette.primary.copy(alpha = 0.2f)
                            else palette.surfaceVariant.copy(alpha = 0.2f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) palette.primary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val newSet = selectedDockApps.toMutableSet()
                            if (isSelected) {
                                newSet.remove(app.packageName)
                            } else {
                                if (newSet.size < 5) {
                                    newSet.add(app.packageName)
                                }
                            }
                            onDockPackagesChange(newSet)
                        }
                        .padding(10.dp)
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = app.label,
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// === БЕСШОВНАЯ СЦЕНА РАСШИРЕНИЯ ===
@Composable
private fun SeamlessExpandAndHoldScene(
    palette: EtherealPalette,
    onComplete: () -> Unit
) {
    val expandProgress = remember { Animatable(0f) }
    val slideUpProgress = remember { Animatable(0f) }
    var showHint by remember { mutableStateOf(false) }
    var isHoldingGlobal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        expandProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
        )
        delay(200)

        slideUpProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(5000L)
        if (!isHoldingGlobal) {
            showHint = true
        }
    }

    val tExpand = expandProgress.value
    val tSlide = slideUpProgress.value

    val mainColor = palette.primary

    val titleOffsetY = (-75 * tSlide).dp
    val contentAlpha = tSlide

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = titleOffsetY)
        ) {
            Text(
                text = "E",
                color = mainColor,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .width((145 * tExpand).dp)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "therea",
                    color = mainColor.copy(alpha = (tExpand * 1.6f - 0.2f).coerceIn(0f, 1f)),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = "l",
                color = mainColor,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (tSlide > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = 70.dp + (30 * (1f - tSlide)).dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_final_subtitle),
                    color = palette.textSecondary,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(45.dp))

                HoldToStartButton(
                    palette = palette,
                    onComplete = onComplete,
                    onHoldingChanged = { holding ->
                        isHoldingGlobal = holding
                        if (holding) showHint = false
                    },
                    onTap = {
                        showHint = true
                    }
                )

                AnimatedVisibility(
                    visible = showHint,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.onboarding_press_and_hold),
                        color = palette.textSecondary.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldToStartButton(
    palette: EtherealPalette,
    onComplete: () -> Unit,
    onHoldingChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isHolding) {
        onHoldingChanged(isHolding)
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            val totalDurationMs = 1800f

            while (isHolding && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                val p = (elapsed / totalDurationMs).coerceIn(0f, 1f)
                holdProgress = p

                triggerEscalatingVibration(context, haptic, p)

                if (p >= 1f) {
                    holdProgress = 1f
                    triggerSuccessVibration(context, haptic)
                    onComplete()
                    break
                }
                delay(20)
            }
        } else {
            val startP = holdProgress
            if (startP > 0f) {
                val startTime = System.currentTimeMillis()
                while (!isHolding && holdProgress > 0f) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val p = (startP - (elapsed / 400f)).coerceAtLeast(0f)
                    holdProgress = p
                    if (p <= 0f) break
                    delay(16)
                }
            }
        }
    }

    val buttonWidth = 220.dp
    val buttonHeight = 56.dp

    Box(
        modifier = Modifier
            .width(buttonWidth)
            .height(buttonHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(palette.surfaceVariant.copy(alpha = 0.4f))
            .border(2.dp, palette.primary, RoundedCornerShape(28.dp))
            .drawWithContent {
                drawContent()
                if (holdProgress > 0f) {
                    val p = holdProgress.coerceIn(0f, 1f)
                    val fillColor = palette.primary.copy(alpha = 0.9f)
                    val halfWidth = size.width / 2f
                    val fillWidthFromEdge = halfWidth * p

                    drawRect(
                        color = fillColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(fillWidthFromEdge, size.height)
                    )

                    drawRect(
                        color = fillColor,
                        topLeft = Offset(size.width - fillWidthFromEdge, 0f),
                        size = Size(fillWidthFromEdge, size.height)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_final_button),
            color = if (holdProgress > 0.5f) palette.background else palette.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Suppress("DEPRECATION")
private fun triggerEscalatingVibration(
    context: Context,
    haptic: HapticFeedback,
    progress: Float
) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            val amplitude = (15 + (progress * 240)).toInt().coerceIn(1, 255)
            val duration = (12 + (progress * 15)).toLong()
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    } catch (_: Exception) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Suppress("DEPRECATION")
private fun triggerSuccessVibration(
    context: Context,
    haptic: HapticFeedback
) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(140, 255))
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    } catch (_: Exception) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}