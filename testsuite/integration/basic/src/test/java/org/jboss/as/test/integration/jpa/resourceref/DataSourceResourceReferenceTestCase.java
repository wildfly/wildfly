/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
