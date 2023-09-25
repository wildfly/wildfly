/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.version;

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensure that version handling works.
 * This tests that JPA 2.0 specification section 7.6.1 is followed.  Specifically:
 * "
 * If the entity manager is invoked outside the scope of a transaction, any entities loaded from the database
 * will immediately become detached at the end of the method call.
 * "
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class VersionTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(VersionTestCase.class,
                Employee.class,
                SFSB1.class
        );
        jar.addAsManifestResource(VersionTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private static InitialContext iniCtx;


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testVersion() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        // tx1 will create the employee
        sfsb1.createEmployee("Sally", "1 home street", 1);

        // non-tx2 will load the entity
        Employee emp = sfsb1.getEmployeeNoTX(1);
        Integer firstVersion = emp.getVersion();

        // non-tx3 will load the entity
        // tx4 will update the employee
        // no-tx4 will load the entity (shouldn't see the stale entity read in non-tx3)
        Employee updatedEmp = sfsb1.mutateEmployee(emp);

        assertTrue(
                "entities read in non-tx should be detached from persistence context as they are read." +
                        "  version at time of creation = " + firstVersion +
                        ", version after update should be greater than creation version" +
                        ", version after update is = " + updatedEmp.getVersion(),
                firstVersion.intValue() < updatedEmp.getVersion().intValue());
    }

}
