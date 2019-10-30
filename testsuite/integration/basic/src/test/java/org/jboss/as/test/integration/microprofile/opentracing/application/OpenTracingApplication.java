package org.jboss.as.test.integration.microprofile.opentracing.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/opentracing")
public class OpenTracingApplication extends Application {
}
