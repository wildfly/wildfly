/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.mail;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class MailLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testAnnotationFromRootPackageUsage() {
        testSingleClassWar(MailAnnotationFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromRootPackageUsage() {
        testSingleClassWar(MailClassFromRootPackageUsage.class);
    }

    @Test
    public void testClassFromEventPackageUsage() {
        testSingleClassWar(MailClassFromEventPackageUsage.class);
    }

    @Test
    public void testClassFromInternetPackageUsage() {
        testSingleClassWar(MailClassFromInternetPackageUsage.class);
    }

    @Test
    public void testClassFromSearchPackageUsage() {
        testSingleClassWar(MailClassFromSearchPackageUsage.class);
    }

    @Test
    public void testClassFromUtilPackageUsage() {
        testSingleClassWar(MailClassFromUtilPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("mail", "mail"));
    }
}
