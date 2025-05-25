package com.example.my_elephant_collar


import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import kotlinx.coroutines.delay
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.my_elephant_collar.ui.theme.My_elephant_collarTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState


// Constants for notification and location updates
const val CHANNEL_ID = "elephant_proximity_channel"
const val CHANNEL_NAME = "Elephant Proximity Alerts"
const val CHANNEL_DESCRIPTION = "Notifications when an elephant is nearby."
const val NOTIFICATION_ID = 101
const val WARNING_DISTANCE_METERS = 1000.0 // 1 kilometer threshold for warning

class MainActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Activity Result Launcher for requesting location permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        when {
            fineLocationGranted -> {
                Log.d("PERMISSIONS", "Fine location granted")
                // Fine location access granted.
            }
            coarseLocationGranted -> {
                Log.d("PERMISSIONS", "Coarse location granted")
                // Only approximate location access granted.
            }
            else -> {
                Log.d("PERMISSIONS", "No location permissions granted")
                // No location access granted.
            }
        }
    }
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean -> // Use 'granted' or any other valid name
        if (granted) {
            Log.d("PERMISSIONS", "Notifications permission granted.")
            // Proceed with sending notifications
        } else {
            Log.d("PERMISSIONS", "Notifications permission denied.")
            // Inform the user that notifications won't work
            // You might want to show a UI element explaining this
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Permission already granted
                Log.d("PERMISSIONS", "Notifications permission already granted.")
            }
        } else {
            // No need to request on older Android versions
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission() // Call the function here
        createNotificationChannel() // Create notification channel on app start


        database = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location permissions when the activity is created
        requestLocationPermissions()
        createNotificationChannel() // Create notification channel on app start

        setContent {
            My_elephant_collarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass both database reference and fusedLocationClient to the screen
                    LocationMapScreen(
                        databaseRef = database,
                        elephantId = "elephantId123",
                        fusedLocationClient = fusedLocationClient
                    )
                }
            }
        }
    }

    // Function to request location permissions
    private fun requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are not granted, request them
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permissions already granted
            Log.d("PERMISSIONS", "Location permissions already granted.")
        }
    }

    // Function to create a Notification Channel (required for Android 8.0+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // High importance for warnings
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NOTIFICATION", "Notification Channel Created")
        }
    }
}

@SuppressLint("DefaultLocale")
@Suppress("UNREACHABLE_CODE")
@Composable
fun LocationMapScreen(
    databaseRef: DatabaseReference,
    elephantId: String,
    fusedLocationClient: FusedLocationProviderClient
) {
    // State to hold the historical locations as LatLng objects
    var historicalLocations by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    // State to hold the latest known elephant location
    var elephantLatLng by remember { mutableStateOf<LatLng?>(null) }
    // State to hold the user's current location
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    // State to indicate if data is still loading
    var isLoading by remember { mutableStateOf(true) }
    // State to hold any error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }


    val context = LocalContext.current // Get the current context for notifications

    // Camera position state for the Google Map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 5f) // Default zoom
    }

    // Declare locationCallback here, outside LaunchedEffect to make it accessible in onDispose
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    userLatLng = LatLng(location.latitude, location.longitude)
                    Log.d("USER_LOCATION", "User location: Lat=${location.latitude}, Lon=${location.longitude}")
                    // Optionally, move camera to user location if desired
                    // cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng!!, 15f)
                }
            }
        }
    }

    // DisposableEffect to manage the Firebase listener lifecycle for elephant location
    DisposableEffect(databaseRef, elephantId) {
        val movementsRef = databaseRef.child("elephantMovements").child(elephantId)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempLocationsWithTimestamps = mutableListOf<Pair<Long, LatLng>>()
                var latestTimestamp = 0L
                var latestElephantLocation: LatLng? = null

                for (locationSnapshot in snapshot.children) {
                    val timestamp = locationSnapshot.key?.toLongOrNull()
                    val latitude = locationSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = locationSnapshot.child("longitude").getValue(Double::class.java)

                    if (timestamp != null && latitude != null && longitude != null) {
                        val latLng = LatLng(latitude, longitude)
                        tempLocationsWithTimestamps.add(Pair(timestamp, latLng))

                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp
                            latestElephantLocation = latLng
                        }
                    }
                }

                historicalLocations = tempLocationsWithTimestamps
                    .sortedBy { it.first }
                    .map { it.second }

                elephantLatLng = latestElephantLocation

                latestElephantLocation?.let {
                    // Only move camera if user location is not available or if it's the initial load
                    if (userLatLng == null || isLoading) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 10f)
                    }
                }

                isLoading = false
                errorMessage = null
                Log.d("FIREBASE_MAP", "Fetched ${historicalLocations.size} historical locations for $elephantId")
            }

            override fun onCancelled(error: DatabaseError) {
                errorMessage = "Failed to load movement data: ${error.message}"
                isLoading = false
                Log.e("FIREBASE_MAP", "Error getting movement data: ${error.message}", error.toException())
            }
        }

        movementsRef.addValueEventListener(valueEventListener)

        // This is the onDispose block for DisposableEffect
        onDispose {
            movementsRef.removeEventListener(valueEventListener)
        }
    }

    LaunchedEffect(fusedLocationClient) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("LOCATION", "Location permissions not granted for FusedLocationProviderClient.")
            return@LaunchedEffect
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

        // ✅ Correct cleanup using awaitDispose
        kotlinx.coroutines.awaitCancellation().also {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("USER_LOCATION", "Location updates removed.")
        }
    }


// Effect to continuously check proximity and send notification
    LaunchedEffect(userLatLng, elephantLatLng) {
        if (userLatLng != null && elephantLatLng != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLatLng!!.latitude, userLatLng!!.longitude,
                elephantLatLng!!.latitude, elephantLatLng!!.longitude,
                results
            )
            val distance = results[0] // Distance in meters

            Log.d("PROXIMITY", "Distance to elephant: $distance meters")

            if (distance <= WARNING_DISTANCE_METERS) {
                sendNotification(
                    context,
                    "Elephant Alert!",
                    "An elephant is ${String.format("%.0f", distance)} meters away!"
                )
                // Optionally, add a small delay to avoid flooding the user with notifications
                delay(5000) // Send notification every 5 seconds while close
            }
            // We no longer need to track warningShown to prevent continuous notifications.
            // The delay will handle the rate limiting.
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Loading elephant movement data...")
            } else if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    if (historicalLocations.isNotEmpty()) {
                        Polyline(points = historicalLocations, color = MaterialTheme.colorScheme.primary)
                    }

                    // Marker for the latest known elephant location
                    elephantLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Elephant Location",
                            snippet = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                        )
                    }

                    // Marker for the user's current location
                    userLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Your Location",
                            snippet = "Lat: ${it.latitude}, Lon: ${it.longitude}",
                            alpha = 0.7f // Make user marker slightly transparent
                        )
                    }
                }
            }
        }
    }
}

// Helper function to send a notification
fun sendNotification(context: Context, title: String, message: String) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_elephant_notification) // Changed to elephant icon
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true) // Dismiss notification when tapped

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider handling this case (e.g., informing user to enable notifications)
            Log.w("NOTIFICATION", "POST_NOTIFICATIONS permission not granted. Cannot send notification.")
            return
        }
        notify(NOTIFICATION_ID, builder.build())
        Log.d("NOTIFICATION", "Notification sent: $title - $message")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    My_elephant_collarTheme {
        Greeting("Android (Preview)")
    }
}