package org.wildfly.test.manual.microprofile.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

@Startup
public class SuccessfulStartupCheck implements HealthCheck {

    public static final String NAME = "SuccessfulStartupCheck";

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up(NAME);
    }
}
