/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.concurrency;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 *
 * Test for EE's default data source on a Jakarta Contexts and Dependency Injection Bean
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultConcurrencyCDITestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(DefaultConcurrencyCDITestCase.class, DefaultConcurrencyTestCDIBean.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        jar.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return jar;
    }

    @Inject
    private DefaultConcurrencyTestCDIBean defaultConcurrencyTestCDIBean;

    @Test
    public void testCDI() throws Throwable {
        defaultConcurrencyTestCDIBean.test();
    }

}
