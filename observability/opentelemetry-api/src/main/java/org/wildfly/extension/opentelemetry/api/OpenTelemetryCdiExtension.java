package org.wildfly.extension.opentelemetry.api;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.wildfly.security.manager.WildFlySecurityManager;

public class OpenTelemetryCdiExtension implements Extension {
    private static final Map<ClassLoader, OpenTelemetry> OTEL_INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ClassLoader, Tracer> TRACERS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Class<?>[] BEANS_TO_ADD = {
            OpenTelemetryContainerFilter.class,
            OpenTelemetryRestClientProducer.class,
            OpenTelemetryClientRequestFilter.class
    };

    public static OpenTelemetry registerApplicationOpenTelemetryBean(ClassLoader classLoader, OpenTelemetry bean) {
        OTEL_INSTANCES.put(classLoader, bean);
        return bean;
    }

    public static Tracer registerApplicationTracer(ClassLoader classLoader, Tracer tracer) {
        TRACERS.put(classLoader, tracer);
        return tracer;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager beanManager) {
        for (Class<?> clazz : BEANS_TO_ADD) {
            bbd.addAnnotatedType(beanManager.createAnnotatedType(clazz), clazz.getName());
        }
    }

    public void registerOpenTelemetryBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        abd.addBean().addTransitiveTypeClosure(OpenTelemetry.class).produceWith(i ->
                OTEL_INSTANCES.get(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged()));
        abd.addBean().addTransitiveTypeClosure(Tracer.class).produceWith(i ->
                TRACERS.get(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged()));
    }

    public void beforeShutdown(@Observes final BeforeShutdown bs) {
        OTEL_INSTANCES.remove(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
        TRACERS.remove(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }
}
