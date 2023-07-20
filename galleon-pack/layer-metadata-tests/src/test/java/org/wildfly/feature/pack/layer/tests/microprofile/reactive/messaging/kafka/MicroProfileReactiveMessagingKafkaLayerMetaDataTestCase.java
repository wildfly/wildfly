package org.wildfly.feature.pack.layer.tests.microprofile.reactive.messaging.kafka;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileReactiveMessagingKafkaLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    /*
    <prop name="org.wildfly.rule.properties-file-match-mp-kafka-property" value="[/META-INF/microprofile-config.properties,/WEB-INF/classes/META-INF/microprofile-config.properties],mp.messaging.connector.smallrye-kafka.*"/>
    <prop name="org.wildfly.rule.properties-file-match-mp-kafka-outgoing" value="[/META-INF/microprofile-config.properties,/WEB-INF/classes/META-INF/microprofile-config.properties],mp.messaging.outgoing.*.connector,smallrye-kafka"/>
    <prop name="org.wildfly.rule.properties-file-match-mp-kafka-incoming" value="[/META-INF/microprofile-config.properties,/WEB-INF/classes/META-INF/microprofile-config.properties],mp.messaging.incoming.*.connector,smallrye-kafka"/>

     */

    @Test
    public void testGlobalPropertyInWebInfClassesMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.WAR, "mp.messaging.connector.smallrye-kafka.sasl.mechanism=PLAIN");
    }

    @Test
    public void testGlobalPropertyInWebInf() throws Exception {
        testArchiveWithFile(ArchiveType.JAR, "mp.messaging.connector.smallrye-kafka.sasl.mechanism=PLAIN");
    }

    @Test
    public void testOutgoingInWebInfClassesMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.WAR, "mp.messaging.outgoing.name.connector=smallrye-kafka");
    }


    @Test
    public void testOutgoingInWebInf() throws Exception {
        testArchiveWithFile(ArchiveType.JAR, "mp.messaging.outgoing.name.connector=smallrye-kafka");
    }


    @Test
    public void testIncomingInWebInfClassesMetaInf() throws Exception {
        testArchiveWithFile(ArchiveType.WAR, "mp.messaging.incoming.name.connector=smallrye-kafka");
    }


    @Test
    public void testIncomingInWebInf() throws Exception {
        testArchiveWithFile(ArchiveType.JAR, "mp.messaging.incoming.name.connector=smallrye-kafka");
    }


    private void testArchiveWithFile(ArchiveType archiveType, String contents) throws Exception {
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
        checkLayersForArchive(p, "microprofile-config", "microprofile-reactive-messaging-kafka");
    }
}
