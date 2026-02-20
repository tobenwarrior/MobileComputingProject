package com.example.snapandcook.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.snapandcook.R
import com.example.snapandcook.databinding.ActivitySplashBinding
import com.example.snapandcook.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen shown on app launch.
 *
 * Displays the brand logo and tagline with a fade-in animation,
 * then navigates to [MainActivity] after a short delay.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate logo and tagline in
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 800
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)
        binding.tvTagline.startAnimation(fadeIn)

        // Navigate to MainActivity after 2 seconds
        lifecycleScope.launch {
            delay(2000)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
