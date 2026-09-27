/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.hibernate.search.batch;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Inject;

/**
 * Test the ability for applications to use Hibernate Search's Jakarta Batch integration and run batch job for mass indexing
 * bundled within Hibernate Search. This feature is considered incubating and thus not included in WildFly "standard" (only in
 * "preview").
 */
@RunWith(Arquillian.class)
public class HibernateSearchBatchTestCase {

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static Archive<?> createTestArchive() {

        // TODO maybe just use managed=false and deploy in the @BeforeClass / undeploy in an @AfterClass
        if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
            return AssumeTestGroupUtil.emptyWar(HibernateSearchBatchTestCase.class.getSimpleName());
        }

        return ShrinkWrap.create(WebArchive.class, HibernateSearchBatchTestCase.class.getSimpleName() + ".war")
                .addClass(HibernateSearchBatchTestCase.class)
                .addClasses(SearchBean.class, IndexedEntity.class, TimeoutUtil.class)
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class).version("2.0").createPersistenceUnit()
                .name("primary").jtaDataSource("java:jboss/datasources/ExampleDS").getOrCreateProperties().createProperty()
                .name("hibernate.hbm2ddl.auto").value("create-drop").up().createProperty()
                .name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up().createProperty()
                .name("hibernate.search.backend.type").value("lucene").up().createProperty()
                .name("hibernate.search.backend.lucene_version").value("LUCENE_CURRENT").up().createProperty()
                .name("hibernate.search.backend.directory.type").value("local-heap").up().createProperty()
                .name("hibernate.search.indexing.plan.synchronization.strategy").value("read-sync").up().createProperty()
                // disable listeners so no indexing happens on persist, we will index things through a batch job:
                .name("hibernate.search.indexing.listeners.enabled").value("false").up().up().up().exportAsString();
        return new StringAsset(persistenceXml);
    }

    @Inject
    private SearchBean searchBean;

    @Before
    public void setUp() throws Exception {
        searchBean.create(1_000);
    }

    @Test
    public void test() throws InterruptedException {
        // indexing is disabled, so there should be no results:
        assertEquals(0, searchBean.search("text").size());

        // run batch job:
        Properties jobProps = MassIndexingJob.parameters().forEntities(IndexedEntity.class).build();

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start(MassIndexingJob.NAME, jobProps);

        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        waitForTermination(jobExecution, 60);

        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        // after the batch job all the entities should be indexed:
        assertEquals(1_000, searchBean.search("text").size());
    }

    public static void waitForTermination(final JobExecution jobExecution, final int timeout) {
        long waitTimeout = TimeoutUtil.adjust(timeout * 1000);
        long sleep = 100L;
        while (true) {
            switch (jobExecution.getBatchStatus()) {
                case STARTED:
                case STARTING:
                case STOPPING:
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleep);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    waitTimeout -= sleep;
                    sleep = Math.max(sleep / 2, 100L);
                    break;
                default:
                    return;
            }
            if (waitTimeout <= 0) {
                throw new IllegalStateException("Batch job did not complete within allotted time.");
            }
        }
    }

}
