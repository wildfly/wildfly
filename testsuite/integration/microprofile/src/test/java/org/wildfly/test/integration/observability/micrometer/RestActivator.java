package org.wildfly.test.integration.observability.micrometer;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class RestActivator extends Application {
}
