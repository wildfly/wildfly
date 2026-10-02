/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.reactive.ConfigureElytronSslContextSetupTask;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup({RunKafkaWithSslSetupTask.class, EnableReactiveExtensionsSetupTask.class, ConfigureElytronSslContextSetupTask.class})
@TestcontainersRequired
public class ReactiveMessagingKafkaSslConfiguredGloballyTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    Bean bean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaSslConfiguredGloballyTestCase.class.getPackage())
                .addClasses(RunKafkaWithSslSetupTask.class, EnableReactiveExtensionsSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaSslConfiguredGloballyTestCase.class.getPackage(), "microprofile-config-ssl-global.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    void test() throws Exception {
        boolean wait = bean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertTrue(wait, "Timed out");
        Set<String> expected = new HashSet<>(Arrays.asList("hello", "reactive", "messaging", "ssl"));
        assertEquals(expected.size(), bean.getWords().size());
        assertTrue(bean.getWords().containsAll(expected), "Expected " + bean.getWords() + " to contain all of " + expected);
    }
}
