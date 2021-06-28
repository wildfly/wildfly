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

package org.wildfly.test.integration.microprofile.reactive;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.core.BrokerAddress;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaSetupTask implements ServerSetupTask {
    EmbeddedKafkaBroker broker;
    Path kafkaDir;
    final boolean ipv6 = WildFlySecurityManager.doChecked(
            (PrivilegedAction<Boolean>) () -> System.getProperties().containsKey("ipv6"));
    final String LOOPBACK = ipv6 ? "[::1]" : "127.0.0.1";

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        Path target = Paths.get("target").toAbsolutePath().normalize();
        kafkaDir = Files.createTempDirectory(target, "kafka").toAbsolutePath();

        Files.createDirectories(kafkaDir);

        broker = new WildFlyEmbeddedKafkaBroker(1, true, getPartitions(), getTopics())
                .zkPort(2181)
                .kafkaPorts(9092)
                .brokerProperty("log.dir", kafkaDir.toString())
                .brokerProperty("offsets.topic.num.partitions", 5);
        broker.afterPropertiesSet();
    }

    protected String[] getTopics() {
        return new String[]{"testing"};
    }

    protected int getPartitions() {
        return 1;
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        try {
            if (broker != null) {
                broker.destroy();
            }
        } finally {
            try {
                System.out.println("======> Exists " + kafkaDir + ": " + Files.exists(kafkaDir));
                if (Files.exists(kafkaDir)) {
                    Files.walkFileTree(kafkaDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (!Files.isDirectory(file)) {
                                Files.delete(file);
                            }
                            return super.visitFile(file, attrs);
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return super.postVisitDirectory(dir, exc);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class WildFlyEmbeddedKafkaBroker extends EmbeddedKafkaBroker {
        final int count;
        private int[] kafkaPorts;

        public WildFlyEmbeddedKafkaBroker(int count) {
            this(count, false);
        }

        public WildFlyEmbeddedKafkaBroker(int count, boolean controlledShutdown, String... topics) {
            this(count, controlledShutdown, 2, topics);
        }

        public WildFlyEmbeddedKafkaBroker(int count, boolean controlledShutdown, int partitions, String... topics) {
            super(count, controlledShutdown, partitions, topics);
            this.count = count;
        }

        public EmbeddedKafkaBroker kafkaPorts(int... ports) {
            super.kafkaPorts(ports);
            // Override so we have our own copy
            this.kafkaPorts = Arrays.copyOf(ports, ports.length);
            return this;
        }

        public BrokerAddress[] getBrokerAddresses() {
            List<BrokerAddress> addresses = new ArrayList<BrokerAddress>();

            for (int kafkaPort : kafkaPorts) {
                addresses.add(new BrokerAddress(LOOPBACK, kafkaPort));
            }
            return addresses.toArray(new BrokerAddress[0]);
        }
    }
}