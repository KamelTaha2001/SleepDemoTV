package mobile.computing.tvsleepdemo

import FileManager
import android.os.Environment
import android.service.dreams.DreamService
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ScreensaverService : DreamService() {
    private lateinit var ivImage: ImageView
    private lateinit var vvVideo: VideoView
    private var globalScopeJob: Job? = null
    private lateinit var alreadyDownloadedVideos: MutableList<String>
    private lateinit var alreadyDownloadedImages: MutableList<String>
    private lateinit var fileManager: FileManager
    private lateinit var customSlideshow: CustomSlideshow

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        globalScopeJob = GlobalScope.launch {
            withContext(Dispatchers.Main) {
                initializeUI()
                fileManager = FileManager()
                customSlideshow = CustomSlideshow(ivImage, vvVideo, fileManager)
            }
            val repository = Repository(this@ScreensaverService)
            repository.setupDriveAndGetUrls(ivImage, vvVideo)

            alreadyDownloadedVideos = fileManager.getDownloadedFilesNames(FileType.VIDEO)
            alreadyDownloadedImages = fileManager.getDownloadedFilesNames(FileType.IMAGE)

            val imagesDownloadTask = async {
                repository.imagesOnDrive.collect { images ->
                    for (image in images) {
                        if (!alreadyDownloadedImages.contains(image.name)) {
                            repository.downloadFileFromDrive(image, FileType.IMAGE)
                        }
                    }
                    val driveImagesNames: MutableList<String> = mutableListOf()
                    images.forEach { file ->
                        Log.d("MYTAG", file.name)
                        driveImagesNames.add(file.name)
                    }
                    for (image in alreadyDownloadedImages) {
                        if (!driveImagesNames.contains(image)) {
                            val deleted = fileManager.deleteFile(FileType.IMAGE, image)
                            Log.d("MYTAG", "$deleted")
                        }
                    }
                }
            }
            val videosDownloadTask = async {
                repository.videosOnDrive.collect { videos ->
                    for (video in videos) {
                        if (!alreadyDownloadedVideos.contains(video.name)) {
                            repository.downloadFileFromDrive(video, FileType.VIDEO)
                        }
                    }
                }
            }

            if (checkIfAnyFilesAvailableLocally()) {
                customSlideshow.startSlideshow()
            } else {
                imagesDownloadTask.await()

            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        globalScopeJob?.cancel()
        customSlideshow.stopSlideshow()
    }

    private fun initializeUI() {
        isInteractive = false
        setContentView(R.layout.activity_main)
        ivImage = findViewById(R.id.ivLeadingLogo)
        ivImage.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                R.drawable.leadingpoint
            )
        )
        vvVideo = findViewById(R.id.videoview)
    }

    private fun checkIfAnyFilesAvailableLocally(): Boolean {
        return alreadyDownloadedVideos.size > 0 || alreadyDownloadedImages.size > 0
    }
}