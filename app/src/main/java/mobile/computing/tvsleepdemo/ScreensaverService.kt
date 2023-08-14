package mobile.computing.tvsleepdemo

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.service.dreams.DreamService
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreensaverService : DreamService() {

    private lateinit var ivLeadingLogo: ImageView
    private lateinit var videoView: VideoView
    private var globalScopeJob: Job? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        globalScopeJob = GlobalScope.launch {
            withContext(Dispatchers.Main) {
                initializeUI()
            }
            val repository = Repository(this@ScreensaverService)
            repository.handleEverythingTemporarily(ivLeadingLogo)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        globalScopeJob?.cancel()
    }

    private fun initializeUI() {
        isInteractive = false
        setContentView(R.layout.activity_main)
        ivLeadingLogo = findViewById(R.id.ivLeadingLogo)
        ivLeadingLogo.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                R.drawable.leadingpoint
            )
        )
        videoView = findViewById(R.id.videoview)
    }
}