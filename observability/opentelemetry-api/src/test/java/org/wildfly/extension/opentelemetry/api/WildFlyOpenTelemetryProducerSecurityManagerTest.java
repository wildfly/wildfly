/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_LOGS_EXPORTER;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_METRICS_EXPORTER;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SDK_DISABLED;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_TRACES_EXPORTER;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.wildfly.security.manager.WildFlySecurityManager;

@SuppressWarnings("removal")
public class WildFlyOpenTelemetryProducerSecurityManagerTest {

    @Test
    public void testInitializationWithRestrictedDeploymentPermissions() throws Exception {
        Assumptions.assumeTrue(Runtime.version().feature() < 24,
                "The Security Manager is permanently disabled starting with Java 24");

        SecurityManager originalSecurityManager = System.getSecurityManager();
        Policy originalPolicy = Policy.getPolicy();
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader deploymentClassLoader = new DeploymentClassLoader(originalContextClassLoader);
        URL testClasses = getClass().getProtectionDomain().getCodeSource().getLocation();
        WildFlyOpenTelemetryProducer producer = new WildFlyOpenTelemetryProducer();
        OpenTelemetry openTelemetry = null;

        GlobalOpenTelemetry.resetForTest();
        Class.forName(WildFlySecurityManager.class.getName());
        Thread.currentThread().setContextClassLoader(deploymentClassLoader);
        Policy.setPolicy(new RestrictedTestPolicy(testClasses));
        try {
            System.setSecurityManager(new SecurityManager());
            WildFlyOpenTelemetryConfig config = new WildFlyOpenTelemetryConfig(
                    Map.of(
                            OTEL_SDK_DISABLED, "false",
                            OTEL_TRACES_EXPORTER, "none",
                            OTEL_METRICS_EXPORTER, "none",
                            OTEL_LOGS_EXPORTER, "none"),
                    false,
                    List.of(),
                    true);

            openTelemetry = producer.getOpenTelemetry(config);

            assertNotNull(openTelemetry);
        } finally {
            System.setSecurityManager(originalSecurityManager);
            Policy.setPolicy(originalPolicy);
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            if (openTelemetry != null) {
                producer.close(openTelemetry);
            }
        }
    }

    private static final class DeploymentClassLoader extends ClassLoader {
        private final ClassLoader delegate;

        private DeploymentClassLoader(ClassLoader delegate) {
            super(null);
            this.delegate = delegate;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return delegate.loadClass(name);
        }

        @Override
        public URL getResource(String name) {
            return delegate.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return delegate.getResources(name);
        }
    }

    private static final class RestrictedTestPolicy extends Policy {
        private final URL testClasses;

        private RestrictedTestPolicy(URL testClasses) {
            this.testClasses = testClasses;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            CodeSource codeSource = domain.getCodeSource();
            if (codeSource == null || !testClasses.equals(codeSource.getLocation())) {
                return true;
            }
            return permission instanceof RuntimePermission && "setSecurityManager".equals(permission.getName());
        }
    }
}
