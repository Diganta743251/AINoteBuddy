package com.ainotebuddy.app.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    
    // String List converters (nullable-safe)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }
    }

    // Long List converters (nullable-safe)
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.let {
            val listType = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(it, listType)
        }
    }
    
    // Map<String, Any> converters (nullable-safe)
    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(it, mapType)
        }
    }
    
    // Map<String, String> converters (nullable-safe)
    @TypeConverter
    fun fromStringToStringMap(value: Map<String, String>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringToStringMap(value: String?): Map<String, String>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, mapType)
        }
    }
    
    // Boolean <-> Int converters
    @TypeConverter
    fun fromBoolean(value: Boolean): Int = if (value) 1 else 0
    
    @TypeConverter
    fun toBoolean(value: Int): Boolean = value == 1
}