/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import java.rmi.NoSuchObjectException;
import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBMetaData;
import jakarta.ejb.EJBObject;
import jakarta.ejb.Handle;
import jakarta.ejb.HomeHandle;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session21;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session21Bean;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session21Home;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session30;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session30RemoteBusiness;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for EJB3.0/EJB2.1 references.
 * Part of migration from EJB Testsuite (reference21_30) to AS7 [JIRA JBQA-5483].
 *
 * @author William DeCoste, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
public class ReferenceAnnotationDescriptorTestCase {

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "reference-ejb2-ejb3.jar")
                .addClasses(
                        HomedStatefulSession30Bean.class,
                        LocalSession30.class,
                        LocalSession30Business.class,
                        LocalStatefulSession30.class,
                        LocalStatefulSession30Business.class,
                        ReferenceAnnotationDescriptorTestCase.class,
                        Session30Home.class,
                        Session30LocalHome.class,
                        Session30Bean.class,
                        StatefulSession30.class,
                        StatefulSession30Bean.class,
                        StatefulSession30Home.class,
                        StatefulSession30LocalHome.class,
                        StatefulSession30RemoteBusiness.class
                )
                .addClasses(
                        Session30.class,
                        Session30RemoteBusiness.class,
                        Session21.class,
                        Session21Home.class,
                        Session21Bean.class);
        jar.addAsManifestResource(ReferenceAnnotationDescriptorTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(ReferenceAnnotationDescriptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testSession21() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session21Home home = (Session21Home) jndiContext.lookup("java:module/Session21!" + Session21Home.class.getName());
        Session21 session = home.create();
        String access = session.access();
        Assertions.assertEquals("Session21", access);
        access = session.access30();
        Assertions.assertEquals("Session30", access);
    }

    @Test
    public void testSession30() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session30Home sessionHome = (Session30Home) jndiContext.lookup("java:module/Session30!" + Session30Home.class.getName());
        Session30 session = sessionHome.create();
        String access = session.access();
        Assertions.assertEquals("Session30", access);
        access = session.access21();
        Assertions.assertEquals("Session21", access);
    }

    @Test
    public void testSessionHome30() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session30RemoteBusiness session = (Session30RemoteBusiness) jndiContext.lookup("java:module/Session30!" + Session30RemoteBusiness.class.getName());
        Assertions.assertNotNull(session);
        String access = session.access();
        Assertions.assertEquals("Session30", access);

        Session30Home home = (Session30Home) jndiContext.lookup("java:module/Session30!" + Session30Home.class.getName());
        Assertions.assertNotNull(home);
        Session30 sessionRemote = home.create();
        Assertions.assertNotNull(sessionRemote);
        access = sessionRemote.access();
        Assertions.assertEquals("Session30", access);
    }

    @Test
    public void testStatefulRemove() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assertions.assertNotNull(home);
        StatefulSession30 session = home.create();
        Assertions.assertNotNull(session);
        session.setValue("123");
        String value = session.getValue();
        Assertions.assertEquals("123", value);

        EJBObject ejbObject = session;

        Handle handle = session.getHandle();
        Assertions.assertNotNull(handle);

        home.remove(handle);

        try {
            session.getValue();
            Assertions.assertTrue(false);
        } catch (NoSuchObjectException nsoe) {
            // OK: EJB3.1 7.5.3
        }

        session = home.create();
        Assertions.assertNotNull(session);
        session.setValue("123");
        value = session.getValue();
        Assertions.assertEquals("123", value);

        session.remove();

        try {
            session.getValue();
            Assertions.assertTrue(false);
        } catch (NoSuchObjectException nsoe) {
            // OK: EJB3.1 7.5.3
        }
    }

    @Test
    public void testStatefulSessionHome30() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness session = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());
        Assertions.assertNotNull(session);
        session.setValue("testing");
        String value = session.getValue();
        Assertions.assertEquals("testing", value);

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assertions.assertNotNull(home);
        session = home.create();
        Assertions.assertNotNull(session);
        session.setValue("123");
        value = session.getValue();
        Assertions.assertEquals("123", value);

        session = home.create("456");
        Assertions.assertNotNull(session);
        value = session.getValue();
        Assertions.assertEquals("456", value);

        session = home.create("combined", new Integer("789"));
        Assertions.assertNotNull(session);
        value = session.getValue();
        Assertions.assertEquals("combined789", value);
    }

    @Test
    public void testRemoteHomeAnnotation() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/HomedStatefulSession30!" + StatefulSession30Home.class.getName());
        Assertions.assertNotNull(home);
        StatefulSession30 session = home.create();
        Assertions.assertNotNull(session);
        session.setValue("123");
        String value = session.getValue();
        Assertions.assertEquals("123", value);

        session = home.create("456");
        Assertions.assertNotNull(session);
        value = session.getValue();
        Assertions.assertEquals("456", value);

        session = home.create("combined", new Integer("789"));
        Assertions.assertNotNull(session);
        value = session.getValue();
        Assertions.assertEquals("combined789", value);
    }

    @Test
    public void testLocalHomeAnnotation() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness session = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());

        String access = session.accessLocalHome();
        Assertions.assertEquals("LocalHome", access);
    }

    @Test
    public void testLocalHome() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness statefulSession = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());
        Assertions.assertNotNull(statefulSession);
        String access = statefulSession.accessLocalStateless();
        Assertions.assertEquals("Session30", access);

        Session30RemoteBusiness session = (Session30RemoteBusiness) jndiContext.lookup("java:module/Session30!" + Session30RemoteBusiness.class.getName());
        Assertions.assertNotNull(session);
        access = session.accessLocalStateful();
        Assertions.assertEquals("default", access);

        access = session.accessLocalStateful("testing");
        Assertions.assertEquals("testing", access);

        access = session.accessLocalStateful("testing", new Integer(123));
        Assertions.assertEquals("testing123", access);
    }

    @Test
    public void testStatefulState() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness session1 = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());
        Assertions.assertNotNull(session1);
        session1.setValue("testing");
        Assertions.assertEquals("testing", session1.getValue());

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assertions.assertNotNull(home);
        StatefulSession30 session3 = home.create();
        Assertions.assertNotNull(session3);
        session3.setValue("123");
        Assertions.assertEquals("123", session3.getValue());

        StatefulSession30 session4 = home.create();
        Assertions.assertNotNull(session4);
        Assertions.assertEquals("default", session4.getValue());
        Assertions.assertEquals("default", session4.getValue());

        StatefulSession30 session5 = home.create("init");
        Assertions.assertNotNull(session5);
        Assertions.assertEquals("init", session5.getValue());

        StatefulSession30 session6 = home.create("init", new Integer(123));
        Assertions.assertNotNull(session6);
        Assertions.assertEquals("init123", session6.getValue());

        StatefulSession30 session7 = home.create("secondinit");
        Assertions.assertNotNull(session7);
        Assertions.assertEquals("secondinit", session7.getValue());

        Assertions.assertEquals("testing", session1.getValue());
        Assertions.assertEquals("123", session3.getValue());
        Assertions.assertEquals("default", session4.getValue());
        Assertions.assertEquals("init", session5.getValue());
        Assertions.assertEquals("init123", session6.getValue());
        Assertions.assertEquals("secondinit", session7.getValue());
    }

    @Test
    public void testStateful21Interfaces() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assertions.assertNotNull(home);

        EJBMetaData metadata = home.getEJBMetaData();
        Assertions.assertNotNull(metadata);
        Assertions.assertEquals(StatefulSession30.class, metadata.getRemoteInterfaceClass());

        HomeHandle homeHandle = home.getHomeHandle();
        Assertions.assertNotNull(homeHandle);

        EJBHome ejbHome = homeHandle.getEJBHome();
        Assertions.assertNotNull(ejbHome);
        metadata = ejbHome.getEJBMetaData();
        Assertions.assertNotNull(metadata);
        Assertions.assertEquals(StatefulSession30.class, metadata.getRemoteInterfaceClass());

        StatefulSession30 session = home.create();
        Assertions.assertNotNull(session);
        ejbHome = session.getEJBHome();
        Assertions.assertNotNull(ejbHome);

        Handle handle = session.getHandle();
        Assertions.assertNotNull(handle);

        EJBObject ejbObject = handle.getEJBObject();
        Assertions.assertNotNull(ejbObject);

        ejbHome = ejbObject.getEJBHome();
        Assertions.assertNotNull(ejbHome);

        Handle handle1 = ejbObject.getHandle();
        Assertions.assertNotNull(handle1);

        StatefulSession30 session1 = home.create();
        Assertions.assertFalse(session.isIdentical(session1));
        Assertions.assertTrue(session.isIdentical(session));
    }

    @Test
    public void testStateless21Interfaces() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session30Home home = (Session30Home) jndiContext.lookup("java:module/Session30!" + Session30Home.class.getName());
        Assertions.assertNotNull(home);

        EJBMetaData metadata = home.getEJBMetaData();
        Assertions.assertNotNull(metadata);
        Assertions.assertEquals(Session30.class.getName(), metadata.getRemoteInterfaceClass().getName());

        HomeHandle homeHandle = home.getHomeHandle();
        Assertions.assertNotNull(homeHandle);

        EJBHome ejbHome = homeHandle.getEJBHome();
        Assertions.assertNotNull(ejbHome);
        metadata = ejbHome.getEJBMetaData();
        Assertions.assertNotNull(metadata);
        Assertions.assertEquals(Session30.class.getName(), metadata.getRemoteInterfaceClass().getName());

        Session30 session = home.create();
        Assertions.assertNotNull(session);
        ejbHome = session.getEJBHome();
        Assertions.assertNotNull(ejbHome);

        Handle handle = session.getHandle();
        Assertions.assertNotNull(handle);

        EJBObject ejbObject = handle.getEJBObject();
        Assertions.assertNotNull(ejbObject);

        ejbHome = ejbObject.getEJBHome();
        Assertions.assertNotNull(ejbHome);

        Handle handle1 = ejbObject.getHandle();
        Assertions.assertNotNull(handle1);

        Session30 session1 = home.create();
        Assertions.assertTrue(session.isIdentical(session1));
    }
}
