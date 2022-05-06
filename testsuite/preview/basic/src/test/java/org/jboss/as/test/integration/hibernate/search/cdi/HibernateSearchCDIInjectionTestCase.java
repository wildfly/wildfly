/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search.cdi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.CDIBeansPackage;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.InternationalizedValue;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.model.EntityWithCDIAwareBridges;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.model.EntityWithCDIAwareBridgesDao;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class HibernateSearchCDIInjectionTestCase {

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static Archive<?> createTestArchive() throws Exception {
        return ShrinkWrap.create(WebArchive.class, HibernateSearchCDIInjectionTestCase.class.getSimpleName() + ".war")
                .addClass(HibernateSearchCDIInjectionTestCase.class)
                .addPackages(true /* recursive */, CDIBeansPackage.class.getPackage())
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("primary")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .createProperty().name("hibernate.search.default.lucene_version").value("LUCENE_CURRENT").up()
                .createProperty().name("hibernate.search.default.directory_provider").value("local-heap").up()
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
    }

    @Inject
    private EntityWithCDIAwareBridgesDao dao;

    @Before
    @After
    public void cleanupDatabase() {
        dao.deleteAll();
    }

    @Test
    public void injectedFieldBridge() {
        assertEquals(0, dao.searchFieldBridge("bonjour").size());
        assertEquals(0, dao.searchFieldBridge("hello").size());
        assertEquals(0, dao.searchFieldBridge("hallo").size());
        assertEquals(0, dao.searchFieldBridge("au revoir").size());

        EntityWithCDIAwareBridges entity = new EntityWithCDIAwareBridges();
        entity.setInternationalizedValue(InternationalizedValue.HELLO);
        dao.create(entity);
        assertThat(dao.searchFieldBridge("bonjour"), hasItems(entity.getId()));
        assertThat(dao.searchFieldBridge("hello"), hasItems(entity.getId()));
        assertThat(dao.searchFieldBridge("hallo"), hasItems(entity.getId()));
        assertEquals(0, dao.searchFieldBridge("au revoir").size());

        EntityWithCDIAwareBridges entity2 = new EntityWithCDIAwareBridges();
        entity2.setInternationalizedValue(InternationalizedValue.GOODBYE);
        dao.create(entity2);
        assertThat(dao.searchFieldBridge("bonjour"), hasItems(entity.getId()));
        assertThat(dao.searchFieldBridge("hello"), hasItems(entity.getId()));
        assertThat(dao.searchFieldBridge("hallo"), hasItems(entity.getId()));
        assertThat(dao.searchFieldBridge("au revoir"), hasItems(entity2.getId()));

        dao.delete(entity);
        assertEquals(0, dao.searchFieldBridge("bonjour").size());
        assertEquals(0, dao.searchFieldBridge("hello").size());
        assertEquals(0, dao.searchFieldBridge("hallo").size());
        assertThat(dao.searchFieldBridge("au revoir"), hasItems(entity2.getId()));
    }

    @Test
    public void injectedClassBridge() {
        assertEquals(0, dao.searchClassBridge("bonjour").size());
        assertEquals(0, dao.searchClassBridge("hello").size());
        assertEquals(0, dao.searchClassBridge("hallo").size());
        assertEquals(0, dao.searchClassBridge("au revoir").size());

        EntityWithCDIAwareBridges entity = new EntityWithCDIAwareBridges();
        entity.setInternationalizedValue(InternationalizedValue.HELLO);
        dao.create(entity);
        assertThat(dao.searchClassBridge("bonjour"), hasItems(entity.getId()));
        assertThat(dao.searchClassBridge("hello"), hasItems(entity.getId()));
        assertThat(dao.searchClassBridge("hallo"), hasItems(entity.getId()));
        assertEquals(0, dao.searchClassBridge("au revoir").size());

        EntityWithCDIAwareBridges entity2 = new EntityWithCDIAwareBridges();
        entity2.setInternationalizedValue(InternationalizedValue.GOODBYE);
        dao.create(entity2);
        assertThat(dao.searchClassBridge("bonjour"), hasItems(entity.getId()));
        assertThat(dao.searchClassBridge("hello"), hasItems(entity.getId()));
        assertThat(dao.searchClassBridge("hallo"), hasItems(entity.getId()));
        assertThat(dao.searchClassBridge("au revoir"), hasItems(entity2.getId()));

        dao.delete(entity);
        assertEquals(0, dao.searchClassBridge("bonjour").size());
        assertEquals(0, dao.searchClassBridge("hello").size());
        assertEquals(0, dao.searchClassBridge("hallo").size());
        assertThat(dao.searchClassBridge("au revoir"), hasItems(entity2.getId()));
    }
}
