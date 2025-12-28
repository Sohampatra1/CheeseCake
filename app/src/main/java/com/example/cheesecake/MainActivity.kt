package com.example.cheesecake

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cheesecake.ui.theme.CheeseCakeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheeseCakeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") { MainRootScreen(navController = navController) }
                        composable(
                            "camera?beep={beep}",
                            arguments = listOf(androidx.navigation.navArgument("beep") { defaultValue = false })
                        ) { backStackEntry ->
                             val beep = backStackEntry.arguments?.getBoolean("beep") ?: false
                             CameraScreen(navController = navController, shouldBeep = beep) 
                        }
                        composable("calendar") { CalendarScreen(navController = navController) }
                    }
                }
            }
        }
    }
}

@Composable
fun MainRootScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("💧") }, // Text is fine, but cleaner syntax
                    label = { Text("Hydration") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("🌸") },
                    label = { Text("Cycles") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> WaterTrackerScreen(navController = navController)
                1 -> PeriodTrackerScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navController: NavController, 
    viewModel: CameraViewModel = hiltViewModel(),
    shouldBeep: Boolean = false
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (cameraPermissionState.status.isGranted) {
        CameraPreview(
            onVerificationSuccess = {
                viewModel.saveWaterIntakeRecord()
                navController.popBackStack()
            },
            shouldBeep = shouldBeep
        )
    } else {
        Text("Camera permission is required to use this feature.")
    }
}

@Composable
fun CameraPreview(
    onVerificationSuccess: () -> Unit,
    shouldBeep: Boolean = false // If true, play beep until verification (Alarm Mode)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var verificationSucceeded by remember { mutableStateOf(false) }
    var isPoseValid by remember { mutableStateOf(false) }
    var debugText by remember { mutableStateOf("Initializing...") }
    var detectedObjectsState by remember { mutableStateOf<List<android.graphics.Rect>>(emptyList()) }
    var mouthPositionState by remember { mutableStateOf<android.graphics.PointF?>(null) }
    
    // We use a timestamp to track when validity started; 0L means invalid/not started
    var poseValidStartTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var lastValidFrameTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) } // For grace period
    
    // Gemini Verification State
    var isVerifyingWithGemini by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val toneGenerator = remember { android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100) }

    // Alarm Logic
    LaunchedEffect(shouldBeep, verificationSucceeded) {
        if (shouldBeep && !verificationSucceeded) {
            while (!verificationSucceeded) {
                try {
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Tone generator failed", e)
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Constants
    val REQUIRED_DURATION_MS = 2000L // Reduced from 3s to 2s
    val GRACE_PERIOD_MS = 500L // Allow 500ms of invalid frames before resetting
    val VALID_LABELS = listOf("Bottle", "Water bottle", "Cup", "Mug", "Drink", "Beverage", "Food", "Tableware", "Container", "Plastic", "Cylinder")
    // Head Tilt Threshold (Pitch) - Looking UP usually results in positive X Euler Angle
    val REQUIRED_TILT_ANGLE = 10.0f // Reduced from 12 to 10 for easier detection

    // Blinking effect for the green dot
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "BlinkingDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(500),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    // Face Detector
    val highAccuracyOpts = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Needed for Euler angles sometimes, though defaults might work
            .build()
    }
    val faceDetector = remember { FaceDetection.getClient(highAccuracyOpts) }

    // Object Detector
    val objectDetectorOptions = remember {
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    }
    val objectDetector = remember { ObjectDetection.getClient(objectDetectorOptions) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (verificationSucceeded) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val image = imageProxy.image

                    if (image != null) {
                        val processImage = InputImage.fromMediaImage(image, rotationDegrees)

                        faceDetector.process(processImage)
                            .addOnSuccessListener { faces ->
                                objectDetector.process(processImage)
                                    .addOnSuccessListener { detectedObjects ->
                                        var currentFrameValid = false
                                        var debugLog = ""
                                        val objectRects = mutableListOf<android.graphics.Rect>()
                                        var mouthPos: android.graphics.PointF? = null
                                        var headTiltX = 0.0f

                                        // Log Objects
                                        if (detectedObjects.isNotEmpty()) {
                                            debugLog += "Objs: ${detectedObjects.joinToString { obj -> 
                                                val label = obj.labels.firstOrNull()?.text ?: "Unknown"
                                                "$label" 
                                            }}\n"
                                        } else {
                                            debugLog += "Objs: None\n"
                                        }

                                        if (faces.isNotEmpty()) {
                                            val face = faces.first()
                                            val mouthBottom = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM)?.position
                                            mouthPos = mouthBottom
                                            headTiltX = face.headEulerAngleX
                                            
                                            if (mouthBottom != null) {
                                                debugLog += "Mouth Found\n"
                                                debugLog += "Tilt: ${headTiltX.toInt()}° (Req: >${REQUIRED_TILT_ANGLE.toInt()}°)\n"
                                                
                                                val isTiltedBack = headTiltX > REQUIRED_TILT_ANGLE
                                                
                                                if (isTiltedBack) {
                                                    for (detectedObject in detectedObjects) {
                                                        objectRects.add(detectedObject.boundingBox)
                                                        
                                                        val label = detectedObject.labels.firstOrNull()?.text ?: "Unknown"
                                                        // Flexible Label Check
                                                        val isValidLabel = VALID_LABELS.any { label.contains(it, ignoreCase = true) } || label == "Unknown"
                                                        
                                                        val box = detectedObject.boundingBox
                                                        val objectCenter = android.graphics.PointF(box.centerX().toFloat(), box.centerY().toFloat())
                                                        
                                                        // Distance Check
                                                        val dx = mouthBottom.x - objectCenter.x
                                                        val dy = mouthBottom.y - objectCenter.y
                                                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                                        
                                                        val DISTANCE_THRESHOLD = 350.0 // Increased from 250 for easier detection

                                                        if (distance < DISTANCE_THRESHOLD) {
                                                            debugLog += "CLOSE: $label\n"
                                                            currentFrameValid = true // All Conditions Met!
                                                        } else {
                                                             debugLog += "FAR: $label\n"
                                                        }
                                                    }
                                                    if (detectedObjects.isEmpty()) {
                                                        debugLog += "No Object Near Mouth\n"
                                                    }
                                                } else {
                                                    debugLog += "Look UP to drink\n"
                                                }
                                            } else {
                                                debugLog += "Mouth: Not visible\n"
                                            }
                                        } else {
                                            debugLog += "Face: None\n"
                                        }

                                        // Update state
                                        val now = System.currentTimeMillis()
                                        
                                        if (currentFrameValid) {
                                            lastValidFrameTime = now // Track last valid frame
                                            if (poseValidStartTime == 0L) {
                                                poseValidStartTime = now
                                            }
                                            val duration = now - poseValidStartTime
                                            
                                            if (duration >= REQUIRED_DURATION_MS && !verificationSucceeded && !isVerifyingWithGemini) {
                                                // Local check passed! Now verify with Gemini AI
                                                isVerifyingWithGemini = true
                                                debugLog += "Verifying with AI..."
                                                
                                                coroutineScope.launch {
                                                    try {
                                                        // Capture bitmap from PreviewView
                                                        val bitmap = previewView.bitmap
                                                        if (bitmap != null) {
                                                            val isConfirmed = GeminiVerifier.verifyDrinking(bitmap)
                                                            if (isConfirmed) {
                                                                verificationSucceeded = true
                                                                try {
                                                                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP)
                                                                } catch(e: Exception) { }
                                                                onVerificationSuccess()
                                                            } else {
                                                                // Gemini said NO - reset and try again
                                                                poseValidStartTime = 0L
                                                                debugText = "AI: Not drinking - Try again"
                                                            }
                                                        } else {
                                                            debugText = "Failed to capture image"
                                                            poseValidStartTime = 0L
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("CameraPreview", "Gemini verification failed", e)
                                                        // Fallback: Accept on error to not block user
                                                        verificationSucceeded = true
                                                        onVerificationSuccess()
                                                    } finally {
                                                        isVerifyingWithGemini = false
                                                    }
                                                }
                                            } else if (!isVerifyingWithGemini) {
                                                debugLog += "DRINKING: ${(duration / 1000.0)}s"
                                            }
                                        } else {
                                            // GRACE PERIOD: Only reset if we've been invalid for too long
                                            val timeSinceLastValid = now - lastValidFrameTime
                                            if (lastValidFrameTime == 0L || timeSinceLastValid > GRACE_PERIOD_MS) {
                                                poseValidStartTime = 0L
                                            }
                                            if (!isVerifyingWithGemini) {
                                                debugLog += "STATUS: Watching..."
                                            }
                                        }
                                        isPoseValid = currentFrameValid
                                        if (!isVerifyingWithGemini) {
                                            debugText = debugLog
                                        }
                                        detectedObjectsState = objectRects
                                        mouthPositionState = mouthPos
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("CameraScreen", "Object detection failed", e)
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraScreen", "Face detection failed", e)
                                imageProxy.close()
                            }
                    } else {
                         imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(context))
        }

        // Overlay: Visual Feedback & Debug
        Box(
            modifier = Modifier.fillMaxSize()
                .drawBehind {
                     val scaleX = size.width / 480f 
                     val scaleY = size.height / 640f 
                     
                     mouthPositionState?.let { mouth ->
                         drawCircle(
                             color = androidx.compose.ui.graphics.Color.Blue,
                             radius = 10f,
                             center = androidx.compose.ui.geometry.Offset(mouth.x * scaleX, mouth.y * scaleY)
                         )
                     }
                }
        ) {
            // Status Dot
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(32.dp)
                    .align(Alignment.TopStart)
                    .drawBehind {
                         val color = if (isPoseValid) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Red
                         val alphaValue = if (isPoseValid) alpha else 1f
                         drawCircle(color = color, alpha = alphaValue)
                    }
            )
            
            // Debug Text Overlay
            Text(
                text = debugText,
                color = androidx.compose.ui.graphics.Color.Green,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            )
        }
    }
}


@ComposePreview(showBackground = true)
@Composable
fun DefaultPreview() {
    CheeseCakeTheme {
        MainRootScreen(navController = rememberNavController())
    }
}
