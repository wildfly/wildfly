package org.jboss.as.weld.ejb;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

/**
* @author Stuart Douglas
*/
public class WeldInterceptorInstances implements Serializable {
    private static final long serialVersionUID = 1L;
    private final CreationalContext<Object> creationalContext;
    private final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances;

    public WeldInterceptorInstances(final CreationalContext<Object> creationalContext, final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances) {
        this.creationalContext = creationalContext;
        this.interceptorInstances = interceptorInstances;
    }

    public CreationalContext<Object> getCreationalContext() {
        return creationalContext;
    }

    public Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> getInterceptorInstances() {
        return interceptorInstances;
    }
}
