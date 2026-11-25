/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.opentelemetry.arquillian;

import static org.wildfly.testing.tools.deployments.DeploymentDescriptors.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.lang.management.ManagementPermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.PropertyPermission;
import java.util.Set;

import io.smallrye.opentelemetry.ExceptionMapper;
import io.smallrye.opentelemetry.TestConfigSource;
import jakarta.ws.rs.ext.Providers;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Pavol Loffay
 * @author Radoslav Husar
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar")
                    .addClasses(ExceptionMapper.class, TestConfigSource.class)
                    .addAsServiceProvider(ConfigSource.class, TestConfigSource.class)
                    .addAsServiceProvider(Providers.class, ExceptionMapper.class);

            WebArchive war = (WebArchive) applicationArchive;
            war
                    .addAsLibraries(extensionsJar)
                    .addAsManifestResource(new StringAsset("telemetry.tck.executor=telemetry.tck.executor=java.util.concurrent.ForkJoinPool"),
                            "microprofile-telemetry-tck.properties")
                    .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"/>"), "beans.xml");
        }

        if (applicationArchive instanceof ManifestContainer<?>) {
            ManifestContainer<?> manifestContainer = (ManifestContainer<?>) applicationArchive;

            Set<Permission> permissions = new HashSet<>(List.of(
                    new PropertyPermission("mptelemetry.tck.*", "read")
            ));

            // Set of test cases that are using awaitility.jar
            Set<String> awaitilityTests = Set.of(
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.LongCounterTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.DoubleUpDownCounterTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.DoubleCounterTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.LongUpDownCounterTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.DoubleGaugeTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.AsyncDoubleCounterTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.LongGaugeTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.DoubleHistogramTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.AsyncLongCounterTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.cdi.LongHistogramTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.jvm.JvmMemoryTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.jvm.JvmClassesTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.jvm.JvmThreadTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.jvm.JvmGarbageCollectionTest",
                    "org.eclipse.microprofile.telemetry.metrics.tck.jvm.JvmCpuTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestSpanTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestClientSpanDefaultTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.PropagatorSpiTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.async.JaxRsClientAsyncTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.spi.ResourceSpiTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestClientSpanDisabledTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.spi.CustomizerSpiTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.async.MpRestClientAsyncTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.spi.ExporterSpiTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.BaggageTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestSpanDefaultTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.W3BaggagePropagationTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.W3PropagationTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestSpanDisabledTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestClientSpanTest"
            );

            if (awaitilityTests.contains(testClass.getName())) {
                permissions.addAll(List.of(
                        // Permissions required by test instrumentation - awaitility.jar
                        new RuntimePermission("modifyThread"),
                        new RuntimePermission("setDefaultUncaughtExceptionHandler"),
                        new ManagementPermission("monitor")
                ));
            }

            if (testClass.getName().equals("org.eclipse.microprofile.telemetry.logs.tck.application.JulTest")
                    || awaitilityTests.contains(testClass.getName())) {
                permissions.add(
                        new FilePermission(System.getProperty("user.dir") + "/target/wildfly/standalone/log/server.log", "read")
                );
            }

            Set<String> httpClientTests = Set.of(
                    "org.eclipse.microprofile.telemetry.metrics.tck.application.http.HttpHistogramTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.async.JaxRsClientAsyncTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.async.MpRestClientAsyncTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestSpanDefaultTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestSpanDisabledTest",
                    "org.eclipse.microprofile.telemetry.tracing.tck.rest.RestSpanTest"
            );

            if (httpClientTests.contains(testClass.getName())) {
                // Permissions required by org.eclipse.microprofile.telemetry.metrics.tck.application.BasicHttpClient
                permissions.add(new SocketPermission("localhost:8080", "connect,resolve"));
            }

            // Run the TCK with security manager
            manifestContainer.addAsManifestResource(createPermissionsXmlAsset(permissions), "permissions.xml");
        }
    }
}
