package org.wildfly.test.integration.microprofile.opentracing.application;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/opentracing")
public class OpenTracingApplication extends Application {
}
