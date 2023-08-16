package mobile.computing.tvsleepdemo

import FileManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch

class CustomSlideshow(
    private val ivImage: ImageView,
    private val vvVideo: VideoView,
    private val fileManager: FileManager,
) {
    private var videosToPlay: MutableList<String> = mutableListOf()
    private var imagesToPlay: MutableList<String> = mutableListOf()
    private var globalSlideshowJob: Job? = null
    private var latch: CountDownLatch
    private var imagesListIterator: ListIterator<String>
    private var videosListIterator: ListIterator<String>

    init {
        videosToPlay = fileManager.getDownloadedFilesNames(FileType.VIDEO)
        imagesToPlay = fileManager.getDownloadedFilesNames(FileType.IMAGE)
        latch = CountDownLatch(1)
        imagesListIterator = imagesToPlay.listIterator()
        videosListIterator = videosToPlay.listIterator()
    }

    fun startSlideshow() {
        globalSlideshowJob = GlobalScope.launch(Dispatchers.Main) {

            if (imagesListIterator.hasNext()) {
                displayNextImage(imagesListIterator.next(), 5000)
            } else {
                resetIterator(imagesListIterator)
                displayNextImage(imagesListIterator.next(), 5000)
            }

            if (videosListIterator.hasNext()) {
                displayNextVideo(videosListIterator.next())
            } else {
                resetIterator(videosListIterator)
                displayNextVideo(videosListIterator.next())
            }
        }
    }

    private suspend fun displayNextImage(imageName: String, displayDuration: Int) {
        withContext(Dispatchers.Main) {
            val imageUri = fileManager.getFullPathOfFile(FileType.IMAGE) + "/$imageName"
            ivImage.setImageURI(Uri.parse(imageUri))
            delay(displayDuration.toLong())
            ivImage.setImageDrawable(null)
        }
    }

    private fun displayNextVideo(videoName: String) {
        ivImage.setImageDrawable(null)
        val videoUri = fileManager.getFullPathOfFile(FileType.VIDEO) + "/$videoName"

        vvVideo.setVideoURI(Uri.parse(videoUri))
        vvVideo.setOnPreparedListener {
            Log.d("MYTAG", "Video Start")
            vvVideo.start()
        }
        vvVideo.setOnCompletionListener {
            latch.countDown()
            Log.d("MYTAG", "Video End")
            globalSlideshowJob?.cancel()
            startSlideshow()
        }
    }

    private fun resetIterator(iterator: ListIterator<String>) {
        while (iterator.hasPrevious()) {
            iterator.previous()
        }
    }

    fun stopSlideshow() {
        globalSlideshowJob?.cancel()
    }
}