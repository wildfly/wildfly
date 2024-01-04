/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.ee.security;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EeSecurityLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    //////////////////////////////////////////////////////////////////////
    // jakarta.security.enterprise-api annotations/classes

    @Test
    public void testSecurityEnterpriseRootPackageClass() {
        addClassAndCheck(SecurityEnterpriseRootPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseHttpPackageAnnotation() {
        addClassAndCheck(SecurityEnterpriseHttpPackageAnnotationUsage.class);
    }

    @Test public void testSecurityEnterpriseHttpPackageClass() {
        addClassAndCheck(SecurityEnterpriseHttpPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseHttpOpenIdPackageAnnotation() {
        addClassAndCheck(SecurityEnterpriseHttpOpenIdPackageAnnotationUsage.class);
    }

    @Test
    public void testSecurityEnterpriseHttpOpenIdPackageClass() {
        addClassAndCheck(SecurityEnterpriseHttpOpenIdPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseCredentialPackageClass() {
        addClassAndCheck(SecurityEnterpriseCredentialPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseIdentityStoreAnnotation() {
        addClassAndCheck(SecurityEnterpriseIdentityStoreAnnotationUsage.class);
    }

    @Test
    public void testSecurityEnterpriseIdentityStoreClass() {
        addClassAndCheck(SecurityEnterpriseIdentityStoreClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseIdentityStoreOpenIdClass() {
        addClassAndCheck(SecurityEnterpriseIdentityStoreOpenIdClassUsage.class);
    }

    //////////////////////////////////////////////////////////////////////
    // jakarta-authentication-api classes

    @Test
    public void testSecurityAuthMessageRootPackageClass() {
        addClassAndCheck(SecurityAuthMessageRootPackageClassUsage.class);
    }

    @Test
    public void testSecurityAuthMessageCallbackPackageClass() {
        addClassAndCheck(SecurityAuthMessageCallbackPackageClassUsage.class);
    }

    @Test
    public void testSecurityAuthMessageConfigPackageClass() {
        addClassAndCheck(SecurityAuthMessageConfigPackageClassUsage.class);
    }

    @Test
    public void testSecurityAuthMessageModulePackageClass() {
        addClassAndCheck(SecurityAuthMessageModulePackageClassUsage.class);
    }

    //////////////////////////////////////////////////////////////////////
    // jakarta-authorization-api classes
    @Test
    public void testSecurityJaccClass() {
        addClassAndCheck(SecurityJaccClassUsage.class);
    }

    private void addClassAndCheck(Class<?> clazz) {
        Path p =
                createArchiveBuilder(ArchiveType.WAR)
                        .addClasses(clazz)
                        .build();
        checkLayersForArchive(p, new ExpectedLayers("ee-security", "ee-security"));
    }
}
