package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes ORDER BY isApplied DESC, createdAt DESC")
    fun observeAllThemes(): Flow<List<ThemeItem>>

    @Query("SELECT * FROM themes WHERE id = :id LIMIT 1")
    suspend fun getThemeById(id: Long): ThemeItem?

    @Query("SELECT * FROM themes WHERE fileName = :fileName LIMIT 1")
    suspend fun getThemeByFileName(fileName: String): ThemeItem?

    @Query("SELECT COUNT(*) FROM themes")
    suspend fun countThemes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(themes: List<ThemeItem>)

    @Update
    suspend fun updateTheme(theme: ThemeItem)

    @Query("UPDATE themes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE themes SET isInstalledToHuawei = :installed, localFilePath = COALESCE(:localFilePath, localFilePath), fileSizeBytes = CASE WHEN :size > 0 THEN :size ELSE fileSizeBytes END WHERE id = :id")
    suspend fun updateInstallStatus(id: Long, installed: Boolean, localFilePath: String?, size: Long)

    @Query("UPDATE themes SET isApplied = 0")
    suspend fun clearAllApplied()

    @Query("UPDATE themes SET isApplied = 1 WHERE id = :id")
    suspend fun markThemeApplied(id: Long)

    @Query("DELETE FROM themes WHERE id = :id")
    suspend fun deleteThemeById(id: Long)
}
