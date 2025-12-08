package org.keon.book.adapter.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.keon.book.application.type.EpochSecond
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun epochSecondModule(): SimpleModule {
        val module = SimpleModule()
        module.addSerializer(EpochSecond::class.java, EpochSecondSerializer())
        module.addDeserializer(EpochSecond::class.java, EpochSecondDeserializer())
        return module
    }

    class EpochSecondSerializer : JsonSerializer<EpochSecond>() {
        override fun serialize(value: EpochSecond, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeNumber(value.value)
        }
    }

    class EpochSecondDeserializer : JsonDeserializer<EpochSecond>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EpochSecond {
            return EpochSecond(p.longValue)
        }
    }
}
