package mobile.computing.tvsleepdemo

import GoogleDriveClient
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.FileReader

/**
 * @property googleAccount The Google account to sign in with.
 * @property videosOnDriveFlow After retrieving all the videos from Google Drive, they are emitted to this flow. Collected at [ScreensaverService.decideDownloadOrDeleteFiles]
 * @property imagesOnDriveFlow After retrieving all the images from Google Drive, they are emitted to this flow. Collected at [ScreensaverService.decideDownloadOrDeleteFiles]
 * @property appSettingsFlow After retrieving the json settings file from Google Drive, it is emitted to this flow. Collected at [ScreensaverService.collectSettingsAndStartSlideshow]
 * @property folderName The name of the folder on Google Drive where all the files are stored.
 */
class Repository(
    private val context: Context,
    private val fileManager: FileManager,
) {
    var googleAccount: GoogleSignInAccount? = null
    val videosOnDriveFlow = MutableSharedFlow<List<File>>(0)
    val imagesOnDriveFlow = MutableSharedFlow<List<File>>(0)
    val appSettingsFlow = MutableSharedFlow<AppSettings?>(0)
    val folderName = "Leading Point"
    var googleDriveClient: GoogleDriveClient? = null
    val defaultAppSettings = AppSettings(true, true, 5000)

    /**
    - Signs in using the last signed in account.
    - Initializes the [googleDriveClient] instance.
    - populates [imagesOnDriveFlow] and [videosOnDriveFlow] and [appSettingsFlow].
     * @return A boolean indicating whether the lastSignedInAccount process was successfull or not.
     */
    fun setupDrive(): Boolean {
        val lastSignInResult = GoogleSignIn.getLastSignedInAccount(context)
        return if (lastSignInResult == null) {
            false
        } else {
            this.googleAccount = lastSignInResult
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = lastSignInResult.account
            googleDriveClient = GoogleDriveClient()
            googleDriveClient?.retrieveDrive(credential, context.getString(R.string.app_name))
            true
        }
    }

    fun retrieveAllData() {
        // Query for the folder containing all the files by its name
        val folderSearchResult: FileList? =
            googleDriveClient?.getAllFoldersWithName(folderName)
        val folders: List<File>? = folderSearchResult?.files

        GlobalScope.launch {
            getAndStoreAppSettingsFile(folders)
        }
        GlobalScope.launch {
            populateFilesList(folders, FileType.IMAGE)
        }
        GlobalScope.launch {
            populateFilesList(folders, FileType.VIDEO)
        }
    }

    /**
     * Retrieves all the images or videos from [folderName] on Google Drive and emits them to [imagesOnDriveFlow] or [videosOnDriveFlow].
     * Does not retrieve the files in the trash.
     * @param folders The list of folders with the name of [folderName] on Google Drive.
     */
    private suspend fun populateFilesList(
        folders: List<File>?,
        fileType: FileType
    ) {
        folders?.let {
            if (folders.isNotEmpty()) {
                val files: List<File>? =
                    googleDriveClient?.getAllFilesInFolder(folders[0], fileType)
                files?.let {
                    when (fileType) {
                        FileType.IMAGE -> imagesOnDriveFlow.emit(files)
                        FileType.VIDEO -> videosOnDriveFlow.emit(files)
                    }
                }
            }
        }
    }

    /**
     * Retrieves the [appSettingsFlow] json file from Google Drive and stores it in [FileManager.downloadsFolder]/[FileManager.rootFolder].
     * Suspends due to the call of [Repository.setAppSettings] which emits the newly retrieved app settings.
     * @param folders The list of folders with the name of [folderName] on Google Drive.
     */
    private suspend fun getAndStoreAppSettingsFile(
        folders: List<File>?,
    ) {
        folders?.let {
            if (folders.isNotEmpty()) {
                val files: List<File>? = googleDriveClient?.getAllFilesWithNameInFolder(
                    folders[0].id,
                    fileManager.settingsFileName
                )?.files
                files?.let {
                    for (file in files) {
                        try {
                            val filePathWithName =
                                "${fileManager.downloadsFolder}/${fileManager.rootFolder}/${fileManager.settingsFileName}" // The path where the file will be stored
                            googleDriveClient?.downloadFile(file.id, filePathWithName)
                            setAppSettings() // Emits the new value of app settings
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads the app settings json file from [FileManager.downloadsFolder]/[FileManager.rootFolder].
     * Then emits the file. Collected in [ScreensaverService.collectSettingsAndStartSlideshow].
     */
    private suspend fun setAppSettings() {
        withContext(Dispatchers.IO) {
            val gson = Gson()
            try {
                val jsonReader = FileReader(
                    java.io.File(
                        fileManager.downloadsFolder,
                        "${fileManager.rootFolder}/${fileManager.settingsFileName}"
                    )
                )
                val settings: AppSettings =
                    gson.fromJson(jsonReader, AppSettings::class.java)
                jsonReader.close()
                appSettingsFlow.emit(settings)
            } catch (e: FileNotFoundException) {
                appSettingsFlow.emit(defaultAppSettings)
            }
        }
    }

    /**
     * Downloads a specific file from the Google Drive. Used when a file is found on Google Drive, but not in the external storage.
     * A synchronized block is needed because the content of the slideshow must be updated immediately with the new content.
     * Updating it without a synchronized block would lead to a race condition with [CustomSlideshow.resetIterator] or [CustomSlideshow.startSlideshow].
     * @param file The file to be downloaded.
     * @param fileType Enum that indicates whether the file to be downloaded is a video or an image.
     * @return A boolean indicating whether the file is deleted successfully or not.
     */
    fun downloadFileFromDrive(file: File, fileType: FileType) {
        when (fileType) {
            FileType.IMAGE -> {
                googleDriveClient?.downloadFile(
                    file.id,
                    "${fileManager.downloadsFolder}/${fileManager.rootFolder}/${fileManager.imagesFolder}/${file.name}"
                )
                synchronized(fileManager.alreadyDownloadedImages) {
                    fileManager.alreadyDownloadedImages.add(file.name)
                    fileManager.updateCustomSlideShow()
                }
            }

            FileType.VIDEO -> {
                googleDriveClient?.downloadFile(
                    file.id,
                    "${fileManager.downloadsFolder}/${fileManager.rootFolder}/${fileManager.videosFolder}/${file.name}"
                )
                synchronized(fileManager.alreadyDownloadedVideos) {
                    fileManager.alreadyDownloadedVideos.add(file.name)
                    fileManager.updateCustomSlideShow()
                }
            }
        }
    }
}
