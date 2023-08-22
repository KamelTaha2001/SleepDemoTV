import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import mobile.computing.tvsleepdemo.FileType
import java.io.FileOutputStream

/**
 * This class is used to download files from Google Drive using this [drive] instance.
 */
class GoogleDriveClient {

    private var drive: Drive? = null

    fun getDrive(): Drive? = drive


    /**
     * Builds an instance of Google Drive using [credential].
     * @return An instance of Google Drive
     */
    fun retrieveDrive(credential: GoogleAccountCredential?, appName: String) {
        drive = Drive
            .Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName(appName)
            .build()
    }

    /**
     * Retrieves all the files inside the folder [folder] on Google Drive.
     * Does not retrieve the folders in the trash.
     * @param folder The folder to get the files from.
     * @param fileType Enum that indicates whether the file to be retrieved is a video or an image.
     * @return A list containing all the files inside the folder [folder].
     */
    fun getAllFilesInFolder(folder: File, fileType: FileType): List<File>? {
        if (folder.isNotEmpty()) {
            var linkQuery = "thumbnailLink"
            val filesQuery = when (fileType) {
                FileType.IMAGE -> "'${folder.id}' in parents and trashed = false and mimeType contains 'image/'"
                FileType.VIDEO -> {
                    linkQuery = "webContentLink"
                    "'${folder.id}' in parents and trashed = false and mimeType contains 'video/'"
                }
            }
            return drive?.files()?.list()
                ?.setQ(filesQuery)
                ?.setFields("files(id, name, $linkQuery)")
                ?.execute()?.files
        }
        return listOf()
    }

    /**
     * Retrieves all the folders with the name of [folderName] on Google Drive.
     * Does not retrieve the folders in the trash.
     * @param folderName The folder name to search for.
     * @return A list containing all the folders with the name of [folderName].
     */
    fun getAllFoldersWithName(folderName: String): FileList? {
        val folderQuery =
            "mimeType='application/vnd.google-apps.folder' and trashed = false and name='$folderName'"
        return drive?.files()?.list()
            ?.setQ(folderQuery)
            ?.setFields("files(id, name)")
            ?.execute()
    }

    /**
     * Retrieves all the files with the name of [fileName] on Google Drive that are inside the folder with the id of [folderId].
     * Does not retrieve the files in the trash.
     * @param folderId The id of the folder to search in.
     * @param fileName The name of the file to search for
     * @return A list containing all the files in the folder with the name of [fileName].
     */
    fun getAllFilesWithNameInFolder(folderId: String, fileName: String): FileList? {
        val fileQuery = "'$folderId' in parents and trashed = false and name='$fileName'"
        return drive?.files()?.list()
            ?.setQ(fileQuery)
            ?.setFields("files(id, name)")
            ?.execute()
    }

    /**
     * Downloads a file from Google Drive and saves it to external storage.
     * @param fileId The Google Drive id of the file.
     * @param filePathWithName The file path where the file should be stored in external storage (file name included).
     */
    fun downloadFile(fileId: String?, filePathWithName: String) {
        try {
            val outputStream = FileOutputStream(filePathWithName)
            drive?.let { it.files()[fileId].executeMediaAndDownloadTo(outputStream) }
        } catch (e: GoogleJsonResponseException) {
            System.err.println("Unable to move file: " + e.details)
            throw e
        }
    }

    /**
     * Updates an existing file on Google Drive.
     * @param fileId The id of the file to be updated.
     * @param mediaContent The new content of the file to be updated.
     */
    fun updateFile(fileId: String, mediaContent: ByteArrayContent) {
        drive?.files()?.update(fileId,null, mediaContent)
            ?.execute()
    }

    /**
     * Creates a file on Google Drive.
     * @param fileId The id of the newly created file.
     * @param fileMetadata The metadata of the newly created file.
     * @param mediaContent The content of the newly created file.
     */
    fun createFile(fileId: String, fileMetadata: File, mediaContent: ByteArrayContent) {
        drive?.files()?.create(fileMetadata, mediaContent)
            ?.setFields(fileId)
            ?.execute()
    }
}