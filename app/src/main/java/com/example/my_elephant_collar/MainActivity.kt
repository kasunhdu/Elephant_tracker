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
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

import com.example.my_elephant_collar.ui.theme.My_elephant_collarTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = FirebaseDatabase.getInstance().reference

        setContent {
            My_elephant_collarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationMapScreen(databaseRef = database, elephantId = "elephantId123") // Specify the elephant ID
                }
            }
        }
    }
}

@Composable
fun LocationMapScreen(databaseRef: DatabaseReference, elephantId: String) {
    // State to hold the historical locations as LatLng objects
    var historicalLocations by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    // State to hold the latest known location
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
    // State to indicate if data is still loading
    var isLoading by remember { mutableStateOf(true) }
    // State to hold any error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Camera position state for the Google Map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 5f) // Default zoom
    }

    // DisposableEffect to manage the Firebase listener lifecycle
    DisposableEffect(databaseRef, elephantId) {
        // Reference to the specific elephant's movements in Firebase
        val movementsRef = databaseRef.child("elephantMovements").child(elephantId)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Temporary list to store locations with their timestamps for sorting
                val tempLocationsWithTimestamps = mutableListOf<Pair<Long, LatLng>>()
                var latestTimestamp: Long = 0L
                var latestLocation: LatLng? = null

                // Iterate through each child snapshot (which should be a timestamp key)
                for (locationSnapshot in snapshot.children) {
                    // Get the timestamp from the key and convert it to Long
                    val timestamp = locationSnapshot.key?.toLongOrNull()

                    val latitude = locationSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = locationSnapshot.child("longitude").getValue(Double::class.java)

                    if (timestamp != null && latitude != null && longitude != null) {
                        val latLng = LatLng(latitude, longitude)
                        tempLocationsWithTimestamps.add(Pair(timestamp, latLng))

                        // Keep track of the latest location based on timestamp
                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp
                            latestLocation = latLng
                        }
                    }
                }

                // Sort the locations by timestamp
                historicalLocations = tempLocationsWithTimestamps
                    .sortedBy { it.first } // Sort by the timestamp (first element of the Pair)
                    .map { it.second } // Map back to just LatLng objects

                currentLatLng = latestLocation

                // If we have a latest location, move the camera there
                latestLocation?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 10f)
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

        // Add the listener to the Firebase reference
        movementsRef.addValueEventListener(valueEventListener)

        // When the composable leaves the composition, remove the listener
        onDispose {
            movementsRef.removeEventListener(valueEventListener)
        }
    }

    // Scaffold provides basic Material Design visual structure
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
                Text("Loading elephant movement data...")
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
                    // Draw the Polyline for the historical path
                    if (historicalLocations.isNotEmpty()) {
                        Polyline(points = historicalLocations, color = MaterialTheme.colorScheme.primary)
                    }

                    // Optionally, add a marker for the latest known location
                    currentLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Current Location",
                            snippet = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                        )
                    }
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
