/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import static org.junit.Assert.assertNotNull;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class PersistenceContextRefTestCase {
    private static final String ARCHIVE_NAME = "persistence-context-ref";

    private static final String persistence_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                    "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
                    "  <persistence-unit name=\"mypc\">" +
                    "    <description>Persistence Unit." +
                    "    </description>" +
                    "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
                    "  <exclude-unlisted-classes>true</exclude-unlisted-classes>" +
                    "  <class>" + PcMyEntity.class.getName() + "</class>" +
                    "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/></properties>" +
                    "  </persistence-unit>" +
                    "  <persistence-unit name=\"otherpc\">" +
                    "    <description>Persistence Unit." +
                    "    </description>" +
                    "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
                    "  <exclude-unlisted-classes>true</exclude-unlisted-classes>" +
                    "  <class>" + PcOtherEntity.class.getName() + "</class>" +
                    "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/></properties>" +
                    "  </persistence-unit>" +
                    "</persistence>";


    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(PersistenceContextRefTestCase.class.getPackage());

        war.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        war.addAsWebInfResource(getWebXml(), "web.xml");
        return war;
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromAnnotation() throws NamingException {
        PcManagedBean bean = getManagedBean();
        bean.getMypc().getMetamodel().entity(PcMyEntity.class);
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromAnnotation2() throws NamingException {
        try {
            PcManagedBean bean = getManagedBean();
            bean.getMypc().getMetamodel().entity(PcOtherEntity.class);
            Assert.fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromPersistenceUnitRef() throws NamingException {
        try {
            PcManagedBean bean = getManagedBean();
            bean.getOtherpc().getMetamodel().entity(PcMyEntity.class);
            Assert.fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromPersistenceUnitRef2() throws NamingException {
        PcManagedBean bean = getManagedBean();
        bean.getOtherpc().getMetamodel().entity(PcOtherEntity.class);
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromRefInjectionTarget() throws NamingException {
        PcManagedBean bean = getManagedBean();
        bean.getMypc2().getMetamodel().entity(PcMyEntity.class);
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromRefInjectionTarget2() throws NamingException {
        try {
            PcManagedBean bean = getManagedBean();
            bean.getMypc2().getMetamodel().entity(PcOtherEntity.class);
            Assert.fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    @Test
    public void testUnsynchronizedPCisNotJoinedToTransaction() throws NamingException {
        PcManagedBean bean = getManagedBean();
        boolean isJoined = bean.unsynchronizedIsNotJoinedToTX();
        Assert.assertFalse("Unsynchronized entity manager should not of been joined to the JTA transaction but was",isJoined);
    }

    @Test
    public void testSynchronizedPCisJoinedToTransaction() throws NamingException {
        PcManagedBean bean = getManagedBean();
        boolean isJoined = bean.synchronizedIsJoinedToTX();
        Assert.assertTrue("Synchronized entity manager should of been joined to the JTA transaction but was not",isJoined);
    }

    private PcManagedBean getManagedBean() throws NamingException {
        InitialContext initialContext = new InitialContext();
        PcManagedBean bean = (PcManagedBean) initialContext.lookup("java:module/pcManagedBean");
        assertNotNull(bean);
        return bean;
    }


    private static StringAsset getWebXml() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<web-app version=\"3.0\"\n" +
                "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
                "         metadata-complete=\"false\">\n" +
                "\n" +
                "    <persistence-context-ref>\n" +
                "        <persistence-context-ref-name>otherPcBinding</persistence-context-ref-name>\n" +
                "        <persistence-unit-name>otherpc</persistence-unit-name>\n" +
                "    </persistence-context-ref>\n" + "\n" +
                "    <persistence-context-ref>\n" +
                "        <persistence-context-ref-name>unsyncPcBinding</persistence-context-ref-name>\n" +
                "        <persistence-unit-name>otherpc</persistence-unit-name>\n" +
                "        <persistence-context-synchronization>Unsynchronized</persistence-context-synchronization>\n" +
                "    </persistence-context-ref>\n" + "\n" +
                "    <persistence-context-ref>\n" +
                "        <persistence-context-ref-name>AnotherPuBinding</persistence-context-ref-name>\n" +
                "        <persistence-unit-name>mypc</persistence-unit-name>\n" +
                "        <injection-target>" +
                "           <injection-target-class>" + PcManagedBean.class.getName() + "</injection-target-class>" +
                "           <injection-target-name>mypc2</injection-target-name>" +
                "        </injection-target>\n" +
                "    </persistence-context-ref>\n" +
                "\n" +
                "</web-app>");
    }

}
