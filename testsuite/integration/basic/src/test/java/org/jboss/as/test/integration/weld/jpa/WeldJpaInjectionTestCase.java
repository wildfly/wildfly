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

package org.jboss.as.test.integration.weld.jpa;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Weld JPA injection tests. Simply tests that a persistence context can be injected into a CDI bean.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldJpaInjectionTestCase {

    private static final String ARCHIVE_NAME = "jpa_OrmTestCase";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"cdiPu\">" +
            "    <description>OrmTestCase Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";


    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(WeldJpaInjectionTestCase.class,
            Employee.class,
            CdiJpaInjectingBean.class
        );

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return jar;
    }

    @Inject
    private CdiJpaInjectingBean bean;

    @Test
    public void testOrmXmlDefinedEmployeeEntity() {

        try {
            Employee emp = bean.queryEmployeeName(1);
        } catch (Exception e) {
            if (!(e instanceof NoResultException)){
                Assert.fail("Expected NoResultException but got " + e);
            }
            return;
        }
        Assert.fail("NoResultException should occur but didn't!");
    }
}
