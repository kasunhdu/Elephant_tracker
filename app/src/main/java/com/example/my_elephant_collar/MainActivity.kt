package com.example.my_elephant_collar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Google Maps imports
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

import com.example.my_elephant_collar.ui.theme.My_elephant_collarTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        // Call super.onCreate(savedInstanceState) FIRST and only ONCE
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enable edge-to-edge display

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference

        // Set the content of the activity using Jetpack Compose
        setContent {
            My_elephant_collarTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Display the LocationMapScreen which fetches and shows location on a map
                    LocationMapScreen(databaseRef = database)
                }
            }
        }
    }
}

/**
 * Composable function to display a Google Map with location data fetched from Firebase.
 * It listens for real-time updates to latitude and longitude.
 */
@Composable
fun LocationMapScreen(databaseRef: DatabaseReference) {
    // State to hold the fetched latitude and longitude
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    // State to indicate if data is still loading
    var isLoading by remember { mutableStateOf(true) }
    // State to hold any error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Define a default camera position for the map
    val defaultLocation = LatLng(0.0, 0.0) // Default to (0,0) or a known central point
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 1f) // Start zoomed out
    }

    // DisposableEffect is used to manage the lifecycle of the Firebase listener.
    // It adds the listener when the composable enters the composition and removes it
    // when the composable leaves the composition to prevent memory leaks.
    DisposableEffect(databaseRef) {
        // Reference to the "location" node in Firebase
        // Assumes data structure like:
        // "location": {
        //   "latitude": 37.7749,
        //   "longitude": -122.4194
        // }
        val locationRef = databaseRef.child("location")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the snapshot exists and contains children
                if (snapshot.exists()) {
                    val lat = snapshot.child("latitude").value as? Double
                    val lon = snapshot.child("longitude").value as? Double

                    if (lat != null && lon != null) {
                        latitude = lat
                        longitude = lon
                        // Update camera position to the new fetched location
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 10f)
                        errorMessage = null // Clear any previous error
                        Log.d("FIREBASE_MAP", "Fetched location: Lat=$latitude, Lon=$longitude")
                    } else {
                        errorMessage = "Location data incomplete or invalid."
                        Log.e("FIREBASE_MAP", "Location data incomplete or invalid: $snapshot")
                    }
                } else {
                    errorMessage = "No location data found at 'location' node."
                    Log.d("FIREBASE_MAP", "No location data found at 'location' node.")
                }
                isLoading = false // Data fetching complete
            }

            override fun onCancelled(error: DatabaseError) {
                errorMessage = "Failed to load location: ${error.message}"
                isLoading = false // Data fetching complete
                Log.e("FIREBASE_MAP", "Error getting location data: ${error.message}", error.toException())
            }
        }

        // Add the listener to the Firebase reference
        locationRef.addValueEventListener(valueEventListener)

        // When the composable leaves the composition, remove the listener
        onDispose {
            locationRef.removeEventListener(valueEventListener)
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
                // Show a loading indicator while data is being fetched
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Loading location data...")
            } else if (errorMessage != null) {
                // Show error message if fetching failed
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Display the Google Map
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    // Add a marker at the fetched location
                    Marker(
                        state = MarkerState(position = LatLng(latitude, longitude)),
                        title = "Elephant Location",
                        snippet = "Lat: $latitude, Lon: $longitude"
                    )
                }
            }
        }
    }
}

// Keeping the Greeting composable for general utility, though not directly used by MainActivity now.
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
