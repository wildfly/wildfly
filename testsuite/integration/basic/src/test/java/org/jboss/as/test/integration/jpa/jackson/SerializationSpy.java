package org.jboss.as.test.integration.jpa.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A data type whose only purpose is to spy on the serializer/deserializer and store the resulting info so that we can check
 * that Jackson was indeed used for serialization/deserialization (and thus that the test is asserting what we think it is).
 */
public class SerializationSpy {

    // Default value needs to be non-null so that custom serializers/deserializers are called
    @JsonSerialize(using = JsonSerializerSpy.class)
    public String serializerInfo = "none";
    @JsonDeserialize(using = JsonDeserializerSpy.class)
    public String deserializerInfo = "none";

    public static class JsonSerializerSpy extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(getCallerClassName());
        }
    }

    public static class JsonDeserializerSpy extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return getCallerClassName();
        }
    }

    private static String getCallerClassName() {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 1 /* skip first frame */; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (!className.startsWith("org.jboss.") && !className.startsWith("org.wildfly.")) {
                return className;
            }
        }
        return null;
    }
}
