/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ee.integration;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EeIntegrationLayerMetadataTestCase extends AbstractLayerMetaDataTestCase {
    @Test
    public void testJakartaAnnotationSecurityPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaAnnotationSecurityPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaAnnotationSqlPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaAnnotationSqlPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaAnnotationPackageAnnotationUsage() throws Exception {
        testWarWithClass(JakartaAnnotationPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaXmlBindAnnotationAdaptersPackageAnnotationUsage() {
        testWarWithClass(JakartaXmlBindAnnotationAdaptersPackageAnnotationUsage.class);
    }

    @Test
    public void testJakartaXmlBindAnnotationPackageAnnotationUsage() {
        testWarWithClass(JakartaXmlBindAnnotationPackageAnnotationUsage.class);
    }


    @Test
    public void testJakartaXmlBindRootPackageClassUsage() {
        testWarWithClass(JakartaXmlBindRootPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlAnnotationPackageClassUsage() {
        testWarWithClass(JakartaXmlAnnotationPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlAnnotationAdaptersPackageClassUsage() {
        testWarWithClass(JakartaXmlAnnotationAdaptersPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlBindAttachmentPackageClassUsage() {
        testWarWithClass(JakartaXmlBindAttachmentPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlBindHelpersPackageClassUsage() {
        // jakarta.xml.bind.helpers.AbstractMarshallerImpl
        testWarWithClass(JakartaXmlBindHelpersPackageClassUsage.class);
    }

    @Test
    public void testJakartaXmlBindUtilPackageClassUsage() {
        testWarWithClass(JakartaXmlBindUtilPackageClassUsage.class);
    }

    private void testWarWithClass(Class<?> clazz) {
        Path p= createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        // ee-integration is a dependency of the ee-core-profile-server so it doesn't show up as a decorator
        checkLayersForArchive(p, "ee-integration");
    }
}
