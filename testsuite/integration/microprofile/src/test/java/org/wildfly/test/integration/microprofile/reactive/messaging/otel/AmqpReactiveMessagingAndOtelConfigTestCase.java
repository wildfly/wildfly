/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SnapshotRestoreSetupTask.class, EnableReactiveExtensionsSetupTask.class})
public class AmqpReactiveMessagingAndOtelConfigTestCase extends BaseReactiveMessagingAndOtelConfigTest {

    public AmqpReactiveMessagingAndOtelConfigTestCase() {
        super("mp.messaging.connector.smallrye-amqp.tracing-enabled", "amqp-connector");
    }

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return createDeployment("mp-rm-amqp-otel-config.war");
    }
}
