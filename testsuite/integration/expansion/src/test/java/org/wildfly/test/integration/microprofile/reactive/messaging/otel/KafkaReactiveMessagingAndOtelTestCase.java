/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;


@ExtendWith(ArquillianExtension.class)
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, EnableReactiveExtensionsSetupTask.class, RunKafkaSetupTask.class})
@TestcontainersRequired
public class KafkaReactiveMessagingAndOtelTestCase extends BaseReactiveMessagingAndOtelTest {

    @BeforeAll
    public static void ignore() {
        IntermittentFailure.thisTestIsFailingIntermittently("https://issues.redhat.com/browse/WFLY-21114");
    }

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;


    public KafkaReactiveMessagingAndOtelTestCase() {
        super("kafka");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return BaseReactiveMessagingAndOtelTest.createDeployment(
                "mp-rm-kafka-otel.war",
                "kafka-microprofile-config.properties");
    }

    @Override
    OpenTelemetryCollectorContainer getCollector() {
        return otelCollector;
    }
}
