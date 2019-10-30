package org.jboss.as.jaxrs.rsources;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class SimpleClassParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                                              Annotation[] annotations) {
        if (rawType.getName().equals(SimpleClass.class.getName())) {
            return new ParamConverter<T>() {
                @Override
                public T fromString(String value) {
                    throw new RuntimeException("Force a failure");
                }

                @Override
                public String toString(T value) {
                    return value.toString();
                }
            };
        }
        return null;
    }
}
