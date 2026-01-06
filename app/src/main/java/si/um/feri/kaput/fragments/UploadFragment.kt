package si.um.feri.kaput.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import si.um.feri.kaput.MyApplication
import si.um.feri.kaput.databinding.FragmentUploadBinding
import si.um.feri.kaput.utils.HttpUtil
import si.um.feri.kaput.utils.MqttUtil
import java.io.ByteArrayOutputStream
import java.util.Date

class UploadFragment : Fragment() {
    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: MyApplication
    private var image: Bitmap? = null
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2

    private lateinit var takeImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        app = requireActivity().application as MyApplication
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        takeImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val img = result.data?.extras?.get("data") as? Bitmap
                    if (img != null) {
                        image = img
                        binding.imagePreview.setImageBitmap(img)
                    } else {
                        Toast.makeText(context, "Failed to take image", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    if (uri != null) {
                        val bitmap =
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                        image = bitmap
                        binding.imagePreview.setImageBitmap(bitmap)
                    } else {
                        Toast.makeText(context, "Failed to pick image", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        initButtons()
    }

    private fun initButtons() {
        val context = requireContext()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val url = prefs.getString("vid_url", "http://10.0.2.2:5000/")!! + "/analiziraj"

        binding.takeImageButton.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takeImageLauncher.launch(takePictureIntent)
        }

        binding.chooseImageButton.setOnClickListener {
            val pickImage = Intent(Intent.ACTION_PICK)
            pickImage.type = "image/*"
            pickImageLauncher.launch(pickImage)
        }

        binding.submitButton.setOnClickListener {
            if (image != null) {
                val stream = ByteArrayOutputStream()
                image!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val byteArray = stream.toByteArray()

                val requestBody =
                    MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
                            "slika", "upload.jpg", byteArray.toRequestBody()
                        ).addFormDataPart("cas", Date().toInstant().toString()).build()

                HttpUtil.URVRVPostRequest(
                    app.httpClient, context, url, requestBody, app.mqttClient, MqttUtil.UPLOAD_TOPIC
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}