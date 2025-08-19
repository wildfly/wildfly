/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.hibernate.orm.internalcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.cache.InternalCache;
import org.hibernate.internal.util.cache.InternalCacheFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.preview.hibernate.search.coordination.HibernateSearchOutboxPollingTestCase;

/**
 * Tests that Hibernate ORM is being injected with the Caffeine based implementation
 * of InternalCacheFactory.
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class InternalCacheServiceTestCase {

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, HibernateSearchOutboxPollingTestCase.class.getSimpleName() + ".war")
                .addClass(InternalCacheServiceTestCase.class)
                .addClass(Employee.class)
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset persistenceXml() {
        return new StringAsset(Descriptors.create(PersistenceDescriptor.class)
                .version("3.2")
                .createPersistenceUnit()
                .name("internalCacheIt")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .getOrCreateProperties()
                    .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .up().up()
                .exportAsString()
        );
    }

    @PersistenceUnit(unitName = "internalCacheIt")
    public EntityManagerFactory emf;

    @Test
    public void test() throws InterruptedException {
        assertNotNull("Failed to inject EntityManagerFactory", emf);
        assertNotNull("Failed to extract SessionFactoryImplementor from EntityManagerFactory", emf.unwrap(SessionFactoryImplementor.class));
        InternalCacheFactory internalCacheFactory = emf.unwrap(SessionFactoryImplementor.class).getServiceRegistry().requireService(InternalCacheFactory.class);
        InternalCache<Object, Object> internalCache = internalCacheFactory.createInternalCache(10);//any number will do
        assertEquals(internalCache.getClass().getName(), "org.wildfly.persistence.jipijapa.hibernate7.service.WildFlyCustomInternalCache");
    }
}
