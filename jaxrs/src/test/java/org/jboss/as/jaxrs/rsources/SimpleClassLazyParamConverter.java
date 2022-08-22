package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.ext.ParamConverter;

@ParamConverter.Lazy
public class SimpleClassLazyParamConverter implements ParamConverter<SimpleClass> {
    @Override
    public SimpleClass fromString(String value) {
        throw new RuntimeException("Force a failure");
    }

    @Override
    public String toString(SimpleClass value) {
        return value.toString();
    }
}
