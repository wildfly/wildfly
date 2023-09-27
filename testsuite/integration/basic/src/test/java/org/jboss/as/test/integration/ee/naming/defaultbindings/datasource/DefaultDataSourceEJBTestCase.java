/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

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
public class DefaultDataSourceEJBTestCase {

    @Deployment
    public static EnterpriseArchive getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DefaultDataSourceEJBTestCase.class.getSimpleName() + ".ear");
        JavaArchive module = ShrinkWrap.create(JavaArchive.class, "module.jar");
        module.addClasses(DefaultDataSourceEJBTestCase.class, DefaultDataSourceTestEJB.class);
        ear.addAsModule(module);
        return ear;
    }

    @Test
    public void testEJB() throws Throwable {
        final DefaultDataSourceTestEJB testEJB = (DefaultDataSourceTestEJB) new InitialContext().lookup("java:module/" + DefaultDataSourceTestEJB.class.getSimpleName());
        testEJB.test();
    }
}
