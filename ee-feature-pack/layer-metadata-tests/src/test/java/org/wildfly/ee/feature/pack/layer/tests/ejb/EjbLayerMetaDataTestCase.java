package org.wildfly.ee.feature.pack.layer.tests.ejb;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EjbLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testMessageDrivenAnnotation() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MessageDrivenAnnotationUsage.class)
                .build();
        checkLayersForArchive(p, "ejb");
    }

    @Test
    public void testRemoteAnnotation() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(RemoteAnnotationUsage.class)
                .build();
        checkLayersForArchive(p, "ejb");
    }

    @Test
    public void testMessageDrivenContextClass() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MessageDrivenContextClassUsage.class)
                .build();
        checkLayersForArchive(p, "ejb");
    }
}
