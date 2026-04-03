package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [Index(value = ["type"])]
)
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val address: String,
    val isActive: Boolean = true,
    val createdAt: Long
)
