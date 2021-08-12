package org.wildfly.test.manual.microprofile.health;

import org.eclipse.microprofile.health.HealthCheckResponse;

public class InvalidHealthResponseException extends Exception {
    public InvalidHealthResponseException(HealthCheckResponse.Status expectedStatus, String receivedStatus) {
        super(String.format("Expected %s but received %s", expectedStatus, receivedStatus));
    }
}
