/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaWithSslSetupTask extends RunKafkaSetupTask {
    private final String SERVER_KEYSTORE =
            "src/test/resources/org/wildfly/test/integration/microprofile/reactive/messaging/kafka/server.keystore.p12";
    private final String KEYSTORE_PWD = "serverks";

    @Override
    protected WildFlyEmbeddedKafkaBroker augmentKafkaBroker(WildFlyEmbeddedKafkaBroker broker) {

        Path path = Paths.get(SERVER_KEYSTORE)
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(path)) {
            throw new IllegalStateException(path.toString());
        }

        // This sets up SSL on port 9092 and PLAINTEXT on 19092
        Map<String, String> properties = new HashMap<>();
        properties.put("listeners", "SSL://localhost:9092,PLAINTEXT://localhost:19092");
        properties.put("advertised.listeners", "SSL://localhost:9092,PLAINTEXT://localhost:19092");
        properties.put("security.inter.broker.protocol", "PLAINTEXT");
        // TODO load from resources folder
        properties.put("ssl.keystore.location", path.toString());
        properties.put("ssl.keystore.password", KEYSTORE_PWD);
        properties.put("ssl.key.password", KEYSTORE_PWD);
        properties.put("ssl.keystore.type", "PKCS12");
        properties.put("ssl.secure.random.implementation", "SHA1PRNG");
        broker.brokerProperties(properties);

        // Set the port to the PLAINTEXT one, as this is the one the AdminClient will use to create the topics
        broker.kafkaPorts(19092);

        return broker;
    }
}