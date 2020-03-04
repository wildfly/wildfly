/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jpa.resourceref;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verify that persistence unit using DataSource resource reference can work with entity class enhancement enabled.
 *
 */
@RunWith(Arquillian.class)
public class DataSourceResourceReferenceTestCase {

    private static final String ARCHIVE_NAME = "DataSourceResourceReferenceTestCase.ear";

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME);
        ear.addAsResource(DataSourceResourceReferenceTestCase.class.getPackage(), "application.xml", "application.xml");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(MyEjb.class, DataSourceResourceReferenceTestCase.class, Employee.class);
        jar.addAsManifestResource(DataSourceResourceReferenceTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(jar);

        return ear;
    }

    /**
     * Test that an application defined DataSource (via resource reference) can be used by persistence unit, in
     * addition to using (Hibernate ORM) bytecode enhancement.
     *
     * Note that the persistence.xml "jboss.as.jpa.classtransformer" is true to enable bytecode enhancement.
     * "wildfly.jpa.applicationdatasource" is also set to true, if it was false (default), the application
     * deployment would fail with:
     *
     * Required services that are not installed:" => ["jboss.naming.context.java.app.DataSourceResourceReferenceTestCase.env.testDS"],
     * "WFLYCTL0180: Services with missing/unavailable dependencies" => ["jboss.persistenceunit.\"DataSourceResourceReferenceTestCase.ear/
     *  ejb.jar#mainPu\".__FIRST_PHASE__ is missing [jboss.naming.context.java.app.DataSourceResourceReferenceTestCase.env.testDS]"
     *
     * @throws NamingException
     */
    @Test
    public void testPostConstruct() throws NamingException {
        MyEjb myEjb = (MyEjb) new InitialContext().lookup("java:module/" + MyEjb.class.getSimpleName());
        Assert.assertEquals(null,  myEjb.queryEmployeeName(100));
    }

}
