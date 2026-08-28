package com.example.data.database

import android.content.Context
import com.example.domain.model.Exercise
import com.squareup.moshi.Json
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source

object ExerciseSeeder {
    suspend fun seedExercises(context: Context, dao: PulseBreakDao) {
        withContext(Dispatchers.IO) {
            try {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(ExerciseData::class.java)
                val batch = ArrayList<Exercise>(100)

                context.assets.open("exercises.json").source().buffer().use { source ->
                    JsonReader.of(source).use { reader ->
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val data = adapter.fromJson(reader) ?: continue
                            batch += Exercise(
                                id = data.id,
                                name = data.name,
                                description = data.instructions["en"] ?: "",
                                category = data.bodyPart,
                                bodyPart = data.bodyPart,
                                equipment = data.equipment,
                                target = data.target,
                                gifUrl = "file:///android_asset/${data.gifUrl}"
                            )
                            if (batch.size == 100) {
                                dao.insertExercises(batch.toList())
                                batch.clear()
                            }
                        }
                        reader.endArray()
                    }
                }
                if (batch.isNotEmpty()) {
                    dao.insertExercises(batch)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class ExerciseData(
    val id: String,
    val name: String,
    @Json(name = "body_part")
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val instructions: Map<String, String>,
    @Json(name = "gif_url")
    val gifUrl: String
)
