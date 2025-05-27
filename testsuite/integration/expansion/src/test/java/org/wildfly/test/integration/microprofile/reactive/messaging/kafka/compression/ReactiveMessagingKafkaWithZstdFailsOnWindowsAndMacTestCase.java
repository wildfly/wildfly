/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.compression;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@DockerRequired
public class ReactiveMessagingKafkaWithZstdFailsOnWindowsAndMacTestCase extends AbstractReactiveMessagingKafkaWithNativeCompressionFailsOnWindowsAndMacTestCase {
    public ReactiveMessagingKafkaWithZstdFailsOnWindowsAndMacTestCase() {
        super("microprofile-config-zstd.properties");
    }
}
