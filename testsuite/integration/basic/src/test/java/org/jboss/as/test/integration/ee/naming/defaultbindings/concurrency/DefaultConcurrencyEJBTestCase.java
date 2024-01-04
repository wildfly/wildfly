/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.naming.defaultbindings.concurrency;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Test for EE's default data source on an EJB
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultConcurrencyEJBTestCase {

    @Deployment
    public static EnterpriseArchive getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DefaultConcurrencyEJBTestCase.class.getSimpleName() + ".ear");
        JavaArchive module = ShrinkWrap.create(JavaArchive.class, "module.jar");
        module.addClasses(DefaultConcurrencyEJBTestCase.class, DefaultConcurrencyTestEJB.class);
        ear.addAsModule(module);
        return ear;
    }

    @Test
    public void testEJB() throws Throwable {
        final DefaultConcurrencyTestEJB testEJB = (DefaultConcurrencyTestEJB) new InitialContext().lookup("java:module/" + DefaultConcurrencyTestEJB.class.getSimpleName());
        testEJB.test();
    }
}
