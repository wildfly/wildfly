/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.jmsconnectionfactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
public class DefaultJMSConnectionFactoryCDITestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(DefaultJMSConnectionFactoryCDITestCase.class, DefaultJMSConnectionFactoryTestCDIBean.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return jar;
    }

    @Inject
    private DefaultJMSConnectionFactoryTestCDIBean defaultJMSConnectionFactoryTestCDIBean;

    @Test
    public void testCDI() throws Throwable {
        defaultJMSConnectionFactoryTestCDIBean.test();
    }

}
