package org.jboss.as.test.integration.jpa.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A data type whose only purpose is to spy on the serializer/deserializer and store the resulting info so that we can check
 * that Jackson was indeed used for serialization/deserialization (and thus that the test is asserting what we think it is).
 */
public class SerializationSpy {

    // Default value needs to be non-null so that custom serializers/deserializers are called
    @XmlJavaTypeAdapter(XmlSerializerSpy.class)
    public String serializerInfo = "none";
    @XmlJavaTypeAdapter(XmlDeserializerSpy.class)
    public String deserializerInfo = "none";

    public static class XmlSerializerSpy extends XmlAdapter<String, String> {
        @Override
        public String unmarshal(String v) {
            return v;
        }

        @Override
        public String marshal(String v) {
            return getCallerClassName();
        }
    }

    public static class XmlDeserializerSpy extends XmlAdapter<String, String> {
        @Override
        public String unmarshal(String v) {
            return getCallerClassName();
        }

        @Override
        public String marshal(String v) {
            return v;
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
