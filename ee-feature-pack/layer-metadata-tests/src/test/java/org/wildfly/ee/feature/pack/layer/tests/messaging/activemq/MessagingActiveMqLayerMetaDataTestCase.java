package org.wildfly.ee.feature.pack.layer.tests.messaging.activemq;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MessagingActiveMqLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAnnotationUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(MessagingActiveMqAnnotationUsage.class)
                .build();
        checkLayersForArchive(p,"messaging-activemq");
    }

    @Test
    public void testClassUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addClasses(MessagingActiveMqClassUsage.class)
                .build();
        checkLayersForArchive(p,"messaging-activemq");
    }
}
