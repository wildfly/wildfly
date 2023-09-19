package org.wildfly.ee.feature.pack.layer.tests.mail;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MailLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationFromRootPackageUsage() throws Exception {
        testSingleClassWar(MailAnnotationFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromRootPackageUsage() throws Exception {
        testSingleClassWar(MailClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromEventPackageUsage() throws Exception {
        testSingleClassWar(MailClassFromEventPackageUsage.class);
    }

    @Test
    public void testClassFromInternetPackageUsage() throws Exception {
        testSingleClassWar(MailClassFromInternetPackageUsage.class);
    }

    @Test
    public void testClassFromSearchPackageUsage() throws Exception {
        testSingleClassWar(MailClassFromSearchPackageUsage.class);
    }

    @Test
    public void testClassFromUtilPackageUsage() throws Exception {
        testSingleClassWar(MailClassFromUtilPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "mail");
    }
}
