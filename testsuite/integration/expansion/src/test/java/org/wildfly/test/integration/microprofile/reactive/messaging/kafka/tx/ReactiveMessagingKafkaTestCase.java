/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.tx;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@DockerRequired
public class ReactiveMessagingKafkaTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(25000);

    @Inject
    Bean bean;

    @Inject
    TransactionalBean txBean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaTestCase.class.getPackage())
                .addClasses(RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaTestCase.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml")
                .addAsWebInfResource(ReactiveMessagingKafkaTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
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
        // KK: Initially I thought we could do something similar here to in ReactiveMessagingKafkaSerializerTestCase
        // to check that messages are received in order on a partition but it is a bit complicated due to the
        // asynchronous storing of entries to a database
        Set<String> expected = new HashSet<>(Arrays.asList("hello", "reactive", "messaging"));
        Assert.assertEquals(expected.size(), bean.getWords().size());
        Assert.assertTrue("Expected " + bean.getWords() + " to contain all of " + expected, bean.getWords().containsAll(expected));

        // Check the data was stored
        txBean.checkValues(Collections.singleton("reactive"));
    }
}
