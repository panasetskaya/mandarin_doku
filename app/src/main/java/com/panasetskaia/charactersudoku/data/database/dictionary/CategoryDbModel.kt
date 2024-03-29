package com.panasetskaia.charactersudoku.data.database.dictionary

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CategoryDbModel(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    val categoryName: String
)
