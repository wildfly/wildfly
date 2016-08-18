/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.packaging;

import static org.junit.Assert.assertNotNull;

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
        ejbjar.addClasses(EmployeeBean.class, EJBJarPackagingTestCase.class);
        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class);
        ear.addAsLibrary(lib);          // add entity jar to ear/lib

        // add persistence.xml to ear/META-INF
        ear.addAsManifestResource(EJBJarPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

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
