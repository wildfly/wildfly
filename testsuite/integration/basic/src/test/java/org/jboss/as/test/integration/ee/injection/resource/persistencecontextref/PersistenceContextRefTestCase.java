/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
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
        PcBean bean = getManagedBean();
        bean.getMypc().getMetamodel().entity(PcMyEntity.class);
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromAnnotation2() throws NamingException {
        try {
            PcBean bean = getManagedBean();
            bean.getMypc().getMetamodel().entity(PcOtherEntity.class);
            Assertions.fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromPersistenceUnitRef() throws NamingException {
        try {
            PcBean bean = getManagedBean();
            bean.getOtherpc().getMetamodel().entity(PcMyEntity.class);
            Assertions.fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromPersistenceUnitRef2() throws NamingException {
        PcBean bean = getManagedBean();
        bean.getOtherpc().getMetamodel().entity(PcOtherEntity.class);
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromRefInjectionTarget() throws NamingException {
        PcBean bean = getManagedBean();
        bean.getMypc2().getMetamodel().entity(PcMyEntity.class);
    }

    @Test
    public void testCorrectPersistenceUnitInjectedFromRefInjectionTarget2() throws NamingException {
        try {
            PcBean bean = getManagedBean();
            bean.getMypc2().getMetamodel().entity(PcOtherEntity.class);
            Assertions.fail();
        } catch (IllegalArgumentException expected) {

        }
    }

    @Test
    public void testUnsynchronizedPCisNotJoinedToTransaction() throws NamingException {
        PcBean bean = getManagedBean();
        boolean isJoined = bean.unsynchronizedIsNotJoinedToTX();
        Assertions.assertFalse(isJoined,"Unsynchronized entity manager should not of been joined to the JTA transaction but was");
    }

    @Test
    public void testSynchronizedPCisJoinedToTransaction() throws NamingException {
        PcBean bean = getManagedBean();
        boolean isJoined = bean.synchronizedIsJoinedToTX();
        Assertions.assertTrue(isJoined,"Synchronized entity manager should of been joined to the JTA transaction but was not");
    }

    private PcBean getManagedBean() throws NamingException {
        InitialContext initialContext = new InitialContext();
        PcBean bean = (PcBean) initialContext.lookup("java:module/pcManagedBean");
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
                "           <injection-target-class>" + PcBean.class.getName() + "</injection-target-class>" +
                "           <injection-target-name>mypc2</injection-target-name>" +
                "        </injection-target>\n" +
                "    </persistence-context-ref>\n" +
                "\n" +
                "</web-app>");
    }

}
