package com.jetdrone.vertx.yoke.core;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jetdrone.vertx.yoke.core.impl.ThreadLocalUTCDateFormat;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.EncodeException;
import org.vertx.java.core.json.impl.Base64;

import java.io.IOException;
import java.util.Date;

public final class JSON {

    private JSON() {}

    // date formatter
    public static final ThreadLocalUTCDateFormat DATE_FORMAT = new ThreadLocalUTCDateFormat();

    // create ObjectMapper instance
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // extensions
    private static final SimpleModule ECMA_COMPAT;

    static {
        // Do not crash if more data than the expected is received (should not happen since we have maps and list)
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Non-standard JSON but we allow C style comments in our JSON (Vert.x default to true, so keep it for compatibility)
        MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        // custom serializers
        ECMA_COMPAT = new SimpleModule("ECMA+Custom Compat Layer");
        // serialize Dates as per ECMAScript SPEC
        ECMA_COMPAT.addSerializer(Date.class, new JsonSerializer<Date>() {
            @Override
            public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                if (value == null) {
                    jgen.writeNull();
                } else {
                    jgen.writeString(DATE_FORMAT.format(value));
                }
            }
        });

        // serialize byte[] as Base64 Strings (same as used to be with Vert.x Json[Object|Array])
        ECMA_COMPAT.addSerializer(byte[].class, new JsonSerializer<byte[]>() {
            @Override
            public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                if (value == null) {
                    jgen.writeNull();
                } else {
                    jgen.writeString(Base64.encodeBytes(value));
                }
            }
        });

        MAPPER.registerModule(ECMA_COMPAT);
    }

    public static <T> void addSerializer(Class<? extends T> clazz, final JsonSerializer<T> serializer) {
        // Serialize Custom Types
        ECMA_COMPAT.addSerializer(clazz, serializer);
    }

    public static <T> void addDeserializer(Class<T> clazz, final JsonDeserializer<? extends T> deserializer) {
        // Serialize Custom Types
        ECMA_COMPAT.addDeserializer(clazz, deserializer);
    }

    public static String encode(Object item) {
        try {
            return MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new EncodeException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static <R> R decode(String source) {
        if (source == null) {
            return null;
        }

        try {
            // Untyped List/Map
            return (R) MAPPER.readValue(source, Object.class);
        } catch (IOException | RuntimeException e) {
            throw new DecodeException(e.getMessage());
        }
    }

    public static <R> R decode(String source, Class<R> clazz) {
        if (source == null) {
            return null;
        }

        try {
            return MAPPER.readValue(source, clazz);
        } catch (IOException | RuntimeException e) {
            throw new DecodeException(e.getMessage());
        }
    }
}