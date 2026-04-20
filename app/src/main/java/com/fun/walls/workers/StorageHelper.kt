package com.`fun`.walls.workers // FIX: Matched the package directive to your folder

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.`fun`.walls.models.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

object StorageHelper {

    private const val FOLDER_NAME = "FunThingWalls"

    suspend fun downloadAndSaveWallpaper(context: Context, wallpaperUrl: String, wallpaperId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context).data(wallpaperUrl).allowHardware(false).build()
                val result = loader.execute(request)
                val bitmap = if (result is SuccessResult) result.drawable.toBitmap() else return@withContext false

                val filename = "Wall_${wallpaperId}_${System.currentTimeMillis()}.jpg"

                // FIX: Removed the redundant SDK_INT checks. Modern MediaStore only!
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + FOLDER_NAME)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                // FIX: Removed the redundant initializer
                val fos: OutputStream? = imageUri?.let { resolver.openOutputStream(it) }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    true
                } ?: false

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun getDownloadedWallpapers(context: Context): List<Wallpaper> {
        return withContext(Dispatchers.IO) {
            val wallpapers = mutableListOf<Wallpaper>()
            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)

            // FIX: Removed the redundant SDK_INT checks. Modern Relative Path only!
            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%$FOLDER_NAME%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()

                    wallpapers.add(
                        Wallpaper(
                            id = id.toString(),
                            imageUrl = contentUri,
                            thumbnailUrl = contentUri,
                            source = "Gallery",
                            credit = "Downloaded Wallpaper"
                        )
                    )
                }
            }
            wallpapers
        }
    }
}