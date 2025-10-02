/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.persistence.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that in a CDI enabled deployment that the legacy {@link PersistenceContext} and {@link PersistenceUnit} still
 * work and don't cause deployment issues.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class LegacyInjectionTestCase {

    private static final String DEPLOYMENT_NAME = LegacyInjectionTestCase.class.getSimpleName() + ".war";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(Employee.class)
                .addAsWebInfResource(LegacyInjectionTestCase.class.getPackage(), "legacy-persistence.xml", "classes/META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @PersistenceContext(unitName = "pu1")
    private EntityManager emPu1;

    @PersistenceContext(unitName = "pu2")
    private EntityManager emPu2;

    @PersistenceUnit(unitName = "pu1")
    private EntityManagerFactory emfPu1;

    @PersistenceUnit(unitName = "pu2")
    private EntityManagerFactory emfPu2;

    @Test
    public void checkPu1EntityManager() {
        Assert.assertNotNull(emPu1);
        Assert.assertEquals(DEPLOYMENT_NAME + "#pu1", emPu1.getEntityManagerFactory().getName());
    }

    @Test
    public void checkPu2EntityManager() {
        Assert.assertNotNull(emPu2);
        Assert.assertEquals(DEPLOYMENT_NAME + "#pu2", emPu2.getEntityManagerFactory().getName());
    }

    @Test
    public void checkPu1EntityManagerFactory() {
        Assert.assertNotNull(emfPu1);
        Assert.assertEquals(DEPLOYMENT_NAME + "#pu1", emfPu1.getName());
    }

    @Test
    public void checkPu2EntityManagerFactory() {
        Assert.assertNotNull(emfPu2);
        Assert.assertEquals(DEPLOYMENT_NAME + "#pu2", emfPu2.getName());
    }
}
