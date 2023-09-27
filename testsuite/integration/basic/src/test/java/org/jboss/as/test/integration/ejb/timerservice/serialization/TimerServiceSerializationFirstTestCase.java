/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.serialization;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Two phase test for timer serialization. This test serializes a class in the info field where there does not exist a single
 * module class loader that has access to every class in the serialized object graph.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServiceSerializationFirstTestCase {

    /**
     * must match between the two tests.
     */
    public static final String ARCHIVE_NAME = "testTimerServiceSerialization.ear";

    @Deployment
    public static Archive<?> deploy() {
        return createTestArchive(TimerServiceSerializationFirstTestCase.class);
    }

    public static Archive<?> createTestArchive(Class<?> testClass) {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "a.jar");
        jar.addClasses(InfoA.class, TimerServiceSerializationBean.class, testClass);
        jar.addAsManifestResource(new StringAsset("Class-Path: b.jar \n"), "MANIFEST.MF");
        ear.addAsModule(jar);

        jar = ShrinkWrap.create(JavaArchive.class, "b.jar");
        jar.addClasses(InfoB.class);
        jar.addAsManifestResource(new StringAsset("Class-Path: c.jar \n"), "MANIFEST.MF");
        jar.addAsManifestResource(new StringAsset(EJB_JAR), "ejb-jar.xml");
        ear.addAsModule(jar);

        jar = ShrinkWrap.create(JavaArchive.class, "c.jar");
        jar.addClasses(InfoC.class);
        jar.addAsManifestResource(new StringAsset(EJB_JAR), "ejb-jar.xml");
        ear.addAsModule(jar);

        ear.addAsManifestResource(new StringAsset(
                "<jboss-deployment-structure><ear-subdeployments-isolated>true</ear-subdeployments-isolated></jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");

        return ear;
    }

    @Test
    public void testCreateTimerWithInfo() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimerServiceSerializationBean bean = (TimerServiceSerializationBean) ctx.lookup("java:module/" + TimerServiceSerializationBean.class.getSimpleName());
        bean.createTimer();
        InfoA info = TimerServiceSerializationBean.awaitTimerCall();
        Assert.assertNotNull(info);
        Assert.assertNotNull(info.infoB);
        Assert.assertNotNull(info.infoB.infoC);
    }

    public static final String EJB_JAR = "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"\n" +
            "         version=\"3.1\">\n" +
            "</ejb-jar>";

}
