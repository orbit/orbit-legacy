/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test.stage

import cloud.orbit.common.util.RandomUtils
import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.ExecutionStrategy
import cloud.orbit.core.hosting.RandomRouting
import cloud.orbit.core.key.Key
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.createProxy
import cloud.orbit.runtime.serialization.kryo.DEFAULT_KRYO_BUFFER_SIZE
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.util.*

@Routing(true, true, RandomRouting::class, true)
interface SerializationTestAddressable : Addressable {
    fun doNothing(): Deferred<Unit>
    fun echoString(value: String): Deferred<String>
    fun echoInt(value: Int): Deferred<Int>
    fun echoGuid(value: UUID): Deferred<UUID>
    fun echoList(value: List<String>): Deferred<List<String>>
    fun echoClass(value: Class<out Any>): Deferred<Class<out Any>>
    fun echoMethod(value: Method): Deferred<Method>
}

@ExecutionModel(ExecutionStrategy.SAFE)
@Lifecycle(true, true)
@Suppress("UNUSED")
class SerializationTestAddressableImpl : SerializationTestAddressable {
    override fun doNothing() = CompletableDeferred(Unit)
    override fun echoString(value: String) = CompletableDeferred(value)
    override fun echoInt(value: Int) = CompletableDeferred(value)
    override fun echoGuid(value: UUID) = CompletableDeferred(value)
    override fun echoList(value: List<String>) = CompletableDeferred(value)
    override fun echoClass(value: Class<out Any>) = CompletableDeferred(value)
    override fun echoMethod(value: Method) = CompletableDeferred(value)


}

abstract class MessageSerializationTest : BaseStageTest() {
    @Test
    fun `ensure all key types succeed`() {
        val echoMsg = "Hello"
        val noKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        val intKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.Int32Key(1234))
        val longKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.Int64Key(5432))
        val stringKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.StringKey("MyKey"))
        val guidKey =
            stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.GuidKey(UUID.randomUUID()))

        runBlocking {
            noKey.echoString(echoMsg).await()
            intKey.echoString(echoMsg).await()
            longKey.echoString(echoMsg).await()
            stringKey.echoString(echoMsg).await()
            guidKey.echoString(echoMsg).await()
        }
    }

    @Test
    fun `ensure multiple basic arg types succeed`() {
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        runBlocking {
            val nothingVal = Unit
            val nothingRes = echo.doNothing().await()
            assertThat(nothingRes).isEqualTo(nothingVal)

            val strVal = "Hola"
            val strRes = echo.echoString(strVal).await()
            assertThat(strRes).isEqualTo(strVal)

            val intVal = 123253
            val intRes = echo.echoInt(intVal).await()
            assertThat(intRes).isEqualTo(intVal)

            val uuidVal = UUID.randomUUID()
            val uuidRes = echo.echoGuid(uuidVal).await()
            assertThat(uuidRes).isEqualTo(uuidVal)

            val listVal = listOf("John", "Ringo", "Paul", "George")
            val listRes = echo.echoList(listVal).await()
            assertThat(listRes).isEqualTo(listVal)

            val classVal = StageConfig::class.java
            val classRes = echo.echoClass(classVal).await()
            assertThat(classRes).isEqualTo(classVal)

            val methodVal = SerializationTestAddressable::class.java.getDeclaredMethod("doNothing")
            val methodRes = echo.echoMethod(methodVal).await()
            assertThat(methodVal.name).isEqualTo(methodRes.name)
        }
    }

    @Test
    fun `ensure copy really happens`() {
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        runBlocking {
            val listVal = listOf("John", "Ringo", "Paul", "George")
            val listRes = echo.echoList(listVal).await()
            assertThat(listRes).isNotSameAs(listVal)
            assertThat(listRes).isEqualTo(listVal)
        }
    }

    @Test
    fun `ensure too large message succeeds`() {
        val echoMsg = RandomUtils.pseudoRandomString(DEFAULT_KRYO_BUFFER_SIZE * 2)
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        val result = runBlocking {
            echo.echoString(echoMsg).await()
        }
        Assertions.assertThat(result).isEqualTo(echoMsg)
    }
}

class RawMessageSerializationTest : MessageSerializationTest() {
    override fun setupStage(stageConfig: StageConfig): StageConfig {
        return stageConfig.copy(
            allowLoopback = false
        )
    }
}

class CloneMessageSerializationTest : MessageSerializationTest() {
    override fun setupStage(stageConfig: StageConfig): StageConfig {
        return stageConfig.copy(
            allowLoopback = true
        )
    }
}