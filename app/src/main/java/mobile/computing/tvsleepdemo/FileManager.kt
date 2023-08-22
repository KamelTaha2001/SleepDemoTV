package mobile.computing.tvsleepdemo

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Communicates with the external storage to store, delete, retrieve files.
 * @property downloadsFolder The path of the downloads folder in the external storage.
 * @property rootFolder The folder name -that is inside [downloadsFolder]- where the application files will be stored.
 * @property videosFolder The folder name inside [rootFolder] where the videos will be stored.
 * @property imagesFolder The folder name inside [rootFolder] where the images will be stored.
 * @property settingsFileName The file name inside [rootFolder] where the app settings will be stored.
 * @property alreadyDownloadedImages Stores all the already downloaded images on the device (gets updated when deleting an image or downloading one).
 * @property alreadyDownloadedVideos Stores all the already downloaded videos on the device (gets updated when deleting a video or downloading one).
 */
class FileManager(private val customSlideshow: CustomSlideshow?) {
    val downloadsFolder: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val rootFolder = "InternalMarketing"
    val videosFolder = "Videos"
    val imagesFolder = "Images"
    val settingsFileName = "app_settings_tv.json"
    var alreadyDownloadedVideos: MutableList<String> = getDownloadedFilesNames(FileType.VIDEO)
    var alreadyDownloadedImages: MutableList<String> = getDownloadedFilesNames(FileType.IMAGE)

    init {
        customSlideshow?.fileManager = this
        updateCustomSlideShow()
    }

    /**
     * Saves a file in the external storage depending on its type.
    - Video: stored inside the path [downloadsFolder]/[rootFolder]/[videosFolder]
    - Image: stored inside the path [downloadsFolder]/[rootFolder]/[imagesFolder]
     * @param byteArray The content of the file to be saved.
     * @param fileName The name of the file to be saved.
     * @param fileType Enum that indicates whether the file is a video or an image.
     - Example: If [fileName]="Ocean.mp4" and [fileType]=[FileType.VIDEO]. Then the file will be stored
     * inside this folder with this name:  /storage/emulated/0/Download/InternalMarketing/Videos/Ocean.mp4
     * @return A boolean indicating whether the file is saved successfully.
     */
    fun saveByteArrayToFile(byteArray: ByteArray, fileName: String, fileType: FileType): Boolean {
        val finalPath = when (fileType) {
            FileType.VIDEO -> "$downloadsFolder/$rootFolder/$videosFolder"
            FileType.IMAGE -> "$downloadsFolder/$rootFolder/$imagesFolder"
        }
        val outputPath = File(finalPath)
        if (!outputPath.exists()) {
            outputPath.mkdirs()
        }

        val outputFile = File(finalPath, fileName)
        return try {
            val outputStream = FileOutputStream(outputFile)
            outputStream.write(byteArray)
            outputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Gets all the already downloaded files of type [FileType.VIDEO] or [FileType.IMAGE].
     * @param fileType Enum that indicates whether the files to be retrieved are videos or images.
     * @return A [MutableList] of type [String] containing all the already downloaded files of type [fileType].
     */
    private fun getDownloadedFilesNames(fileType: FileType): MutableList<String> {
        val folderPath = when (fileType) {
            FileType.VIDEO -> File(downloadsFolder, "$rootFolder/$videosFolder")
            FileType.IMAGE -> File(downloadsFolder, "$rootFolder/$imagesFolder")
        }

        val names: MutableList<String> = mutableListOf()
        if (folderPath.exists() && folderPath.isDirectory) {
            val filesInFolder = folderPath.listFiles()
            if (filesInFolder != null) {
                for (file in filesInFolder) {
                    names.add(file.name)
                }
            }
        }
        return names
    }

    /**
     * @param fileType Enum that indicates whether the path to be retrieved is related to videos or images.
     * @return The full path of the folder where images or videos are stored.
     */
    fun getFullPathOfFile(fileType: FileType): String {
        return when (fileType) {
            FileType.VIDEO -> File(downloadsFolder, "$rootFolder/$videosFolder").toString()
            FileType.IMAGE -> File(downloadsFolder, "$rootFolder/$imagesFolder").toString()
        }
    }

    /**
     * Deletes a specific file from the [rootFolder]. Used when a file is found in the external storage, but on Google Drive.
     * A synchronized block is needed because the content of the slideshow must be updated immediately with the new content.
     * Updating it without a synchronized block would lead to a race condition with [CustomSlideshow.resetIterator] or [CustomSlideshow.startSlideshow].
     * @param fileType Enum that indicates whether the file to be deleted is a video or an image.
     * @param fileName The name of the file to be deleted.
     * @return A boolean indicating whether the file is deleted successfully or not.
     */
    fun deleteFile(fileType: FileType, fileName: String): Boolean {
        when (fileType) {
            FileType.VIDEO -> {
                val file = File(downloadsFolder, "$rootFolder/$videosFolder/$fileName")
                if (file.exists()) {
                    val deleted = file.delete()
                    synchronized(alreadyDownloadedVideos) {
                        alreadyDownloadedVideos = getDownloadedFilesNames(FileType.VIDEO)
                        updateCustomSlideShow()
                    }
                    return deleted
                }
            }

            FileType.IMAGE -> {
                val file = File(downloadsFolder, "$rootFolder/$imagesFolder/$fileName")
                if (file.exists()) {
                    val deleted = file.delete()
                    synchronized(alreadyDownloadedImages) {
                        alreadyDownloadedImages = getDownloadedFilesNames(FileType.IMAGE)
                        updateCustomSlideShow()
                    }
                    return deleted
                }
            }
        }
        return false
    }

    /**
     * @return A boolean indicating whether there are any images or videos already stored in the external storage.
     */
    fun checkIfAnyFilesAvailableLocally(): Boolean {
        return getDownloadedFilesNames(FileType.VIDEO).size > 0 || getDownloadedFilesNames(FileType.IMAGE).size > 0
    }

    /**
     * Updates the iterators of the [customSlideshow].
     * Used when a file is deleted or downloaded successfully to update the content of the slideshow.
     */
    fun updateCustomSlideShow() {
        customSlideshow?.imagesListIterator = alreadyDownloadedImages.listIterator()
        customSlideshow?.videosListIterator = alreadyDownloadedVideos.listIterator()
    }
}
