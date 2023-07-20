package org.wildfly.ee.feature.pack.layer.tests.ee.security;

import org.junit.Test;
import org.wildfly.ee.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class EeSecurityLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {
    //////////////////////////////////////////////////////////////////////
    // jakarta.security.enterprise-api annotations/classes

    @Test
    public void testSecurityEnterpriseRootPackageClass() throws Exception {
        addClassAndCheck(SecurityEnterpriseRootPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseHttpPackageAnnotation() throws Exception {
        addClassAndCheck(SecurityEnterpriseHttpPackageAnnotationUsage.class);
    }

    @Test public void testSecurityEnterpriseHttpPackageClass() throws Exception {
        addClassAndCheck(SecurityEnterpriseHttpPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseHttpOpenIdPackageAnnotation() throws Exception {
        addClassAndCheck(SecurityEnterpriseHttpOpenIdPackageAnnotationUsage.class);
    }

    @Test
    public void testSecurityEnterpriseHttpOpenIdPackageClass() throws Exception {
        addClassAndCheck(SecurityEnterpriseHttpOpenIdPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseCredentialPackageClass() throws Exception {
        addClassAndCheck(SecurityEnterpriseCredentialPackageClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseIdentityStoreAnnotation() throws Exception {
        addClassAndCheck(SecurityEnterpriseIdentityStoreAnnotationUsage.class);
    }

    @Test
    public void testSecurityEnterpriseIdentityStoreClass() throws Exception {
        addClassAndCheck(SecurityEnterpriseIdentityStoreClassUsage.class);
    }

    @Test
    public void testSecurityEnterpriseIdentityStoreOpenIdClass() throws Exception {
        addClassAndCheck(SecurityEnterpriseIdentityStoreOpenIdClassUsage.class);
    }

    //////////////////////////////////////////////////////////////////////
    // jakarta-authentication-api classes

    @Test
    public void testSecurityAuthMessageRootPackageClass() throws Exception {
        addClassAndCheck(SecurityAuthMessageRootPackageClassUsage.class);
    }

    @Test
    public void testSecurityAuthMessageCallbackPackageClass() throws Exception {
        addClassAndCheck(SecurityAuthMessageCallbackPackageClassUsage.class);
    }

    @Test
    public void testSecurityAuthMessageConfigPackageClass() throws Exception {
        addClassAndCheck(SecurityAuthMessageConfigPackageClassUsage.class);
    }

    @Test
    public void testSecurityAuthMessageModulePackageClass() throws Exception {
        addClassAndCheck(SecurityAuthMessageModulePackageClassUsage.class);
    }

    //////////////////////////////////////////////////////////////////////
    // jakarta-authorization-api classes
    @Test
    public void testSecurityJaccClass() throws Exception {
        addClassAndCheck(SecurityJaccClassUsage.class);
    }

    private void addClassAndCheck(Class<?> clazz) throws Exception {
        Path p =
                createArchiveBuilder(ArchiveType.WAR)
                        .addClasses(clazz)
                        .build();
        checkLayersForArchive(p, "ee-security");
    }
}
