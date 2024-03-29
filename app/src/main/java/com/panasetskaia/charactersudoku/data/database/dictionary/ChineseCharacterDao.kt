package com.panasetskaia.charactersudoku.data.database.dictionary

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChineseCharacterDao {

    @Query("SELECT character FROM chinesecharacterdbmodel")
    suspend fun getAllChineseAsList(): List<String>

    @Query("SELECT * FROM chinesecharacterdbmodel")
    suspend fun getAllDictAsList(): List<ChineseCharacterDbModel>

    @Query("SELECT * FROM chinesecharacterdbmodel")
    fun getWholeDictionary(): Flow<List<ChineseCharacterDbModel>>

    @Query("DELETE FROM chinesecharacterdbmodel WHERE id=:characterId")
    suspend fun deleteCharFromDict(characterId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrEditCharacter(characterModel: ChineseCharacterDbModel)

    @Query("SELECT * FROM chinesecharacterdbmodel WHERE character=:characterStr")
    suspend fun getCharacterByChinese(characterStr: String): ChineseCharacterDbModel?

    @Query("SELECT * FROM chinesecharacterdbmodel WHERE isChosen=1")
    suspend fun getSelectedCharacters(): List<ChineseCharacterDbModel>

    @Query("SELECT * FROM categorydbmodel")
    fun getAllCategories(): Flow<List<CategoryDbModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrEditCategory(categoryDbModel: CategoryDbModel)

    @Query("DELETE FROM categorydbmodel WHERE categoryName=:catName")
    suspend fun deleteCategory(catName: String)

    @Query("SELECT EXISTS (SELECT 1 FROM categorydbmodel WHERE categoryName=:category)")
    suspend fun categoryExists(category: String): Boolean

    @Query("SELECT character FROM chinesecharacterdbmodel WHERE category=:category")
    suspend fun getChineseByCategory(category: String): List<String>
}
