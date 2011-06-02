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

import java.util.Map;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensure that both pu definitions can be used.
 *
 * @author Scott Marlow
 */
@Ignore // Currently failing during AS7-734 Migration
@RunWith(Arquillian.class)
public class MultiplePuTestCase {

    private static final String ARCHIVE_NAME = "MultiplePuTestCase";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"pu1\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "    <jta-data-source>H2DS</jta-data-source>" +
            "    <properties> " +
            "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "      <property name=\"PersistenceUnitName\" value=\"pu1\"/>" +
            "    </properties>" +
            "  </persistence-unit>" +
            "  <persistence-unit name=\"pu2\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "    <jta-data-source>H2DS</jta-data-source>" +
            "    <properties> " +
            "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "      <property name=\"PersistenceUnitName\" value=\"pu2\"/>" +
            "    </properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(MultiplePuTestCase.class,
            SLSBPU1.class,
            SLSBPU2.class
        );

        jar.add(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.add(new StringAsset(""), "META-INF/MANIFEST.MF");
        return jar;
    }

    @EJB(mappedName = "java:global/test/SLSBPU1!org.jboss.as.test.spec.jpa.SLSBPU1")
    private SLSBPU1 slsbpu1;

    @EJB(mappedName = "java:global/test/SLSBPU2!org.jboss.as.test.spec.jpa.SLSBPU2")
    private SLSBPU2 slsbpu2;


    @Test
    public void testBothPersistenceUnitDefinitions() throws Exception {
        Map sl1Props = slsbpu1.getEMInfo();
        Map sl2Props = slsbpu2.getEMInfo();

        Assert.assertEquals("wrong pu " ,sl1Props.get("PersistenceUnitName"),"pu1");
        Assert.assertEquals("wrong pu ", sl2Props.get("PersistenceUnitName"),"pu2");
    }

}
