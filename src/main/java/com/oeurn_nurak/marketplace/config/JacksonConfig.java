package com.oeurn_nurak.marketplace.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalDateTime has no zone, so it is serialized as UTC with an explicit "Z".
 * Without the offset clients read the server's UTC wall clock as their own
 * local time, which made fresh notifications look 7 hours old in UTC+7.
 */
@Configuration
public class JacksonConfig {
    private static final DateTimeFormatter UTC =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'");

    @Bean
    Jackson2ObjectMapperBuilderCustomizer utcLocalDateTime() {
        return builder -> builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider serializers)
                    throws IOException {
                generator.writeString(value.format(UTC));
            }
        });
    }
}
