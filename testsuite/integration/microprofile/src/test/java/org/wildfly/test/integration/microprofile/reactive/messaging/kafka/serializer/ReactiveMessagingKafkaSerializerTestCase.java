/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.serializer;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
        Assert.assertTrue("Timed out", wait);

        List<Person> list = bean.getReceived();
        Assert.assertEquals(3, list.size());
        // Kafka messages only have order per partition, so do some massaging of the data
        Map<Integer, List<Person>> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            List<Person> persons = map.computeIfAbsent(bean.getPartitionReceived().get(i), ind -> new ArrayList<>());
            persons.add(list.get(i));

        }


        Person kabir = assertPersonNextOnAPartition(map, "Kabir");
        Person bob = assertPersonNextOnAPartition(map, "Bob");
        Person roger = assertPersonNextOnAPartition(map, "Roger");


        Assert.assertEquals(101, kabir.getAge());
        Assert.assertEquals(18, bob.getAge());
        Assert.assertEquals(21, roger.getAge());
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
        Assert.assertNotNull("Could not find " + name, found);
        return found;
    }

}
