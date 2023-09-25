/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.persistence;

import jakarta.ejb.EJB;

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
