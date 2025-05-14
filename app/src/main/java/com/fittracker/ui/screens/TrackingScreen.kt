package com.fittracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittracker.utils.LocationPermissionHandler
import com.fittracker.viewmodel.LocationTrackingViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.concurrent.TimeUnit
import androidx.compose.animation.AnimatedVisibility
import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

@Composable
@SuppressLint("MissingPermission")
fun TrackingScreen(
    viewModel: LocationTrackingViewModel = viewModel()
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val trackingStats by viewModel.trackingStats.collectAsState()
    var mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) }
    var cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }
    var expanded by remember { mutableStateOf(false) }
    var cameraMoved by remember { mutableStateOf(false) }

    // Check location permissions
    LaunchedEffect(Unit) {
        if (LocationPermissionHandler.hasLocationPermission(context)) {
            mapProperties = mapProperties.copy(isMyLocationEnabled = true)
            // Try to get last known location for initial camera position
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        15f
                    )
                }
            }
        }
    }

    // Animate camera to user location only on first update
    LaunchedEffect(currentLocation) {
        if (!cameraMoved) {
            currentLocation?.let { location ->
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f
                    )
                )
                cameraMoved = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true,
                    mapToolbarEnabled = false
                )
            ) {
                // Draw the route
                trackingStats?.locations?.let { locations ->
                    if (locations.isNotEmpty()) {
                        Polyline(
                            points = locations.map { LatLng(it.latitude, it.longitude) },
                            color = MaterialTheme.colorScheme.primary,
                            width = 8f
                        )

                        // Show current location marker
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    locations.last().latitude,
                                    locations.last().longitude
                                )
                            ),
                            title = "Current Location"
                        )
                    }
                }
            }
        }

        // Stats and Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Distance",
                    value = String.format("%.2f km", (trackingStats?.distance ?: 0f) / 1000f)
                )
                StatItem(
                    label = "Duration",
                    value = formatDuration(trackingStats?.duration ?: 0)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide details" else "Show details"
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Steps",
                            value = (trackingStats?.steps ?: 0).toString()
                        )
                        StatItem(
                            label = "Speed",
                            value = String.format("%.2f km/h", (trackingStats?.speed ?: 0f) * 3.6f)
                        )
                        StatItem(
                            label = "Calories",
                            value = String.format("%.0f kcal", trackingStats?.calories ?: 0f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Current HR",
                            value = "${trackingStats?.currentHeartRate ?: 0} bpm"
                        )
                        StatItem(
                            label = "Avg HR",
                            value = "${trackingStats?.averageHeartRate ?: 0} bpm"
                        )
                        StatItem(
                            label = "Max HR",
                            value = "${trackingStats?.maxHeartRate ?: 0} bpm"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Button
            Button(
                onClick = { if (isTracking) viewModel.stopTracking() else viewModel.startTracking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) "Stop Tracking" else "Start Tracking"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTracking) "Stop Tracking" else "Start Tracking")
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
} 