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

package org.jboss.as.test.integration.jpa.ormxml;

import static org.junit.Assert.assertNull;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
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
public class OrmTestCase {

    private static final String ARCHIVE_NAME = "jpa_OrmTestCase";

    private static final String orm_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<entity-mappings xmlns=\"http://java.sun.com/xml/ns/persistence/orm\" version=\"2.0\">" +
                    "<entity class=\"org.jboss.as.test.integration.jpa.ormxml.Employee\"/>" +
                    "</entity-mappings>";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(OrmTestCase.class,
                Employee.class,
                SFSBCMT.class
        );

        jar.addAsManifestResource(OrmTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsResource(new StringAsset(orm_xml), "META-INF/orm.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Test
    public void testOrmXmlDefinedEmployeeEntity() throws Exception {
        SFSBCMT sfsbcmt = lookup("SFSBCMT", SFSBCMT.class);
        Employee emp = sfsbcmt.queryEmployeeName(1);
        assertNull("entity shouldn't exist", emp);

    }


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }
}
