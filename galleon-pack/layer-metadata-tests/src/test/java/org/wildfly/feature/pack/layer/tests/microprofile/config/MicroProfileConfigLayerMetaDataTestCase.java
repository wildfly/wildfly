package org.wildfly.feature.pack.layer.tests.microprofile.config;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MicroProfileConfigLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    //<prop name="org.wildfly.rule.expected-file" value="[/META-INF/microprofile-config.properties,/WEB-INF/classes/META-INF/microprofile-config.properties]"/>
    @Test
    public void testAnnotationUsage() throws Exception {
        // Only package containing annotations
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MicroProfileConfigAnnotationUsage.class)
                .build();
        checkLayersForArchive(p, "microprofile-config");
    }

    @Test
    public void testClassFromRootPackageUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MicroProfileConfigClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "microprofile-config");
    }

    @Test
    public void testClassFromSpiPackageUsage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(MicroProfileConfigClassFromSpiPackageUsage.class)
                .build();
        checkLayersForArchive(p, "microprofile-config");
    }

    @Test
    public void testMetaInfMicroProfileConfigProperties() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addFileToInf("microprofile-config.properties", "")
                .build();
        checkLayersForArchive(p, "microprofile-config");
    }

    @Test
    public void testWebInfClassesMetaInfMicroProfileConfigProperties() throws Exception{
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addFileToInf("microprofile-config.properties", "", true)
                .build();
        checkLayersForArchive(p, "microprofile-config");
    }
}
