/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jpa.scoping;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1761
 * <p/>
 * Weld JPA injection tests. Simply tests that a persistence context can be injected into a CDI bean in another deployment unit
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldJpaInjectionScopeTestCase {

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
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "cdiPuScope.ear");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "simple.war");
        war.addClasses(WeldJpaInjectionScopeTestCase.class, CdiJpaInjectingBean.class);
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        ear.addAsModule(war);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "util.jar");
        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        jar.addClass(Employee.class);
        ear.addAsLibrary(jar);
        return ear;
    }

    @Inject
    private CdiJpaInjectingBean bean;

    @Test
    public void testOrmXmlDefinedEmployeeEntity() throws Exception {
        try {
            Employee emp = bean.queryEmployeeName(1);
        } catch (Exception e) {
            if (!(e instanceof NoResultException)) {
                Assert.fail("Expected NoResultException but got " + e);
            }
            return;
        }
        Assert.fail("NoResultException should occur but didn't!");
    }

    // [WFLY-19973] (Weld injected) @PersistenceContext (initial) properties attribute should be processed
    @Test
    public void testInitialPersistenceContextPropertiesAreSet() throws Exception {
        Assert.assertEquals("true",bean.getInitialPropertyValue());
    }

    // [WFLY-19973] (Weld injected) @PersistenceContext (later set) properties attribute should be available
    @Test
    public void testEntityManagerPropertiesAreSaved() throws Exception {
        bean.setAdditionalPropertyValue("WeldJpaInjectionScopeTestCase.testproperty");
        Assert.assertEquals("WeldJpaInjectionScopeTestCase.testproperty",bean.getAdditionalPropertyValue());
    }

    // [WFLY-19973] (Weld injected) @PersistenceContext (later set) ensure that property can be added to empty property map
    @Test
    public void testEntityManagerPropertiesEmptyCase() throws Exception {
        Assert.assertEquals("AddedToEmptyHashMap", bean.addPropertyToEmptyPropertyMap("AddedToEmptyHashMap"));
    }

}

