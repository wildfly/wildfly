/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.ConfigureElytronSslContextSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RunKafkaWithSslSetupTask.class, EnableReactiveExtensionsSetupTask.class, ConfigureElytronSslContextSetupTask.class})
@TestcontainersRequired
public class ReactiveMessagingKafkaSslConfiguredOnConnectionTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    Bean bean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaSslConfiguredOnConnectionTestCase.class.getPackage())
                .addClasses(RunKafkaWithSslSetupTask.class, EnableReactiveExtensionsSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaSslConfiguredOnConnectionTestCase.class.getPackage(), "microprofile-config-ssl-connection.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    public void test() throws InterruptedException {
        boolean wait = bean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Timed out", wait);
        Set<String> expected = new HashSet<>(Arrays.asList("hello", "reactive", "messaging", "ssl"));
        Assert.assertEquals(expected.size(), bean.getWords().size());
        Assert.assertTrue("Expected " + bean.getWords() + " to contain all of " + expected, bean.getWords().containsAll(expected));
    }
}
