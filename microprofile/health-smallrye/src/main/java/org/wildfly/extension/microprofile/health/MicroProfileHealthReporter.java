/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.microprofile.health;


import java.util.Collections;
import java.util.HashMap;
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


public class MicroProfileHealthReporter {

    public static final String DOWN = "DOWN";
    public static final String UP = "UP";
    private final boolean defaultServerProceduresDisabled;
    private Map<HealthCheck, ClassLoader> healthChecks = new HashMap<>();
    private Map<HealthCheck, ClassLoader> livenessChecks = new HashMap<>();
    private Map<HealthCheck, ClassLoader> readinessChecks = new HashMap<>();
    private Map<HealthCheck, ClassLoader> serverReadinessChecks = new HashMap<>();

    private final HealthCheck emptyDeploymentLivenessCheck;
    private final HealthCheck emptyDeploymentReadinessCheck;

    private static class EmptyDeploymentCheckStatus implements HealthCheck {
        private final String name;
        private final String status;

        EmptyDeploymentCheckStatus(String name, String status) {
            this.name = name;
            this.status = status;
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named(name)
                    .state(status.equals("UP"))
                    .build();
        }
    }


    public MicroProfileHealthReporter(String emptyLivenessChecksStatus, String emptyReadinessChecksStatus, boolean defaultServerProceduresDisabled) {
        this.emptyDeploymentLivenessCheck  = new EmptyDeploymentCheckStatus("empty-liveness-checks", emptyLivenessChecksStatus);
        this.emptyDeploymentReadinessCheck  = new EmptyDeploymentCheckStatus("empty-readiness-checks", emptyReadinessChecksStatus);
        this.defaultServerProceduresDisabled = defaultServerProceduresDisabled;
    }

    public SmallRyeHealth getHealth() {
        HashMap<HealthCheck, ClassLoader> deploymentChecks = new HashMap<>();
        deploymentChecks.putAll(healthChecks);
        deploymentChecks.putAll(livenessChecks);
        deploymentChecks.putAll(readinessChecks);

        HashMap<HealthCheck, ClassLoader> serverChecks= new HashMap<>();
        serverChecks.putAll(serverReadinessChecks);
        if (deploymentChecks.size() == 0 && !defaultServerProceduresDisabled) {
            serverChecks.put(emptyDeploymentLivenessCheck, Thread.currentThread().getContextClassLoader());
            serverChecks.put(emptyDeploymentReadinessCheck, Thread.currentThread().getContextClassLoader());
        }

        return getHealth(serverChecks, deploymentChecks);
    }

    public SmallRyeHealth getLiveness() {
        final Map<HealthCheck, ClassLoader> serverChecks;
        if (livenessChecks.size() == 0 && !defaultServerProceduresDisabled) {
            serverChecks = Collections.singletonMap(emptyDeploymentLivenessCheck, Thread.currentThread().getContextClassLoader());
        } else {
            serverChecks = Collections.emptyMap();
        }
        return getHealth(serverChecks, livenessChecks);
    }

    public SmallRyeHealth getReadiness() {
        final Map<HealthCheck, ClassLoader> serverChecks = new HashMap<>();
        serverChecks.putAll(serverReadinessChecks);
        if (readinessChecks.size() == 0 && !defaultServerProceduresDisabled) {
            serverChecks.put(emptyDeploymentReadinessCheck, Thread.currentThread().getContextClassLoader());
        }
        return getHealth(serverChecks, readinessChecks);
    }

    private final SmallRyeHealth getHealth(Map<HealthCheck, ClassLoader> serverChecks, Map<HealthCheck, ClassLoader> deploymentChecks) {
        JsonArrayBuilder results = Json.createArrayBuilder();
        HealthCheckResponse.State status = HealthCheckResponse.State.UP;

        status = processChecks(serverChecks, results, status);

        status = processChecks(deploymentChecks, results, status);

        JsonObjectBuilder builder = Json.createObjectBuilder();

        JsonArray checkResults = results.build();

        builder.add("status", status.toString());
        builder.add("checks", checkResults);

        return new SmallRyeHealth(builder.build());
    }

    private HealthCheckResponse.State processChecks(Map<HealthCheck, ClassLoader> checks, JsonArrayBuilder results, HealthCheckResponse.State status) {
        if (checks != null) {
            for (Map.Entry<HealthCheck, ClassLoader> entry : checks.entrySet()) {
                // use the classloader of the deployment's module instead of the TCCL (which is the server's ModuleClassLoader
                // to ensure that any resources that checks the TCCL (such as MP Config) will use the correct one
                // when the health checks are called.
                final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(entry.getValue());
                    status = fillCheck(entry.getKey(), results, status);
                } finally {
                    Thread.currentThread().setContextClassLoader(oldTCCL);
                }
            }
        }

        return status;
    }

    private HealthCheckResponse.State fillCheck(HealthCheck check, JsonArrayBuilder results, HealthCheckResponse.State globalOutcome) {
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

    public void addHealthCheck(HealthCheck check, ClassLoader moduleClassLoader) {
        if (check != null) {
            healthChecks.put(check, moduleClassLoader);
        }
    }

    public void removeHealthCheck(HealthCheck check) {
        healthChecks.remove(check);
    }

    public void addReadinessCheck(HealthCheck check, ClassLoader moduleClassLoader) {
        if (check != null) {
            readinessChecks.put(check, moduleClassLoader);
        }
    }

    public void addServerReadinessCheck(HealthCheck check, ClassLoader moduleClassLoader) {
        if (check != null) {
            serverReadinessChecks.put(check, moduleClassLoader);
        }
    }

    public void removeReadinessCheck(HealthCheck check) {
        readinessChecks.remove(check);
    }

    public void addLivenessCheck(HealthCheck check, ClassLoader moduleClassLoader) {
        if (check != null) {
            livenessChecks.put(check, moduleClassLoader);
        }
    }

    public void removeLivenessCheck(HealthCheck check) {
        livenessChecks.remove(check);
    }
}
