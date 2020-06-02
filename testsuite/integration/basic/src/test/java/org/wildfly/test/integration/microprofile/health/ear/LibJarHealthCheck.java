package org.wildfly.test.integration.microprofile.health.ear;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public class LibJarHealthCheck implements HealthCheck {

    @Inject
    private Config config;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named(getClass().getName())
                .withData(HealthTestConstants.PROPERTY_NAME_EJB_JAR, getValue(HealthTestConstants.PROPERTY_NAME_EJB_JAR))
                .withData(HealthTestConstants.PROPERTY_NAME_LIB_JAR, getValue(HealthTestConstants.PROPERTY_NAME_LIB_JAR))
                .withData(HealthTestConstants.PROPERTY_NAME_WAR1, getValue(HealthTestConstants.PROPERTY_NAME_WAR1))
                .withData(HealthTestConstants.PROPERTY_NAME_WAR2, getValue(HealthTestConstants.PROPERTY_NAME_WAR2))
                .up()
                .build();
    }

    private Boolean getValue(String name) {
        return config.getOptionalValue(name, Boolean.class).orElse(false);
    }
}
