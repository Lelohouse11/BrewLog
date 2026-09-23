package com.example.brewlog.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testSerializationAndDeserialization() {
        val originalList = listOf("Chocolate", "Nutty, Caramel", "Fruity")
        val jsonString = converters.fromStringList(originalList)
        val deserializedList = converters.toStringList(jsonString)

        assertEquals(originalList, deserializedList)
    }

    @Test
    fun testLegacyCommaSeparatedFallback() {
        val legacyCsv = "Chocolate, Nutty, Berry"
        val parsedList = converters.toStringList(legacyCsv)

        assertEquals(listOf("Chocolate", "Nutty", "Berry"), parsedList)
    }

    @Test
    fun testEmptyString() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyList<String>(), converters.toStringList("   "))
    }
}
