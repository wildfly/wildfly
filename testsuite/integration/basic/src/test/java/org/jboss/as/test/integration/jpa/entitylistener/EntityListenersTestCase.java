/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.entitylistener;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 * EntityListeners tests
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@Ignore("AS7-2968")
public class EntityListenersTestCase {

    private static final String ARCHIVE_NAME = "jpa_EntityListeners";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(EntityListenersTestCase.class,
            Employee.class,
            MyListener.class,
            SFSBBMT.class,
            SFSBCMT.class
        );

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
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

    @Test
    public void testBMT() throws Exception {
        MyListener.setInvocationCount(0);
        SFSBBMT bmt = lookup("SFSBBMT", SFSBBMT.class);
        bmt.createEmployee("Alfred E. Neuman", "101010 Mad Street", 1);
        Employee emp = bmt.getEmployeeNoTX(1);
        bmt.updateEmployee(emp);
        assertTrue("could not load added employee", emp != null);
        assertTrue("EntityListener wasn't invoked twice as expected, instead " + MyListener.getInvocationCount(), 2 == MyListener.getInvocationCount());
    }

    @Test
    public void testCMT() throws Exception {
        MyListener.setInvocationCount(0);
        SFSBCMT cmt = lookup("SFSBCMT", SFSBCMT.class);
        cmt.createEmployee("Alfred E. Neuman", "101010 Mad Street", 2);
        Employee emp = cmt.getEmployeeNoTX(2);
        cmt.updateEmployee(emp);
        assertTrue("could not load added employee", emp != null);
        assertTrue("EntityListener wasn't invoked twice as expected, instead " + MyListener.getInvocationCount(), 2 == MyListener.getInvocationCount());
    }

}
