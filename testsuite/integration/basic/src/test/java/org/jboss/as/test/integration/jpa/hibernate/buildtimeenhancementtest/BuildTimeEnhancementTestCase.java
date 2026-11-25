/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest.entityClasses.Employee;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verify that the Hibernate ORM build time bytecode enhancement Maven plugin works as expected.
 * - Verify that Hibernate ORM build time enhancement was run against the Employee class.
 * <p>
 * For more details on build time enhancement see:
 * - https://docs.hibernate.org/orm/6.6/userguide/html_single/#tooling-maven
 * - https://docs.hibernate.org/orm/7.4/userguide/html_single/#tooling-maven
 * @author Scott Marlow
 */

@RunWith(Arquillian.class)
public class BuildTimeEnhancementTestCase {

    private static final String ARCHIVE_NAME = "jpa_BuildTimeEnhancementTestCase";
    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(BuildTimeEnhancementTestCase.class,
                Employee.class,
                SFSB1.class
        );
        jar.addAsManifestResource(BuildTimeEnhancementTestCase.class.getPackage(), "persistencenoclasstransformer.xml", "persistence.xml");
        return jar;
    }


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testHibernateBuildTimeEnhancement() {
        // Note: ManagedTypeHelper is an internal Hibernate ORM class, if it is removed or renamed then this test can be updated
        // accordingly.
        assertTrue("Employee class is expected to be be build time bytecode enhanced but wasn't (" +
                        org.hibernate.engine.internal.ManagedTypeHelper.isManagedType(Employee.class) + ")",
                org.hibernate.engine.internal.ManagedTypeHelper.isManagedType(Employee.class));
    }

    @Test
    public void testhibernateCanCreateEmployeeAndReadEmployee() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Kelly Smith", "Watford, England", 10);
        sfsb1.createEmployee("Alex Scott", "London, England", 20);
        Employee emp = sfsb1.getEmployeeNoTX(10);

        assertNotNull("was able to read database row", emp);
    }


}
