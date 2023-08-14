package mobile.computing.tvsleepdemo

import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApi {
    @GET("videos")
    suspend fun getVideos(
        @Query("key") key: String
    ) : VideoMedia
}