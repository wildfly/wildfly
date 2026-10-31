/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.weld.beanarchives;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * See also <a href="https://issues.jboss.org/browse/WFLY-7025">WFLY-7025</a>.
 *
 * @author Martin Kouba
 */
@ExtendWith(ArquillianExtension.class)
public class BootstrapBeanDeploymentArchiveTestCase {

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class).addPackage(BootstrapBeanDeploymentArchiveTestCase.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider(Extension.class, TestExtension.class)
                .addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("accessDeclaredMembers")),"permissions.xml");
    }

    @Inject
    @Alpha
    Map<String, String> alphaMap;

    @Inject
    @Bravo
    Map<String, String> bravoMap;

    @Test
    public void testDeployment() throws NamingException {
        // Test that the deployment does not fail due to non-unique bean deployment identifiers and also the custom beans
        assertEquals(alphaMap.get("foo"), bravoMap.get("foo"));
    }

}
