/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.tests.deployment.classloading.jar;

import junit.framework.Assert;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.jpa.container.SFSBXPCMap;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.Exception;
import java.lang.RuntimeException;

@RunWith(Arquillian.class)
public class JPAMockSFSBContainerTestCase {

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"H2DS-UNIT\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:/H2DS</jta-data-source>" +
            "  <class>org.jboss.as.tests.deployment.classloading.jar.Employee</class>" +
            "  </persistence-unit>" +
            "</persistence>"
        ;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "testjar.jar");
        jar.addResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.addClass(JPAMockSFSBContainerTestCase.class);
        jar.addClass(Employee.class);
        jar.addClass(SFSBInterface.class);
        jar.addClass(SFSB.class);
        jar.addResource(new StringAsset(""), "META-INF/ejb-jar.xml");

        return jar;
    }

    @Test
    public void createMockSFSB() throws ClassNotFoundException,Exception {
        Class clsSFSBInterface = loadClass("org.jboss.as.tests.deployment.classloading.jar.SFSBInterface");
        Class cls = loadClass("org.jboss.as.tests.deployment.classloading.jar.SFSB");
//        MockStatefulSessionBeanContainer mockStatefulSessionBeanContainer = new MockStatefulSessionBeanContainer();
//        SFSBInterface bean = (SFSBInterface)mockStatefulSessionBeanContainer.getReady(cls);
        Assert.assertEquals(1, bean.getCount());
    }


    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
