/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker;
import org.apache.kafka.common.Endpoint;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.wildfly.test.integration.microprofile.reactive.KeystoreUtil;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static io.smallrye.reactive.messaging.kafka.companion.test.EmbeddedKafkaBroker.endpoint;
import static org.apache.kafka.common.security.auth.SecurityProtocol.PLAINTEXT;
import static org.apache.kafka.common.security.auth.SecurityProtocol.SSL;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaWithSslSetupTask implements ServerSetupTask {
    volatile EmbeddedKafkaBroker broker;
    volatile KafkaCompanion companion;
    private static final Logger log = Logger.getLogger(RunKafkaWithSslSetupTask.class);
    private File configDir;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        try {
            configDir = new File("target", "reactive-messaging-kafka");
            configDir.mkdir();

            KeystoreUtil.createKeystores();

            Endpoint external = endpoint("EXTERNAL", SSL, "localhost", 9092);
            Endpoint internal = endpoint("INTERNAL", PLAINTEXT, "localhost", 19002);
            broker = new EmbeddedKafkaBroker()
                    .withAdvertisedListeners(external, internal)
                    .withAdditionalProperties(properties -> {
                        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KeystoreUtil.SERVER_KEYSTORE);
                        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, KeystoreUtil.KEYSTORE_PWD);
                        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, KeystoreUtil.KEYSTORE_PWD);
                        properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
                        properties.put(SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG, "SHA1PRNG");
                    })
                    .withDeleteLogDirsOnClose(true);
            broker.start();

            companion = new KafkaCompanion(EmbeddedKafkaBroker.toListenerString(internal));
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } catch (Exception e) {
            try {
                if (companion != null) {
                    companion.close();
                }
                if (broker != null) {
                    broker.close();
                }
            } finally {
                throw e;
            }
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        try {
            if (companion != null) {
                companion.close();
            }
            if (broker != null) {
                broker.close();
            }
        } finally {
            KeystoreUtil.cleanUp();
        }
    }
}