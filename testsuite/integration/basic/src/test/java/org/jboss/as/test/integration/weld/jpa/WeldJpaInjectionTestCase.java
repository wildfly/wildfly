/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * Weld JPA injection tests. Simply tests that a persistence context can be injected into a CDI bean.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldJpaInjectionTestCase {

    private static final String ARCHIVE_NAME = "jpa_OrmTestCase";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"cdiPu\">" +
            "    <description>OrmTestCase Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";


    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(WeldJpaInjectionTestCase.class,
            Employee.class,
            CdiJpaInjectingBean.class
        );

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return jar;
    }

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Inject
    private CdiJpaInjectingBean bean;

    @Test
    public void testOrmXmlDefinedEmployeeEntity() {
        expected.expect(NoResultException.class);
        bean.queryEmployeeName(1);
    }
}
