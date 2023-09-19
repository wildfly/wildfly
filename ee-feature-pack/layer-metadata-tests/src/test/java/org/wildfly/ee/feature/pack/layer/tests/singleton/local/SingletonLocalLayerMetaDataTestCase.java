package org.wildfly.ee.feature.pack.layer.tests.singleton.local;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class SingletonLocalLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testSingletonDeploymentXmlInMetaInf() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.JAR)
                .addXml("singleton-deployment.xml", "")
                .build();
        checkLayersForArchive(p, "singleton-local");
    }

    @Test
    public void testSingletonDeploymentXmlInWebInfClassesMetaInf() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addXml("singleton-deployment.xml", "", true)
                .build();
        checkLayersForArchive(p, "singleton-local");
    }

    @Test
    public void testSingletonDeploymentClassUsageFromRootPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(SingletonLocalClassFromRootPackageUsage.class)
                .build();
        checkLayersForArchive(p, "singleton-local");
    }

    @Test
    public void testSingletonDeploymentClassUsageFromElectionPackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(SingletonLocalClassFromElectionPackageUsage.class)
                .build();
        checkLayersForArchive(p, "singleton-local");
    }

    @Test
    public void testSingletonDeploymentClassUsageFromServicePackage() throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(SingletonLocalClassFromServicePackageUsage.class)
                .build();
        checkLayersForArchive(p, "singleton-local");
    }
}
