package com.kotirao.gpsnotes

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.io.InputStream

class PostCaptureDialogFragment : DialogFragment() {

    interface Listener {
        fun onSaveClicked(photoUri: String, note: String)
        fun onDeleteClicked(photoUri: String)
    }

    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) listener = context
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoUri = arguments?.getString(ARG_URI) ?: ""
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_presets, null)
        val edit = view.findViewById<android.widget.EditText>(R.id.editPreset)
        val addBtn = view.findViewById<android.widget.Button>(R.id.btnAddPreset)

        // populate presets spinner if needed (we reuse dialog_presets layout)
        val presets = PresetsManager.getPresets(requireContext())
        val spinner = android.widget.Spinner(requireContext())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, viewSel: android.view.View?, position: Int, id: Long) {
                edit.setText(presets[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        addBtn.setOnClickListener {
            val text = edit.text.toString().trim()
            if (text.isNotEmpty()) {
                PresetsManager.addPreset(requireContext(), text)
                // refresh spinner adapter
                adapter.clear(); adapter.addAll(PresetsManager.getPresets(requireContext()))
            }
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Post capture")
        // preview thumbnail
        try {
            val input: InputStream? = requireContext().contentResolver.openInputStream(Uri.parse(photoUri))
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            val imageView = android.widget.ImageView(requireContext())
            imageView.setImageBitmap(bmp)
            val container = android.widget.LinearLayout(requireContext())
            container.orientation = android.widget.LinearLayout.VERTICAL
            container.addView(imageView)
            container.addView(spinner)
            container.addView(edit)
            container.addView(addBtn)
            builder.setView(container)
        } catch (e: Exception) {
            builder.setView(view)
        }

        builder.setPositiveButton("Save") { _, _ ->
            val note = edit.text.toString()
            listener?.onSaveClicked(photoUri, note)
        }
        builder.setNeutralButton("Delete") { _, _ ->
            listener?.onDeleteClicked(photoUri)
        }
        builder.setNegativeButton("Cancel", null)
        return builder.create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        private const val ARG_URI = "photoUri"
        fun newInstance(uri: String): PostCaptureDialogFragment {
            val f = PostCaptureDialogFragment()
            val b = Bundle()
            b.putString(ARG_URI, uri)
            f.arguments = b
            return f
        }
    }
}
