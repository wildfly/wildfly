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
package org.wildfly.test.preview.hibernate.search.coordination;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Test the ability for applications to use outbox-polling coordination,
 * provided they add the appropriate module dependency,
 * because that feature is still considered incubating and thus not included in WildFly "standard" (only in "preview").
 *
 * This test relies on a Lucene backend because it simpler to set up (no need for an Elasticsearch container),
 * but users would generally rely on an Elasticsearch backend (because they have multiple instances of their application,
 * and thus can't just rely on the filesystem for index storage).
 */
@RunWith(Arquillian.class)
public class HibernateSearchOutboxPollingTestCase {

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static Archive<?> createTestArchive() {

        // TODO maybe just use managed=false and deploy in the @BeforeClass / undeploy in an @AfterClass
        if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
            return AssumeTestGroupUtil.emptyWar(HibernateSearchOutboxPollingTestCase.class.getSimpleName());
        }

        return ShrinkWrap.create(WebArchive.class, HibernateSearchOutboxPollingTestCase.class.getSimpleName() + ".war")
                .addClass(HibernateSearchOutboxPollingTestCase.class)
                .addClasses(SearchBean.class, IndexedEntity.class, TimeoutUtil.class)
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
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.backend.type").value("lucene").up()
                .createProperty().name("hibernate.search.backend.lucene_version").value("LUCENE_CURRENT").up()
                .createProperty().name("hibernate.search.backend.directory.type").value("local-heap").up()
                .createProperty().name("hibernate.search.coordination.strategy").value("outbox-polling").up()
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
    }

    @Inject
    private SearchBean searchBean;

    @Test
    public void test() throws InterruptedException {
        // Check that we ARE using outbox-polling coordination
        awaitAssertion(() -> assertEquals(1, searchBean.findAgentNames().size()));

        // Check that indexing through the outbox works correctly
        assertEquals(0, searchBean.search("mytoken").size());
        searchBean.create("This is MYToken");
        awaitAssertion(() -> assertEquals(1, searchBean.search("mytoken").size()));
    }

    private static void awaitAssertion(Runnable assertion) throws InterruptedException {
        long end = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
        AssertionError lastError;
        do {
            try {
                assertion.run();
                return; // The assertion passed
            } catch (AssertionError e) {
                lastError = e;
            }
            Thread.sleep(100);
        }
        while (System.currentTimeMillis() < end);

        throw lastError;
    }

}
