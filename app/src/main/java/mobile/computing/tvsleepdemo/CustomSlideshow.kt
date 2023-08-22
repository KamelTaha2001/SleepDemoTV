package mobile.computing.tvsleepdemo

import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.VideoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This class is responsible for displaying images and videos in slideshow mode.
 * @property ivImage The image view where the image will be displayed.
 * @property vvVideo The video view where the video will be displayed.
 * @property globalSlideshowJob Refers to the created coroutine at [startSlideshow]. So we can cancel it when calling either [stopSlideshow] or [displayNextVideo].
 * @property imagesListIterator The iterator of the list containing the already stored images on the device.
 * @property videosListIterator The iterator of the list containing the already stored videos on the device.
 * @property displayImages A boolean to decide whether the slideshow should display images or not.
 * @property displayVideos A boolean to decide whether the slideshow should display videos or not.
 * @property imageDuration A single image in the slideshow is displayed for [imageDuration] milli seconds.
 */
class CustomSlideshow(
    private val ivImage: ImageView,
    private val ivLeadingLogo: ImageView,
    private val vvVideo: VideoView,
) {
    private var globalSlideshowJob: Job? = null
    lateinit var imagesListIterator: ListIterator<String>
    lateinit var videosListIterator: ListIterator<String>
    lateinit var fileManager: FileManager
    private var displayImages: Boolean = true
    private var displayVideos: Boolean = true
    private var imageDuration: Int = 10000

    /**
     - Starts a coroutine that displays an image for [imageDuration] milli seconds, then displays a video until it finishes.
     - The video's on complete listener calls this method again to repeat the process.
     - The method is using a coroutine because it needs to call a delay inside the [displayNextImage] method.
     */
    fun startSlideshow() {
        globalSlideshowJob = GlobalScope.launch(Dispatchers.Main) {
            if (displayImages) {
                if (!imagesListIterator.hasNext()) {
                    resetIterator(imagesListIterator)
                }
                if (imagesListIterator.hasNext()) // Check if the list is not empty
                    displayNextImage(imagesListIterator.next())
            }

            if (displayVideos) {
                if (!videosListIterator.hasNext()) {
                    resetIterator(videosListIterator)
                }
                if (videosListIterator.hasNext()) // Check if the list is not empty
                    displayNextVideo(videosListIterator.next())
            }
        }

    }

    /**
    - Gets the next image to display from the external storage.
     - Displays the image for [imageDuration] milli seconds.
     * @param imageName The name of the image to display (eg. ocean.jpg).
     */
    private suspend fun displayNextImage(imageName: String) {
        withContext(Dispatchers.Main) {
            val imageUri = fileManager.getFullPathOfFile(FileType.IMAGE) + "/$imageName"
            ivImage.setImageURI(Uri.parse(imageUri))
            delay(imageDuration.toLong())
        }
    }

    /**
    - Gets the next video to display from the external storage.
    - When the video is done, a new call to [startSlideshow] is fired to repeat the process of showing an image then a video. The old coroutine job is cancelled to avoid creating unuseful jobs.
     * @param videoName The name of the video to display (eg. ocean.mp4).
     */
    private fun displayNextVideo(videoName: String) {
        val videoUri = fileManager.getFullPathOfFile(FileType.VIDEO) + "/$videoName"
        vvVideo.setVideoURI(Uri.parse(videoUri))
        vvVideo.setOnPreparedListener {
            vvVideo.start()
        }
        vvVideo.setOnInfoListener { mp, what, extra ->
            ivImage.setImageDrawable(null)
            true
        }
        vvVideo.setOnCompletionListener {
            globalSlideshowJob?.cancel()
            startSlideshow()
        }
    }

    /**
    * Resets the iterator to the beginning of the list.
     * @param iterator The iterator to reset.
     */
    private fun resetIterator(iterator: ListIterator<String>) {
        while (iterator.hasPrevious()) {
            iterator.previous()
        }
    }

    /**
    * Stops the slideshow.
     * NOTE: If this is called when a video is playing. It will produce a bug because the video's on complete listener starts the slideshow again.
     */
    fun stopSlideshow() {
        globalSlideshowJob?.cancel()
    }

    /**
     * Sets settings like:
     - Whether to display image in the slideshow or not.
     - Whether to display videos in the slideshow or not.
     - Sets the duration for hor how long a single image will be displayed.
     */
    fun setupAppSettings(appSettings: AppSettings) {
        displayImages = appSettings.displayImages
        displayVideos = appSettings.displayVideos
        imageDuration = appSettings.imageDuration
    }

    /**
     * Displays the default image on the screen (Leading Point Logo).
     */
    fun displayDefaultImage() {
        ivImage.setImageResource(R.drawable.leadingpoint)
    }
}