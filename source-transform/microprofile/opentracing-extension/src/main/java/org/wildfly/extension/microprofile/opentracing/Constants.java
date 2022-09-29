package org.wildfly.extension.microprofile.opentracing;

public class Constants {
    public static final String CONTRIB_PACKAGE = "io.smallrye.opentracing-contrib";
    public static final String INTERCEPTOR_PACKAGE = CONTRIB_PACKAGE;
    public static final String SPAN_FINISHING_FILTER = "io.smallrye.opentracing.contrib.jaxrs2.server.SpanFinishingFilter";
    public static final String TRACERRESOLVER_PACKAGE = CONTRIB_PACKAGE;
    public static final String TRACER_CLASS = "io.smallrye.opentracing.contrib.resolver.TracerResolver";

    private Constants() {

    }
}
