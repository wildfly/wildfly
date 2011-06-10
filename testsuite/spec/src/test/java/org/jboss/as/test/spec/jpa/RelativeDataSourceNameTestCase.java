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

package org.jboss.as.test.spec.jpa;

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
 * Transaction tests
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class RelativeDataSourceNameTestCase {

    private static final String ARCHIVE_NAME = "RelativeDataSourceNameTestCase";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>jboss/datasources/ExampleDS" +
                "</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";
    
    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addClasses(Employee.class, SFSB1.class)
                .addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        System.out.println(jar.toString(true));
        return jar;
    }

    @EJB(mappedName = "java:global/RelativeDataSourceNameTestCase/SFSB1!org.jboss.as.test.spec.jpa.SFSB1")
    private SFSB1 sfsb1;

    @Test
    public void testQueryNonTXTransactionalEntityManagerInvocations() throws Exception {
        sfsb1.createEmployee("Susan Sells", "1 Main Street", 1);
    }

}
