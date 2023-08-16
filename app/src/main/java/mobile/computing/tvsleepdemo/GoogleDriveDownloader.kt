import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.Drive
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class GoogleDriveDownloader(private val drive: Drive?) {
    fun downloadFile(fileId: String?): ByteArrayOutputStream {
        return try {
            val outputStream: OutputStream = ByteArrayOutputStream()
            drive?.let { drive.files()[fileId].executeMediaAndDownloadTo(outputStream) }
            outputStream as ByteArrayOutputStream
        } catch (e: GoogleJsonResponseException) {
            System.err.println("Unable to move file: " + e.details)
            throw e
        }
    }
}