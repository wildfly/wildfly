/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.serializer;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@TestcontainersRequired
public class ReactiveMessagingKafkaSerializerTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    Bean bean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaSerializerTestCase.class.getPackage())
                .addClasses(RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaSerializerTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    public void test() throws InterruptedException {
        boolean wait = bean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(wait, "Timed out");

        List<Person> list = bean.getReceived();
        Assertions.assertEquals(3, list.size());
        // Kafka messages only have order per partition, so do some massaging of the data
        Map<Integer, List<Person>> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            List<Person> persons = map.computeIfAbsent(bean.getPartitionReceived().get(i), ind -> new ArrayList<>());
            persons.add(list.get(i));

        }


        Person kabir = assertPersonNextOnAPartition(map, "Kabir");
        Person bob = assertPersonNextOnAPartition(map, "Bob");
        Person roger = assertPersonNextOnAPartition(map, "Roger");


        Assertions.assertEquals(101, kabir.getAge());
        Assertions.assertEquals(18, bob.getAge());
        Assertions.assertEquals(21, roger.getAge());
    }

    private Person assertPersonNextOnAPartition(Map<Integer, List<Person>> map, String name) {
        Person found = null;
        int remove = -1;
        for (Map.Entry<Integer, List<Person>> entry : map.entrySet()) {
            List<Person> persons = entry.getValue();
            Person p = persons.get(0);
            if (p.getName().equals(name)) {
                found = p;
                persons.remove(0);
                if (persons.size() == 0) {
                    remove = entry.getKey();
                }
            }
        }
        map.remove(remove);
        Assertions.assertNotNull(found, "Could not find " + name);
        return found;
    }

}
