import android.os.Environment
import mobile.computing.tvsleepdemo.FileType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileManager {
    private val downloadsFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun saveByteArrayToFile(byteArray: ByteArray, fileName: String, fileType: FileType): Boolean {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val finalPath = when (fileType) {
            FileType.VIDEO -> "$downloadsFolder/InternalMarketing/Videos"
            FileType.IMAGE -> "$downloadsFolder/InternalMarketing/Images"
        }

        val outputFile = File(finalPath, fileName)
        val outputStream: FileOutputStream

        try {
            outputStream = FileOutputStream(outputFile)
            outputStream.write(byteArray)
            outputStream.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun getDownloadedFilesNames(fileType: FileType): MutableList<String> {
        val folderPath = when (fileType) {
            FileType.VIDEO -> File(downloadsFolder, "InternalMarketing/Videos")
            FileType.IMAGE -> File(downloadsFolder, "InternalMarketing/Images")
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

    fun getFullPathOfFile(fileType: FileType): String {
        return when (fileType) {
            FileType.VIDEO -> File(downloadsFolder, "InternalMarketing/Videos").toString()
            FileType.IMAGE -> File(downloadsFolder, "InternalMarketing/Images").toString()
        }
    }

    fun deleteFile(fileType: FileType, fileName: String): Boolean {
        val file = when (fileType) {
            FileType.VIDEO -> File(downloadsFolder, "InternalMarketing/Videos/$fileName")
            FileType.IMAGE -> File(downloadsFolder, "InternalMarketing/Images/$fileName")
        }
        if (file.exists()) {
            return file.delete()
        }
        return false
    }
}
