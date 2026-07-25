package com.nexus.admin.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.admin.data.entity.*

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSaleProductList(value: List<SaleProduct>?): String {
        return gson.toJson(value ?: emptyList<SaleProduct>())
    }

    @TypeConverter
    fun toSaleProductList(value: String): List<SaleProduct> {
        val type = object : TypeToken<List<SaleProduct>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromPaymentList(value: List<Payment>?): String {
        return gson.toJson(value ?: emptyList<Payment>())
    }

    @TypeConverter
    fun toPaymentList(value: String): List<Payment> {
        val type = object : TypeToken<List<Payment>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromRestockProductList(value: List<RestockProduct>?): String {
        return gson.toJson(value ?: emptyList<RestockProduct>())
    }

    @TypeConverter
    fun toRestockProductList(value: String): List<RestockProduct> {
        val type = object : TypeToken<List<RestockProduct>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}