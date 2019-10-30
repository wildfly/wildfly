/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
