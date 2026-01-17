package si.um.feri.kaput.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.R
import si.um.feri.kaput.databinding.FragmentLocationMapBinding
import si.um.feri.kaput.models.Location
import java.util.UUID

class LocationMapFragment : Fragment() {
    private var _binding: FragmentLocationMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication

    private var selectedLocation: Location? = null
    private var isExtremeEvent: Boolean = false
    private var customMarker: Marker? = null

    companion object {
        const val LOCATION_RESULT_KEY = "location_result"
        const val LOCATION_DATA = "location_data"
        const val IS_EXTREME = "is_extreme"

        // Default center - Slovenia (Maribor area)
        private const val DEFAULT_LAT = 46.5547
        private const val DEFAULT_LNG = 15.6459
        private const val DEFAULT_ZOOM = 12.0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = requireContext().packageName

        _binding = FragmentLocationMapBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupButtons()
        addExistingLocationMarkers()
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)

        val mapController = binding.mapView.controller
        mapController.setZoom(DEFAULT_ZOOM)

        // Center on existing locations if available, otherwise default
        val locations = app.dataManager.familyLocations
        if (locations.isNotEmpty()) {
            val firstLoc = locations.first()
            val lat = firstLoc.lat ?: DEFAULT_LAT
            val lng = firstLoc.lng ?: DEFAULT_LNG
            mapController.setCenter(GeoPoint(lat, lng))
        } else {
            mapController.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LNG))
        }

        // Handle map clicks for new locations (extreme events)
        binding.mapView.setOnClickListener { } // Consume to prevent parent handling

        val mapEventsOverlay = object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e != null && mapView != null) {
                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                    // Check if click is near any existing location marker
                    val existingLoc = findNearbyExistingLocation(geoPoint)

                    if (existingLoc != null) {
                        // Clicked on existing location - normal event
                        selectExistingLocation(existingLoc)
                    } else {
                        // Clicked elsewhere - extreme event (new location)
                        selectNewLocation(geoPoint)
                    }
                    return true
                }
                return false
            }
        }
        binding.mapView.overlays.add(0, mapEventsOverlay)
    }

    private fun findNearbyExistingLocation(clickPoint: GeoPoint): Location? {
        val threshold = 0.002 // Approximately 200m

        for (loc in app.dataManager.familyLocations) {
            val lat = loc.lat ?: continue
            val lng = loc.lng ?: continue

            val distance = Math.sqrt(
                Math.pow(clickPoint.latitude - lat, 2.0) +
                Math.pow(clickPoint.longitude - lng, 2.0)
            )

            if (distance < threshold) {
                return loc
            }
        }
        return null
    }

    private fun selectExistingLocation(location: Location) {
        selectedLocation = location
        isExtremeEvent = false

        // Remove custom marker if exists
        customMarker?.let { binding.mapView.overlays.remove(it) }
        customMarker = null

        // Update UI
        binding.selectedLocationText.text = getString(
            R.string.existing_location_selected,
            location.name ?: location.identifier ?: "Unknown"
        )
        binding.confirmButton.isEnabled = true

        binding.mapView.invalidate()
    }

    private fun selectNewLocation(geoPoint: GeoPoint) {
        // Create a new location for extreme event
        selectedLocation = Location(
            _id = UUID.randomUUID().toString(),
            name = "Nova lokacija",
            identifier = "custom_${System.currentTimeMillis()}",
            lat = geoPoint.latitude,
            lng = geoPoint.longitude
        )
        isExtremeEvent = true

        // Remove old custom marker
        customMarker?.let { binding.mapView.overlays.remove(it) }

        // Add new custom marker (red for extreme event)
        customMarker = Marker(binding.mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Nova lokacija (ekstremen dogodek)"
            // Red tint for extreme event marker
            icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null).apply {
                setTint(Color.RED)
            }
        }
        binding.mapView.overlays.add(customMarker)

        // Update UI
        binding.selectedLocationText.text = getString(
            R.string.new_location_selected,
            geoPoint.latitude,
            geoPoint.longitude
        )
        binding.confirmButton.isEnabled = true

        binding.mapView.invalidate()
    }

    private fun addExistingLocationMarkers() {
        val locations = app.dataManager.familyLocations

        for (location in locations) {
            val lat = location.lat ?: continue
            val lng = location.lng ?: continue

            val marker = Marker(binding.mapView)
            marker.position = GeoPoint(lat, lng)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = location.name ?: location.identifier ?: "Lokacija"
            marker.snippet = location.address

            // Blue tint for existing locations
            marker.icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null).apply {
                setTint(Color.BLUE)
            }

            marker.setOnMarkerClickListener { _, _ ->
                selectExistingLocation(location)
                true
            }

            binding.mapView.overlays.add(marker)
        }

        binding.mapView.invalidate()
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.confirmButton.setOnClickListener {
            selectedLocation?.let { loc ->
                // Return result to CustomEventFragment
                setFragmentResult(LOCATION_RESULT_KEY, bundleOf(
                    LOCATION_DATA to loc._id,
                    "location_name" to (loc.name ?: loc.identifier ?: ""),
                    "location_lat" to (loc.lat ?: 0.0),
                    "location_lng" to (loc.lng ?: 0.0),
                    IS_EXTREME to isExtremeEvent
                ))
                findNavController().popBackStack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
