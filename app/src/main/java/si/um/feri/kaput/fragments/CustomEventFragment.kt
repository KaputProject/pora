package si.um.feri.kaput.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.R
import si.um.feri.kaput.databinding.FragmentCustomEventBinding
import si.um.feri.kaput.models.Location
import si.um.feri.kaput.models.LocationUser
import si.um.feri.kaput.models.Transaction
import si.um.feri.kaput.utils.MqttUtil
import java.util.UUID

class CustomEventFragment : Fragment() {
    private var _binding: FragmentCustomEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication

    // Selected location data
    private var selectedLocation: Location? = null
    private var isExtremeEvent: Boolean = false
    private var isInflow: Boolean = true  // true = inflow, false = outflow

    companion object {
        private const val TAG = "CustomEventFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for location selection result from LocationMapFragment
        setFragmentResultListener(LocationMapFragment.LOCATION_RESULT_KEY) { _, bundle ->
            val locationId = bundle.getString(LocationMapFragment.LOCATION_DATA) ?: return@setFragmentResultListener
            val locationName = bundle.getString("location_name") ?: ""
            val locationLat = bundle.getDouble("location_lat")
            val locationLng = bundle.getDouble("location_lng")
            isExtremeEvent = bundle.getBoolean(LocationMapFragment.IS_EXTREME, false)

            // Create or find the location
            if (isExtremeEvent) {
                // New location for extreme event
                selectedLocation = Location(
                    _id = locationId,
                    name = locationName,
                    identifier = "custom_${System.currentTimeMillis()}",
                    lat = locationLat,
                    lng = locationLng
                )
            } else {
                // Find existing location from dataManager
                selectedLocation = app.dataManager.familyLocations.find { it._id == locationId }
                    ?: app.dataManager.userLocations.find { it._id == locationId }
                    ?: Location(
                        _id = locationId,
                        name = locationName,
                        identifier = locationName,
                        lat = locationLat,
                        lng = locationLng
                    )
            }

            updateLocationDisplay()
            updateEventTypeDisplay()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomEventBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        updateEventTypeDisplay()
        updateTransactionTypeDisplay()
    }

    private fun initUI() {
        // Location selection - navigate to map
        binding.locationSelectText.setOnClickListener {
            findNavController().navigate(R.id.action_customEventFragment_to_locationMapFragment)
        }

        // Transaction type switch (inflow/outflow)
        binding.transactionTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isInflow = isChecked
            updateTransactionTypeDisplay()
        }

        // Submit button
        binding.submitButton.setOnClickListener {
            publishTransaction()
        }
    }

    private fun updateLocationDisplay() {
        if (selectedLocation == null) {
            binding.locationSelectText.text = null
            binding.locationSelectText.hint = getString(R.string.select_location_hint)
        } else {
            if (isExtremeEvent) {
                binding.locationSelectText.text = String.format(
                    "Nova lokacija: %.4f, %.4f",
                    selectedLocation?.lat ?: 0.0,
                    selectedLocation?.lng ?: 0.0
                )
            } else {
                binding.locationSelectText.text = selectedLocation?.name
                    ?: selectedLocation?.identifier
                    ?: "Izbrana lokacija"
            }
        }
    }

    private fun updateEventTypeDisplay() {
        if (isExtremeEvent) {
            binding.eventTypeValue.text = getString(R.string.extreme_event)
            binding.eventTypeValue.setBackgroundColor(Color.parseColor("#FFCDD2")) // Light red
        } else {
            binding.eventTypeValue.text = getString(R.string.normal_event)
            binding.eventTypeValue.setBackgroundColor(Color.parseColor("#C8E6C9")) // Light green
        }
    }

    private fun updateTransactionTypeDisplay() {
        if (isInflow) {
            binding.transactionTypeValue.text = getString(R.string.inflow_label)
            binding.transactionTypeValue.setTextColor(Color.parseColor("#388E3C")) // Green
        } else {
            binding.transactionTypeValue.text = getString(R.string.outflow_label)
            binding.transactionTypeValue.setTextColor(Color.parseColor("#D32F2F")) // Red
        }
    }

    private fun publishTransaction() {
        val amountText = binding.amountInput.text?.toString()?.trim()

        // Validation
        if (amountText.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            binding.amountInput.requestFocus()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Neveljaven znesek", Toast.LENGTH_SHORT).show()
            binding.amountInput.requestFocus()
            return
        }

        if (selectedLocation == null) {
            Toast.makeText(requireContext(), "Prosim izberite lokacijo na zemljevidu", Toast.LENGTH_SHORT).show()
            return
        }

        // Build transaction JSON similar to SimulateFragment
        val jsonData = buildTransactionJson(amount)

        Log.d(TAG, "Publishing transaction: $jsonData")

        // Choose topic based on event type
        val topic = if (isExtremeEvent) {
            MqttUtil.BLOCKCHAIN_UPLOAD_TOPIC  // Extreme event -> blockchain
        } else {
            MqttUtil.EVENT_TOPIC  // Normal event -> regular topic
        }

        // Publish via MQTT
        try {
            MqttUtil.publish(app.mqttClient, topic, jsonData)

            val message = if (isExtremeEvent) {
                "Ekstremen dogodek poslan na blockchain!"
            } else {
                "Transakcija objavljena!"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            // Clear form
            clearForm()

        } catch (e: Exception) {
            Log.e(TAG, "Error publishing transaction: ${e.message}")
            Toast.makeText(requireContext(), getString(R.string.event_publish_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildTransactionJson(amount: Double): String {
        val transactions = mutableListOf<Transaction>()

        // Copy location and set transaction values
        val locationCopy = selectedLocation!!.copy()

        // Set inflow/outflow based on switch
        val inflow = if (isInflow) amount else 0.0
        val outflow = if (isInflow) 0.0 else amount

        locationCopy.total_inflow = inflow
        locationCopy.total_outflow = outflow
        locationCopy.numbOfTrans = 1

        // Create user entry for this transaction
        val userId = app.dataManager.userId ?: ""
        val username = app.dataManager.username

        val newUsers: List<LocationUser> = listOf(
            LocationUser(
                userId = userId,
                username = username,
                numbOfTrans = 1,
                inflow = inflow,
                outflow = outflow
            )
        )
        locationCopy.users = newUsers

        // Create transaction with current timestamp
        val transaction = Transaction(
            location = locationCopy,
            datetime = System.currentTimeMillis()
        )
        transactions.add(transaction)

        // Convert to JSON (same format as SimulateFragment)
        val gson: Gson = GsonBuilder().create()
        return gson.toJson(transactions)
    }

    private fun clearForm() {
        binding.amountInput.text?.clear()
        selectedLocation = null
        isExtremeEvent = false
        isInflow = true
        binding.transactionTypeSwitch.isChecked = true
        updateLocationDisplay()
        updateEventTypeDisplay()
        updateTransactionTypeDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}