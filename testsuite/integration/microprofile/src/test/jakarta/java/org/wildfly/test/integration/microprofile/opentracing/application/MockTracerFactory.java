package org.wildfly.test.integration.microprofile.opentracing.application;

import io.opentracing.Tracer;
import io.smallrye.opentracing.contrib.resolver.TracerFactory;
import io.opentracing.mock.MockTracer;

public class MockTracerFactory implements TracerFactory {

//    @Override
    public Tracer getTracer() {
        return new MockTracer();
    }
}
