package com.rtvvoice.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtvvoice.app.R
import com.rtvvoice.app.databinding.DialogAboutBinding

class AboutDialog : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DialogAboutBinding.inflate(inflater, container, false)
        binding.message.text = getString(R.string.about_message)
        return binding.root
    }
}
