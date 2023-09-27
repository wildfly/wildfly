/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.reactive.messaging.kafka;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileReactiveMessagingKafkaLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testGlobalPropertyInWebInfClassesMetaInf() {
        testArchiveWithFile(ArchiveType.WAR, "mp.messaging.connector.smallrye-kafka.sasl.mechanism=PLAIN");
    }

    @Test
    public void testGlobalPropertyInWebInf() {
        testArchiveWithFile(ArchiveType.JAR, "mp.messaging.connector.smallrye-kafka.sasl.mechanism=PLAIN");
    }

    @Test
    public void testOutgoingInWebInfClassesMetaInf() {
        testArchiveWithFile(ArchiveType.WAR, "mp.messaging.outgoing.name.connector=smallrye-kafka");
    }


    @Test
    public void testOutgoingInWebInf() {
        testArchiveWithFile(ArchiveType.JAR, "mp.messaging.outgoing.name.connector=smallrye-kafka");
    }


    @Test
    public void testIncomingInWebInfClassesMetaInf() {
        testArchiveWithFile(ArchiveType.WAR, "mp.messaging.incoming.name.connector=smallrye-kafka");
    }


    @Test
    public void testIncomingInWebInf() {
        testArchiveWithFile(ArchiveType.JAR, "mp.messaging.incoming.name.connector=smallrye-kafka");
    }


    private void testArchiveWithFile(ArchiveType archiveType, String contents) {
        ArchiveBuilder builder = createArchiveBuilder(archiveType);
        if (contents == null) {
            contents = "";
        }
        if (archiveType == ArchiveType.WAR) {
            // Will go into WEB-INF/classes/META-INF
            builder.addFileToInf("microprofile-config.properties", contents, true);
        } else if (archiveType == ArchiveType.JAR) {
            // Will go into /META-INF
            builder.addFileToInf("microprofile-config.properties", contents);
        } else {
            throw new IllegalStateException("Unhandled archiveType " + archiveType);
        }
        Path p = builder.build();
        checkLayersForArchive(p,
                // microprofile-config doesn't show up as a decorator since it is a dependency of microprofile-reactive-messaging-kafka
                new ExpectedLayers("microprofile-config")
                        .addLayerAndDecorator("microprofile-reactive-messaging-kafka", "microprofile-reactive-messaging-kafka"));
    }
}
