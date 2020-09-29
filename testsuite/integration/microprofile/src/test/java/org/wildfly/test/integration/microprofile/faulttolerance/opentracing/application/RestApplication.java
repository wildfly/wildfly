package org.wildfly.test.integration.microprofile.faulttolerance.opentracing.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/rest")
public class RestApplication extends Application {
}
