package mobile.computing.tvsleepdemo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private lateinit var repository: Repository
    private lateinit var fileManager: FileManager
    private lateinit var etImageDuration: EditText
    private lateinit var btnUpload: Button

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (result.data != null) {
                    val task: Task<GoogleSignInAccount> =
                        GoogleSignIn.getSignedInAccountFromIntent(intent)

                    task.run {
                        GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                            ?.let { googleAccount ->
                                // get credentials
                                val credential = GoogleAccountCredential.usingOAuth2(
                                    this@MainActivity,
                                    listOf(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE)
                                )
                                credential.selectedAccount = googleAccount.account!!
                            }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        startForResult.launch(getGoogleSignInClient(this).signInIntent)
        fileManager = FileManager(null)
        repository = Repository(this, fileManager)
        GlobalScope.launch {
            repository.setupDrive()
            withContext(Dispatchers.Main) {
                initializeUploadButtonListener()
            }
        }
    }

    private fun initializeUI() {
        etImageDuration = findViewById(R.id.etImageDuration)
        btnUpload = findViewById(R.id.btnUpload)
    }

    private fun initializeUploadButtonListener() {
        btnUpload.setOnClickListener {
            showToast("Uploading...", true)
            lifecycleScope.launch {
                val imageDuration = withContext(Dispatchers.Main) {
                    if (etImageDuration.text.isNotEmpty())
                     etImageDuration.text.toString().toInt() * 1000
                    else
                        repository.defaultAppSettings.imageDuration
                }
                withContext(Dispatchers.IO) {
                    val appSettings = AppSettings(true, true, imageDuration)
                    val gson = Gson()
                    val json = gson.toJson(appSettings)
                    val folders = repository.googleDriveClient?.getAllFoldersWithName(repository.folderName)        // May need an enhancement
                    folders?.let {
                        if (folders.files.isNotEmpty()) {
                            val files = repository.googleDriveClient?.getAllFilesWithNameInFolder(folders.files[0].id, fileManager.settingsFileName)        // May need an enhancement
                            files?.let {
                                if (files.files.isNotEmpty()) {
                                    val mediaContent = ByteArrayContent("application/json", json.toByteArray())
                                    repository.googleDriveClient?.updateFile(files.files[0].id, mediaContent)
                                } else {
                                    val fileMetadata = File()
                                    fileMetadata.name = fileManager.settingsFileName
                                    fileMetadata.mimeType = "application/json"
                                    fileMetadata.parents = listOf(folders.files[0].id)
                                    val mediaContent = ByteArrayContent("application/json", json.toByteArray())
                                    repository.googleDriveClient?.createFile("id", fileMetadata, mediaContent)
                                }
                                withContext(Dispatchers.Main) {
                                    showToast("Settings Updated", false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE))
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    private fun showToast(message: String, isShort: Boolean) {
        if (isShort)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}


