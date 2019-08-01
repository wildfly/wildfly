package org.wildfly.extension.microprofile.health;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.smallrye.health.SmallRyeHealth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.wildfly.extension.microprofile.health._private.MicroProfileHealthLogger;


public class HealthReporter {

    public static final String DOWN = "DOWN";
    public static final String UP = "UP";
    private List<HealthCheck> healthChecks = new ArrayList<>();
    private List<HealthCheck> livenessChecks = new ArrayList<>();
    private List<HealthCheck> serverReadinessChecks = new ArrayList<>();
    private List<HealthCheck> readinessChecks = new ArrayList<>();

    private final String emptyLivenessChecksStatus;
    private final String emptyReadinessChecksStatus;

    public HealthReporter(String emptyLivenessChecksStatus, String emptyReadinessChecksStatus) {
        this.emptyLivenessChecksStatus = emptyLivenessChecksStatus;
        this.emptyReadinessChecksStatus = emptyReadinessChecksStatus;
    }

    public SmallRyeHealth getHealth() {
        String emptyChecksStatus = emptyLivenessChecksStatus.equals(DOWN) || emptyReadinessChecksStatus.equals(DOWN) ? DOWN : UP;
        return getHealth(emptyChecksStatus, serverReadinessChecks, healthChecks, livenessChecks, readinessChecks);
    }

    public SmallRyeHealth getLiveness() {
        return getHealth(emptyLivenessChecksStatus, Collections.emptyList(), livenessChecks);
    }

    public SmallRyeHealth getReadiness() {
        return getHealth(emptyReadinessChecksStatus, serverReadinessChecks, readinessChecks);
    }


    @SafeVarargs
    private final SmallRyeHealth getHealth(String emptyChecksStatus, List<HealthCheck> serverChecks, List<HealthCheck>... deploymentChecks) {
        JsonArrayBuilder results = Json.createArrayBuilder();
        HealthCheckResponse.State status = HealthCheckResponse.State.UP;

        if (serverChecks != null) {
            status = processChecks(serverChecks, results, status);
        }
        boolean emptyDeploymentChecks = true;
        for (List<HealthCheck> instance : deploymentChecks) {
            if (!instance.isEmpty()) {
                emptyDeploymentChecks = false;
            }
            status = processChecks(instance, results, status);
        }

        // if there were no deployment checks
        if (emptyDeploymentChecks && System.getProperty("__MP_HEALTH_TCK_DISABLE_SERVER_CHECKS") == null) {
            status = processChecks(Collections.singletonList(new HealthCheck() {
                @Override
                public HealthCheckResponse call() {
                    return HealthCheckResponse.named("empty-deployment-readiness-checks")
                            .state(emptyChecksStatus.equals("UP"))
                            .build();
                }
            }), results, status);
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();

        JsonArray checkResults = results.build();

        builder.add("status", status.toString());
        builder.add("checks", checkResults);

        return new SmallRyeHealth(builder.build());
    }

    private HealthCheckResponse.State processChecks(Iterable<HealthCheck> checks, JsonArrayBuilder results, HealthCheckResponse.State status) {
        if (checks != null) {
            for (HealthCheck check : checks) {
                status = fillCheck(check, results, status);
            }
        }

        return status;
    }

    private HealthCheckResponse.State fillCheck(HealthCheck check, JsonArrayBuilder results, HealthCheckResponse.State globalOutcome) {
        if (check == null) {
            return globalOutcome;
        }
        JsonObject each = jsonObject(check);
        results.add(each);
        if (globalOutcome == HealthCheckResponse.State.UP) {
            String status = each.getString("status");
            if (status.equals(DOWN)) {
                return HealthCheckResponse.State.DOWN;
            }
        }
        return globalOutcome;
    }

    private JsonObject jsonObject(HealthCheck check) {
        try {
            return jsonObject(check.call());
        } catch (RuntimeException e) {
            // Log Stacktrace to server log so an error is not just in Health Check response
            MicroProfileHealthLogger.LOGGER.error("Error processing Health Checks", e);

            HealthCheckResponseBuilder response = HealthCheckResponse.named(check.getClass().getName()).down();

            return jsonObject(response.build());
        }
    }

    private JsonObject jsonObject(HealthCheckResponse response) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("name", response.getName());
        builder.add("status", response.getState().toString());
        response.getData().ifPresent(d -> {
            JsonObjectBuilder data = Json.createObjectBuilder();
            for (Map.Entry<String, Object> entry : d.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    data.add(entry.getKey(), (String) value);
                } else if (value instanceof Long) {
                    data.add(entry.getKey(), (Long) value);
                } else if (value instanceof Boolean) {
                    data.add(entry.getKey(), (Boolean) value);
                }
            }
            builder.add("data", data.build());
        });

        return builder.build();
    }

    public void addHealthCheck(HealthCheck check) {
        if (check != null) {
            healthChecks.add(check);
        }
    }

    public void removeHealthCheck(HealthCheck check) {
        healthChecks.remove(check);
    }

    public void addReadinessCheck(HealthCheck check) {
        if (check != null) {
            readinessChecks.add(check);
        }
    }

    public void addServerReadinessCheck(HealthCheck check) {
        if (check != null) {
            serverReadinessChecks.add(check);
        }
    }

    public void removeReadinessCheck(HealthCheck check) {
        readinessChecks.remove(check);
    }

    public void addLivenessCheck(HealthCheck check) {
        if (check != null) {
            livenessChecks.add(check);
        }
    }

    public void removeLivenessCheck(HealthCheck check) {
        livenessChecks.remove(check);
    }
}
