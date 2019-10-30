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

package org.jboss.as.test.integration.ejb.persistence;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
public class PersistenceUnitTestCase {

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"ejb3-persistence-test-pu\">" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";



    @EJB (mappedName = "java:module/SingletonBean")
    private SingletonBean singletonBean;

    @Deployment
    public static Archive<?> getDeployment() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-persistence-test.jar");
        jar.addClasses(SingletonBean.class, SimpleEntity.class);
        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }

    @Test
    public void testSingletonBeanDestroy() {
        this.singletonBean.doNothing();
    }
}
