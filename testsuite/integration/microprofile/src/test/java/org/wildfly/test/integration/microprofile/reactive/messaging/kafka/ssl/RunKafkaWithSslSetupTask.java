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

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker;
import org.apache.kafka.common.Endpoint;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker.endpoint;
import static org.apache.kafka.common.security.auth.SecurityProtocol.PLAINTEXT;
import static org.apache.kafka.common.security.auth.SecurityProtocol.SSL;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaWithSslSetupTask implements ServerSetupTask {
    private static final String SERVER_KEYSTORE =
            "src/test/resources/org/wildfly/test/integration/microprofile/reactive/messaging/kafka/server.keystore.p12";
    private static final String KEYSTORE_PWD = "serverks";

    volatile EmbeddedKafkaBroker broker;
    volatile KafkaCompanion companion;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        try {
            Path keyStorePath = Paths.get(SERVER_KEYSTORE)
                    .toAbsolutePath()
                    .normalize();
            if (!Files.exists(keyStorePath)) {
                throw new IllegalStateException(keyStorePath.toString());
            }


            Endpoint external = endpoint("EXTERNAL", SSL, "localhost", 9092);
            Endpoint internal = endpoint("INTERNAL", PLAINTEXT, "localhost", 19002);
            broker = new EmbeddedKafkaBroker()
                    .withAdvertisedListeners(external, internal)
                    .withAdditionalProperties(properties -> {
                        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStorePath.toString());
                        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, KEYSTORE_PWD);
                        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, KEYSTORE_PWD);
                        properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
                        properties.put(SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG, "SHA1PRNG");
                    })
                    .withDeleteLogDirsOnClose(true);
            broker.start();

            companion = new KafkaCompanion(EmbeddedKafkaBroker.toListenerString(internal));
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } catch (Exception e) {
            if (companion != null) {
                companion.close();
            }
            if (broker != null) {
                broker.close();
            }
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        if (companion != null) {
            companion.close();
        }
        if (broker != null) {
            broker.close();
        }
    }
}