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
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunArtemisAmqpSetupTask;


@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, EnableReactiveExtensionsSetupTask.class, RunArtemisAmqpSetupTask.class})
@DockerRequired
public class AmqpReactiveMessagingAndOtelTestCase extends BaseReactiveMessagingAndOtelTest {
    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    public AmqpReactiveMessagingAndOtelTestCase() {
        super("amqp");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return BaseReactiveMessagingAndOtelTest.createDeployment(
                "mp-rm-amqp-otel.war",
                "amqp-microprofile-config.properties");
    }

    @Override
    OpenTelemetryCollectorContainer getCollector() {
        return otelCollector;
    }
}
