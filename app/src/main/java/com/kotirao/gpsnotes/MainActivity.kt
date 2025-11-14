package com.kotirao.gpsnotes

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), PostCaptureDialogFragment.Listener {

    private lateinit var previewImage: ImageView
    private lateinit var btnCapture: Button
    private lateinit var btnSettings: Button
    private lateinit var btnGallery: Button

    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private var lastKnownLocation: Location? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("photoUri")
            val photoUri = if (uriString != null) Uri.parse(uriString) else null
            photoUri?.let { handleCapturedUri(it) }
        } else {
            Toast.makeText(this, "Capture canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewImage = findViewById(R.id.previewImage)
        btnCapture = findViewById(R.id.btnCapture)
        btnGallery = findViewById(R.id.btnGallery)
        btnSettings = findViewById(R.id.btnSettings)

        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        btnGallery.setOnClickListener { startActivity(Intent(this, GalleryActivity::class.java)) }

        btnCapture.setOnClickListener { startCaptureFlow() }

        requestPermissionsIfNeeded()
        startLocationUpdates()
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.CAMERA)
        if (perms.isNotEmpty()) ActivityCompat.requestPermissions(this, perms.toTypedArray(), 2001)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val provider = LocationManager.GPS_PROVIDER
        try {
            locationManager.requestLocationUpdates(provider, 2000L, 1f, object : LocationListener {
                override fun onLocationChanged(location: Location) { lastKnownLocation = location }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            })
            lastKnownLocation = locationManager.getLastKnownLocation(provider)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun startCaptureFlow() {
        val prefs = getSharedPreferences("androidx.preference.PreferenceManager_default", MODE_PRIVATE)
        val cameraEnabled = prefs.getBoolean("pref_toggle_camera", true)
        if (cameraEnabled) {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent)
        } else {
            generateAndHandleNoCamera()
        }
    }

    private fun generateAndHandleNoCamera() {
        val bmp = BitmapFactory.decodeResource(resources, android.R.drawable.screen_background_light)
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val name = "GPSNotes_${sdf.format(Date())}.jpg"
        val picturesDir = getExternalFilesDir("Pictures/GPSNotes")
        if (picturesDir?.exists() == false) picturesDir.mkdirs()
        val file = File(picturesDir, name)
        try {
            file.outputStream().use { out -> bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out) }
            handleCapturedUri(Uri.fromFile(file))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun handleCapturedUri(uri: Uri) {
        val prefs = getSharedPreferences("androidx.preference.PreferenceManager_default", MODE_PRIVATE)
        val autoSave = prefs.getBoolean("pref_auto_save", true)
        val noteMode = prefs.getString("pref_note_mode", "user") ?: "user"

        if (autoSave) {
            // auto-save: apply default/preset note if needed and save metadata
            val note = when (noteMode) {
                "preset" -> {
                    val p = PresetsManager.getPresets(this)
                    if (p.isNotEmpty()) p.first() else ""
                }
                "none" -> ""
                else -> ""
            }
            applyOverlayAndSave(uri, note)
        } else {
            // show DialogFragment to get note or delete
            val dlg = PostCaptureDialogFragment.newInstance(uri.toString())
            dlg.show(supportFragmentManager, "postCapture")
        }
    }

    private fun applyOverlayAndSave(uri: Uri, note: String?) {
        try {
            // Load bitmap
            val input = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()

            val prefs = getSharedPreferences("androidx.preference.PreferenceManager_default", MODE_PRIVATE)
            val includeGPS = prefs.getBoolean("pref_toggle_gps", true)
            val includeAddress = prefs.getBoolean("pref_toggle_address", true)
            val includeDateTime = prefs.getBoolean("pref_toggle_datetime", true)

            val loc = lastKnownLocation
            val lines = mutableListOf<String>()
            if (includeGPS && loc != null) {
                lines.add("Lat: ${loc.latitude}")
                lines.add("Lon: ${loc.longitude}")
                lines.add("Accuracy: ${loc.accuracy}")
            }
            if (includeDateTime) {
                val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                lines.add("Date: $dateTime")
            }
            if (includeAddress && loc != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    if (!list.isNullOrEmpty()) lines.add("Address: ${list[0].getAddressLine(0)}")
                } catch (e: Exception) { e.printStackTrace() }
            }
            if (!note.isNullOrEmpty()) lines.add("Note: $note")

            val annotated = ImageUtils.drawOverlayOnBitmap(bmp, lines)

            // Save annotated image as new MediaStore entry
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val name = "GPSNotes_annotated_${sdf.format(Date())}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GPSNotes")
            }
            val outUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            outUri?.let {
                var out: OutputStream? = null
                try {
                    out = contentResolver.openOutputStream(it)
                    annotated.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                } finally {
                    out?.close()
                }
                // Save CSV metadata
                CsvStore.appendEntry(this, CsvStore.Entry(name, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()), loc?.latitude, loc?.longitude, loc?.accuracy, null, note))
                runOnUiThread { previewImage.setImageBitmap(annotated); Toast.makeText(this, "Saved annotated image", Toast.LENGTH_SHORT).show() }
            }

        } catch (e: Exception) { e.printStackTrace() }
    }

    // DialogFragment callbacks
    override fun onSaveClicked(photoUri: String, note: String) {
        applyOverlayAndSave(Uri.parse(photoUri), note)
    }

    override fun onDeleteClicked(photoUri: String) {
        // delete the original MediaStore Uri
        try {
            val uri = Uri.parse(photoUri)
            contentResolver.delete(uri, null, null)
            Toast.makeText(this, "Deleted image", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
