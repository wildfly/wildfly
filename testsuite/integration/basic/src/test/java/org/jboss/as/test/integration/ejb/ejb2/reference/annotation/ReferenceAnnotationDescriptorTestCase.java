/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import java.rmi.NoSuchObjectException;
import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session21;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session21Bean;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session21Home;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session30;
import org.jboss.as.test.integration.ejb.ejb2.reference.global.Session30RemoteBusiness;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for EJB3.0/EJB2.1 references.
 * Part of migration from EJB Testsuite (reference21_30) to AS7 [JIRA JBQA-5483].
 *
 * @author William DeCoste, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
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
        Assert.assertEquals("Session21", access);
        access = session.access30();
        Assert.assertEquals("Session30", access);
    }

    @Test
    public void testSession30() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session30Home sessionHome = (Session30Home) jndiContext.lookup("java:module/Session30!" + Session30Home.class.getName());
        Session30 session = sessionHome.create();
        String access = session.access();
        Assert.assertEquals("Session30", access);
        access = session.access21();
        Assert.assertEquals("Session21", access);
    }

    @Test
    public void testSessionHome30() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session30RemoteBusiness session = (Session30RemoteBusiness) jndiContext.lookup("java:module/Session30!" + Session30RemoteBusiness.class.getName());
        Assert.assertNotNull(session);
        String access = session.access();
        Assert.assertEquals("Session30", access);

        Session30Home home = (Session30Home) jndiContext.lookup("java:module/Session30!" + Session30Home.class.getName());
        Assert.assertNotNull(home);
        Session30 sessionRemote = home.create();
        Assert.assertNotNull(sessionRemote);
        access = sessionRemote.access();
        Assert.assertEquals("Session30", access);
    }

    @Test
    public void testStatefulRemove() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assert.assertNotNull(home);
        StatefulSession30 session = home.create();
        Assert.assertNotNull(session);
        session.setValue("123");
        String value = session.getValue();
        Assert.assertEquals("123", value);

        EJBObject ejbObject = session;

        Handle handle = session.getHandle();
        Assert.assertNotNull(handle);

        home.remove(handle);

        try {
            session.getValue();
            Assert.assertTrue(false);
        } catch (NoSuchObjectException nsoe) {
            // OK: EJB3.1 7.5.3
        }

        session = home.create();
        Assert.assertNotNull(session);
        session.setValue("123");
        value = session.getValue();
        Assert.assertEquals("123", value);

        session.remove();

        try {
            session.getValue();
            Assert.assertTrue(false);
        } catch (NoSuchObjectException nsoe) {
            // OK: EJB3.1 7.5.3
        }
    }

    @Test
    public void testStatefulSessionHome30() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness session = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());
        Assert.assertNotNull(session);
        session.setValue("testing");
        String value = session.getValue();
        Assert.assertEquals("testing", value);

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assert.assertNotNull(home);
        session = home.create();
        Assert.assertNotNull(session);
        session.setValue("123");
        value = session.getValue();
        Assert.assertEquals("123", value);

        session = home.create("456");
        Assert.assertNotNull(session);
        value = session.getValue();
        Assert.assertEquals("456", value);

        session = home.create("combined", new Integer("789"));
        Assert.assertNotNull(session);
        value = session.getValue();
        Assert.assertEquals("combined789", value);
    }

    @Test
    public void testRemoteHomeAnnotation() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/HomedStatefulSession30!" + StatefulSession30Home.class.getName());
        Assert.assertNotNull(home);
        StatefulSession30 session = home.create();
        Assert.assertNotNull(session);
        session.setValue("123");
        String value = session.getValue();
        Assert.assertEquals("123", value);

        session = home.create("456");
        Assert.assertNotNull(session);
        value = session.getValue();
        Assert.assertEquals("456", value);

        session = home.create("combined", new Integer("789"));
        Assert.assertNotNull(session);
        value = session.getValue();
        Assert.assertEquals("combined789", value);
    }

    @Test
    public void testLocalHomeAnnotation() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness session = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());

        String access = session.accessLocalHome();
        Assert.assertEquals("LocalHome", access);
    }

    @Test
    public void testLocalHome() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness statefulSession = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());
        Assert.assertNotNull(statefulSession);
        String access = statefulSession.accessLocalStateless();
        Assert.assertEquals("Session30", access);

        Session30RemoteBusiness session = (Session30RemoteBusiness) jndiContext.lookup("java:module/Session30!" + Session30RemoteBusiness.class.getName());
        Assert.assertNotNull(session);
        access = session.accessLocalStateful();
        Assert.assertEquals("default", access);

        access = session.accessLocalStateful("testing");
        Assert.assertEquals("testing", access);

        access = session.accessLocalStateful("testing", new Integer(123));
        Assert.assertEquals("testing123", access);
    }

    @Test
    public void testStatefulState() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30RemoteBusiness session1 = (StatefulSession30RemoteBusiness) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30RemoteBusiness.class.getName());
        Assert.assertNotNull(session1);
        session1.setValue("testing");
        Assert.assertEquals("testing", session1.getValue());

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assert.assertNotNull(home);
        StatefulSession30 session3 = home.create();
        Assert.assertNotNull(session3);
        session3.setValue("123");
        Assert.assertEquals("123", session3.getValue());

        StatefulSession30 session4 = home.create();
        Assert.assertNotNull(session4);
        Assert.assertEquals("default", session4.getValue());
        Assert.assertEquals("default", session4.getValue());

        StatefulSession30 session5 = home.create("init");
        Assert.assertNotNull(session5);
        Assert.assertEquals("init", session5.getValue());

        StatefulSession30 session6 = home.create("init", new Integer(123));
        Assert.assertNotNull(session6);
        Assert.assertEquals("init123", session6.getValue());

        StatefulSession30 session7 = home.create("secondinit");
        Assert.assertNotNull(session7);
        Assert.assertEquals("secondinit", session7.getValue());

        Assert.assertEquals("testing", session1.getValue());
        Assert.assertEquals("123", session3.getValue());
        Assert.assertEquals("default", session4.getValue());
        Assert.assertEquals("init", session5.getValue());
        Assert.assertEquals("init123", session6.getValue());
        Assert.assertEquals("secondinit", session7.getValue());
    }

    @Test
    public void testStateful21Interfaces() throws Exception {
        InitialContext jndiContext = new InitialContext();

        StatefulSession30Home home = (StatefulSession30Home) jndiContext.lookup("java:module/StatefulSession30!" + StatefulSession30Home.class.getName());
        Assert.assertNotNull(home);

        EJBMetaData metadata = home.getEJBMetaData();
        Assert.assertNotNull(metadata);
        Assert.assertEquals(StatefulSession30.class, metadata.getRemoteInterfaceClass());

        HomeHandle homeHandle = home.getHomeHandle();
        Assert.assertNotNull(homeHandle);

        EJBHome ejbHome = homeHandle.getEJBHome();
        Assert.assertNotNull(ejbHome);
        metadata = ejbHome.getEJBMetaData();
        Assert.assertNotNull(metadata);
        Assert.assertEquals(StatefulSession30.class, metadata.getRemoteInterfaceClass());

        StatefulSession30 session = home.create();
        Assert.assertNotNull(session);
        ejbHome = session.getEJBHome();
        Assert.assertNotNull(ejbHome);

        Handle handle = session.getHandle();
        Assert.assertNotNull(handle);

        EJBObject ejbObject = handle.getEJBObject();
        Assert.assertNotNull(ejbObject);

        ejbHome = ejbObject.getEJBHome();
        Assert.assertNotNull(ejbHome);

        Handle handle1 = ejbObject.getHandle();
        Assert.assertNotNull(handle1);

        StatefulSession30 session1 = home.create();
        Assert.assertFalse(session.isIdentical(session1));
        Assert.assertTrue(session.isIdentical(session));
    }

    @Test
    public void testStateless21Interfaces() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Session30Home home = (Session30Home) jndiContext.lookup("java:module/Session30!" + Session30Home.class.getName());
        Assert.assertNotNull(home);

        EJBMetaData metadata = home.getEJBMetaData();
        Assert.assertNotNull(metadata);
        Assert.assertEquals(Session30.class.getName(), metadata.getRemoteInterfaceClass().getName());

        HomeHandle homeHandle = home.getHomeHandle();
        Assert.assertNotNull(homeHandle);

        EJBHome ejbHome = homeHandle.getEJBHome();
        Assert.assertNotNull(ejbHome);
        metadata = ejbHome.getEJBMetaData();
        Assert.assertNotNull(metadata);
        Assert.assertEquals(Session30.class.getName(), metadata.getRemoteInterfaceClass().getName());

        Session30 session = home.create();
        Assert.assertNotNull(session);
        ejbHome = session.getEJBHome();
        Assert.assertNotNull(ejbHome);

        Handle handle = session.getHandle();
        Assert.assertNotNull(handle);

        EJBObject ejbObject = handle.getEJBObject();
        Assert.assertNotNull(ejbObject);

        ejbHome = ejbObject.getEJBHome();
        Assert.assertNotNull(ejbHome);

        Handle handle1 = ejbObject.getHandle();
        Assert.assertNotNull(handle1);

        Session30 session1 = home.create();
        Assert.assertTrue(session.isIdentical(session1));
    }
}
