/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.hibernate.generator;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * PooledGeneratorTestCase
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class PooledGeneratorTestCase {

    private static final String ARCHIVE_NAME = "PooledGenerator.ear";

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME);
        JavaArchive ejbModule = ShrinkWrap.create(JavaArchive.class, "my-ejb-module.jar");
        ejbModule.addClasses(PooledGeneratorTestCase.class, TestSlsb.class);
        ejbModule.addAsManifestResource(PooledGeneratorTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ejbModule.addAsManifestResource(PooledGeneratorTestCase.class.getPackage(), "orm.xml", "orm.xml");

        ear.addAsModule(ejbModule);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addClass(Employee.class);
        ear.addAsLibrary(jar);
        return ear;
    }

    @Test
    public void testEntityInJarFileArchive() throws NamingException {
        TestSlsb slsb = (TestSlsb) new InitialContext().lookup("java:module/" + TestSlsb.class.getSimpleName());
        for (int looper = 0; looper < 500; looper++) {
            Employee employee = new Employee();
            employee.setName("Sarah" + looper);
            employee.setAddress(looper + " Main Street");
            slsb.save(employee);
        }
    }
}

