package org.wildfly.test.integration.observability.opentelemetry.nocdi;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("rest")
public class NoCdiApplication extends Application {
    // Left empty intentionally
}
