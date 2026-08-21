package com.manette.app.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.manette.app.databinding.ActivitySplashBinding
import com.manette.app.ui.home.HomeActivity

/**
 * Splash screen with logo animation and water ripple effects.
 * Transitions automatically to HomeActivity after animations complete.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Full immersive mode
        window.decorView.windowInsetsController?.apply {
            hide(android.view.WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        startAnimations()
    }

    private fun startAnimations() {
        // Cover image fade-in plein écran (immédiat)
        binding.imgCover.animate()
            .alpha(1f)
            .setDuration(900)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // App name fade-in + slide up (delay 600ms)
        binding.tvAppName.translationY = 40f
        binding.tvAppName.postDelayed({
            binding.tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }, 600)

        // Tagline fade in (delay 900ms)
        binding.tvTagline.postDelayed({
            binding.tvTagline.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
        }, 900)

        // Progress bar (delay 1100ms)
        binding.progressBar.postDelayed({
            binding.progressBar.animate()
                .alpha(1f)
                .setDuration(400)
                .start()
        }, 1100)

        // Navigate to Home (after 2800ms total)
        binding.root.postDelayed({
            navigateToHome()
        }, 2800)
    }

    private fun navigateToHome() {
        // Fade out entire screen
        binding.root.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                startActivity(Intent(this, HomeActivity::class.java))
                overridePendingTransition(
                    com.manette.app.R.anim.fade_in,
                    com.manette.app.R.anim.fade_out
                )
                finish()
            }
            .start()
    }
}
