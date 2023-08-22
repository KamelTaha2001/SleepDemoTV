package mobile.computing.tvsleepdemo

import android.service.dreams.DreamService
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @property ivImage The image view where the images of the slideshow will be displayed
 * @property vvVideo The video view where the videos of the slideshow will be displayed
 * @property globalScopeJob A reference to the coroutine job that is launched when the screensaver starts. It get cancelled at [ScreensaverService.onDetachedFromWindow]
 */
class ScreensaverService : DreamService() {
    private lateinit var ivImage: ImageView
    private lateinit var ivLeadingLogo: ImageView
    private lateinit var vvVideo: VideoView
    private lateinit var tvMessage: TextView
    private var globalScopeJob: Job? = null
    private lateinit var fileManager: FileManager
    private lateinit var customSlideshow: CustomSlideshow
    private lateinit var repository: Repository

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Beginning of Initialization
        globalScopeJob = GlobalScope.launch {
            withContext(Dispatchers.Main) {
                initializeUI()
            }
            customSlideshow = CustomSlideshow(ivImage,ivLeadingLogo, vvVideo)
            fileManager = FileManager(customSlideshow)
            repository = Repository(this@ScreensaverService, fileManager)
            val signInSuccess = repository.setupDrive()
            if (!signInSuccess) setErrorMessage(this@ScreensaverService.getString(R.string.service_sign_in_error))
            repository.retrieveAllData()
            // End of Initialization

            decideDownloadOrDeleteFiles(repository.imagesOnDriveFlow, FileType.IMAGE)
            decideDownloadOrDeleteFiles(repository.videosOnDriveFlow, FileType.VIDEO)

            collectSettingsAndStartSlideshow()
        }
    }

    /**
     * Initializes the UI.
     */
    private fun initializeUI() {
        isInteractive = false
        setContentView(R.layout.service_screensaver)
        ivImage = findViewById(R.id.ivImage)
        ivLeadingLogo = findViewById(R.id.ivLeadingLogo)
        vvVideo = findViewById(R.id.vvVideo)
        tvMessage = findViewById(R.id.tvMessage)
    }

    private fun setErrorMessage(message: String) {
        tvMessage.text = message
    }

    /**
    - Collects [Repository.appSettingsFlow].
    - Assigns the new settings to [customSlideshow] and starts it.
     */
    private suspend fun collectSettingsAndStartSlideshow() {
        repository.appSettingsFlow.collect { settings ->
            settings?.let {
                customSlideshow.setupAppSettings(settings)
            }
            customSlideshow.startSlideshow()
            withContext(Dispatchers.Main) {
                ivLeadingLogo.setImageDrawable(null)
            }
        }
    }

    /**
     * Synchronizes the files between External Storage and Google Drive.
     * @param flow List of files to be collected from Google Drive.
     * @param fileType Enum that indicates whether the file to be processed is a video or an image.
     */
    private fun decideDownloadOrDeleteFiles(
        flow: MutableSharedFlow<List<File>>,
        fileType: FileType
    ) {
        GlobalScope.launch {
            val alreadyDownloadedFiles = when (fileType) {
                FileType.IMAGE -> fileManager.alreadyDownloadedImages
                FileType.VIDEO -> fileManager.alreadyDownloadedVideos
            }
            flow.collect { files ->
                for (file in files) {
                    if (!alreadyDownloadedFiles.contains(file.name)) {    // If the file exists on Google Drive, but not in external storage
                        repository.downloadFileFromDrive(file, fileType)
                    }
                }
                val driveFilesNames = mutableListOf<String>()
                driveFilesNames.addAll(getFilesNames(files))
                for (file in alreadyDownloadedFiles) {    // If the file exists in the external storage, but not on Google Drive
                    if (!driveFilesNames.contains(file)) {
                        fileManager.deleteFile(fileType, file)
                    }
                }
            }
        }
    }

    /**
     * Extracts the names of files from a list of [File].
     * @param files The list of files.
     * @return A [MutableList] of type [String] containing the files names.
     */
    private fun getFilesNames(
        files: List<File>,
    ): MutableList<String> {
        val filesNames = mutableListOf<String>()
        files.forEach { file ->
            filesNames.add(file.name)
        }
        return filesNames
    }

    /**
     * Cancels [globalScopeJob] and stops [customSlideshow].
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        globalScopeJob?.cancel()
        customSlideshow.stopSlideshow()
    }
}