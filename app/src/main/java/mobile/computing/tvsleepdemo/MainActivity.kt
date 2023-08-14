package mobile.computing.tvsleepdemo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private lateinit var viewModelTv: ViewModelTV
    private lateinit var repository: Repository
    private lateinit var ivLeadingLogo: ImageView
    private lateinit var videoView: VideoView

/*    private val startForResult =
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

                                // get Drive Instance
                                val drive = getDrive(credential)

                                lifecycleScope.launch(Dispatchers.IO) {
                                    val folderName = "Leading Point"
                                    // Query for the folder by its name

                                    val folderSearchResult: FileList? = getAllFoldersWithName(folderName, drive)
                                    val folders: List<File>? = folderSearchResult?.files

                                    val imagesUrls: MutableList<String?> = mutableListOf()
                                    populateImageUrlsList(folders, drive, imagesUrls)
                                    if (imagesUrls.isNotEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            getAndDisplayImageUsingGlide(imagesUrls[0])
                                        }
                                    }
                                }
                            }
                    }
                } else {
                    Log.d("MYTAG", "Null")
                }
            }
        }*/



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
//
        repository = Repository(this)
        viewModelTv =
            ViewModelProvider(this, ViewModelTvFactory(repository))[ViewModelTV::class.java]

//        startForResult.launch(getGoogleSignInClient(this).signInIntent)


    }

    private fun initializeUI() {
        ivLeadingLogo = findViewById(R.id.ivLeadingLogo)
        videoView = findViewById(R.id.videoview)
    }

/*    private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE))
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    private fun getDrive(credential: GoogleAccountCredential?): Drive? =
        Drive
            .Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName(this@MainActivity.getString(R.string.app_name))
            .build()

    private fun populateImageUrlsList(
        folders: List<File>?,
        drive: Drive?,
        imagesUrls: MutableList<String?>
    ) {
        folders?.let {
            if (folders.isNotEmpty()) {
                val filesQuery =
                    "'${folders[0].id}' in parents and mimeType contains 'image/'"
                val filesSearchResult: FileList? = drive?.files()?.list()
                    ?.setQ(filesQuery)
                    ?.setFields("files(id, name, thumbnailLink)")
                    ?.execute()
                val images: List<File>? = filesSearchResult?.files
                images?.let {
                    for (image in images) {
                        val imageId: String = image.id
                        val imageName: String = image.name
                        val thumbnailLink: String? = image.thumbnailLink
                        Log.d(
                            "MYTAG",
                            "Image ID: $imageId, Image Name: $imageName, Thumbnail Link: $thumbnailLink"
                        )
                        imagesUrls.add(thumbnailLink)
                    }
                }
            }
        }
    }

    private fun getAllFoldersWithName(folderName: String, drive: Drive?): FileList? {
        val folderQuery =
            "mimeType='application/vnd.google-apps.folder' and name='$folderName'"
        return drive?.files()?.list()
            ?.setQ(folderQuery)
            ?.setFields("files(id, name)")
            ?.execute()
    }

    private fun getAndDisplayImageUsingGlide(thumbnailLink: String?) = Glide.with(this)
        .load(thumbnailLink)
        .fitCenter()
        .placeholder(R.drawable.leadingpoint) // Placeholder image while loading
        .error(R.drawable.leadingpoint) // Error image if the load fails
        .into(ivLeadingLogo)*/
}


