/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AssumptionViolatedException;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;


@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({EnableReactiveExtensionsSetupTask.class, RunKafkaSetupTask.class, OpenTelemetrySetupTask.class})
@DockerRequired(AssumptionViolatedException.class)
public class KafkaReactiveMessagingAndOtelTestCase extends BaseReactiveMessagingAndOtelTest {
    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;


    public KafkaReactiveMessagingAndOtelTestCase() {
        super("mp.messaging.connector.smallrye-kafka.tracing-enabled", "kafka-connector");
    }

    @Deployment
    public static WebArchive createDeployment() {
        return createDeployment(
                "mp-rm-kafka-otel.war",
                "kafka-microprofile-config.properties");
    }

    @Override
    OpenTelemetryCollectorContainer getCollector() {
        return otelCollector;
    }
}
