package org.wildfly.test.integration.observability.opentelemetry.nocdi;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("rest")
public class NoCdiApplication extends Application {
    // Left empty intentionally
}
