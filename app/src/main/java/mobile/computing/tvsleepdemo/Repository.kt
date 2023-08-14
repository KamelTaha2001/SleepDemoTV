package mobile.computing.tvsleepdemo

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Repository(val context: Context) {

    fun handleEverythingTemporarily(iv: ImageView, vv: VideoView) {
        GoogleSignIn.getLastSignedInAccount(context)
            ?.let { googleAccount ->

                // get credentials
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account!!

                // get Drive Instance
                val drive = getDrive(credential)

                GlobalScope.launch(Dispatchers.IO) {
                    val folderName = "Leading Point"
                    // Query for the folder by its name

                    val folderSearchResult: FileList? = getAllFoldersWithName(folderName, drive)
                    val folders: List<File>? = folderSearchResult?.files

                    /*val imagesUrls: MutableList<String?> = mutableListOf()
                    populateImageUrlsList(folders, drive, imagesUrls)
                    if (imagesUrls.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            getAndDisplayImageUsingGlide(imagesUrls[0], iv)
                        }
                    }*/

                    val videosUrls: MutableList<String?> = mutableListOf()
                    populateVideoUrlsList(folders, drive, videosUrls)
                    if (videosUrls.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            getAndDisplayVideo(videosUrls[0], vv, iv)
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

    private fun getDrive(credential: GoogleAccountCredential?): Drive? =
        Drive
            .Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName(context.getString(R.string.app_name))
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

    private fun populateVideoUrlsList(
        folders: List<File>?,
        drive: Drive?,
        videoUrls: MutableList<String?>
    ) {
        folders?.let {
            if (folders.isNotEmpty()) {
                val filesQuery =
                    "'${folders[0].id}' in parents and mimeType contains 'video/'"
                val filesSearchResult: FileList? = drive?.files()?.list()
                    ?.setQ(filesQuery)
                    ?.setFields("files(id, name, webContentLink)")
                    ?.execute()
                val videos: List<File>? = filesSearchResult?.files
                videos?.let {
                    for (video in videos) {
                        val videoId: String = video.id
                        val videoName: String = video.name
                        val videoLink: String? = video.webContentLink
                        Log.d(
                            "MYTAG",
                            "Video ID: $videoId, Video  Name: $videoName, Video Link: $videoLink"
                        )
                        videoUrls.add(videoLink)
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

    private fun getAndDisplayImageUsingGlide(thumbnailLink: String?, iv: ImageView) = Glide.with(context)
        .load(thumbnailLink)
        .fitCenter()
        .placeholder(R.drawable.leadingpoint) // Placeholder image while loading
        .error(R.drawable.leadingpoint) // Error image if the load fails
        .into(iv)

    private fun getAndDisplayVideo(videoUrl: String?, vv: VideoView, imageToHide: ImageView) {
        vv.setVideoURI(Uri.parse(videoUrl))
        vv.setOnPreparedListener {
            imageToHide.setImageDrawable(null)
            vv.start()
        }
    }
}