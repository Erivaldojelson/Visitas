package com.monst.transfiranow.cards.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.monst.transfiranow.R
import com.monst.transfiranow.cards.api.RetrofitProvider
import com.monst.transfiranow.cards.data.CardsRepository
import com.monst.transfiranow.databinding.ActivityCreateCardBinding
import kotlinx.coroutines.launch

class CreateCardActivity : ComponentActivity() {
    private lateinit var binding: ActivityCreateCardBinding

    private val viewModel: CreateCardViewModel by viewModels {
        CreateCardViewModel.Factory(CardsRepository(RetrofitProvider.cardsApi))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setTitle(R.string.create_card_title)

        binding.createButton.setOnClickListener {
            val name = binding.nameEdit.text?.toString().orEmpty()
            val photoUrl = binding.photoEdit.text?.toString().orEmpty()
            val instagram = binding.instagramEdit.text?.toString()
            val whatsapp = binding.whatsappEdit.text?.toString()
            val website = binding.websiteEdit.text?.toString()

            val validationError = validate(name = name, photoUrl = photoUrl, website = website)
            if (validationError != null) {
                Snackbar.make(binding.root, validationError, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.createCard(
                name = name,
                photo = photoUrl,
                instagram = instagram,
                whatsapp = whatsapp,
                website = website
            )
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progress.isVisible = state.isLoading
                binding.createButton.isEnabled = !state.isLoading

                state.errorMessage?.let { msg ->
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                }

                val created = state.createdCard
                if (created != null) {
                    startActivity(
                        Intent(this@CreateCardActivity, CardResultActivity::class.java).apply {
                            putExtra(CardResultActivity.EXTRA_NAME, created.name)
                            putExtra(CardResultActivity.EXTRA_PHOTO_URL, created.photo)
                            putExtra(CardResultActivity.EXTRA_QR_BASE64, created.qrCode)
                        }
                    )
                }
            }
        }
    }

    private fun validate(name: String, photoUrl: String, website: String?): String? {
        if (name.trim().length < 2) return getString(R.string.error_name_required)
        if (photoUrl.isBlank()) return getString(R.string.error_photo_required)
        if (!Patterns.WEB_URL.matcher(photoUrl.trim()).matches()) return getString(R.string.error_photo_invalid)
        val w = website?.trim().orEmpty()
        if (w.isNotBlank() && !Patterns.WEB_URL.matcher(w).matches()) return getString(R.string.error_website_invalid)
        return null
    }
}

