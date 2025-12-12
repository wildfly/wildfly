/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.earbytecodeenhancement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test that an EJB jar can reference the persistence provider classes
 */
@RunWith(Arquillian.class)
public class EJBJarPackagingTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "scopedToEar.ear");

        JavaArchive ejbjar = ShrinkWrap.create(JavaArchive.class, "ejbjar.jar");
        ejbjar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbjar.addAsManifestResource(EJBJarPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ejbjar.addClasses(EmployeeBean.class, EJBJarPackagingTestCase.class);
        ejbjar.addClasses(Employee.class);
        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Organisation.class);
        ear.addAsLibrary(lib);          // add entity jar to ear/lib




        return ear;
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    /**
     * Test that bean in ejbjar can access persistence provider class
     */
    @Test
    public void testBeanInEJBJarCanAccessPersistenceProviderClass() throws Exception {
        EmployeeBean bean = (EmployeeBean) iniCtx.lookup("java:app/ejbjar/EmployeeBean");
        Class sessionClass = bean.getPersistenceProviderClass("org.hibernate.Session");
        assertNotNull("was able to load 'org.hibernate.Session' class from persistence provider", sessionClass);
    }

    @Test
    public void testEntityByteCodeIsEnhanced() throws Exception {
            EmployeeBean bean = (EmployeeBean) iniCtx.lookup("java:app/ejbjar/EmployeeBean");
            assertTrue("Employee entity class needs to be bytecode enhanced",
                    bean.isEmployeeClassByteCodeEnhanced());
            assertTrue("Organisation entity class (that resides in ear/lib/lib.jar) needs to be bytecode enhanced",
                    bean.isOrganisationClassByteCodeEnhanced());
        }

    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                        "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                        "         version=\"3.0\">\n" +
                        "   \n" +
                        "</ejb-jar>");
    }

}
