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
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;

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
                .addAsResource(manifest(), "META-INF/MANIFEST.MF").addAsResource(persistenceXml(), "META-INF/persistence.xml")
                // we shouldn't really need this xml added to the war, since it is packaged with Search,
                // but: WFLYBATCH000006: Could not find the job XML file in the deployment: hibernate-search-mass-indexing
                .addAsResource(jobXML(), "META-INF/batch-jobs/hibernate-search-mass-indexing.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset jobXML() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!--\n"
                + " ~ Hibernate Search, full-text search for your domain model\n" + " ~\n"
                + " ~ License: GNU Lesser General Public License (LGPL), version 2.1 or later\n"
                + " ~ See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.\n" + "  -->\n"
                + "<job id=\"hibernate-search-mass-indexing\" xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/jobXML_2_0.xsd\" version=\"2.0\">\n"
                + "\n" + "    <listeners>\n"
                + "        <listener ref=\"org.hibernate.search.jakarta.batch.core.massindexing.spi.JobContextSetupListener\">\n"
                + "            <properties>\n"
                + "                <property name=\"entityManagerFactoryNamespace\" value=\"#{jobParameters['entityManagerFactoryNamespace']}\" />\n"
                + "                <property name=\"entityManagerFactoryReference\" value=\"#{jobParameters['entityManagerFactoryReference']}\" />\n"
                + "                <property name=\"entityTypes\" value=\"#{jobParameters['entityTypes']}\" />\n" + "\n"
                + "                <property name=\"maxThreads\" value=\"#{jobParameters['maxThreads']}\" />\n"
                + "                <property name=\"maxResultsPerEntity\" value=\"#{jobParameters['maxResultsPerEntity']}\" />\n"
                + "                <property name=\"idFetchSize\" value=\"#{jobParameters['idFetchSize']}\" />\n"
                + "                <property name=\"entityFetchSize\" value=\"#{jobParameters['entityFetchSize']}\" />\n"
                + "                <property name=\"cacheMode\" value=\"#{jobParameters['cacheMode']}\" />\n"
                + "                <property name=\"mergeSegmentsOnFinish\" value=\"#{jobParameters['mergeSegmentsOnFinish']}\" />\n"
                + "                <property name=\"mergeSegmentsAfterPurge\" value=\"#{jobParameters['mergeSegmentsAfterPurge']}\" />\n"
                + "                <property name=\"purgeAllOnStart\" value=\"#{jobParameters['purgeAllOnStart']}\" />\n"
                + "                <property name=\"dropAndCreateSchemaOnStart\" value=\"#{jobParameters['dropAndCreateSchemaOnStart']}\" />\n"
                + "                <property name=\"sessionClearInterval\" value=\"#{jobParameters['sessionClearInterval']}\" />\n"
                + "                <property name=\"checkpointInterval\" value=\"#{jobParameters['checkpointInterval']}\" />\n"
                + "                <property name=\"rowsPerPartition\" value=\"#{jobParameters['rowsPerPartition']}\" />\n"
                + "                <property name=\"reindexOnlyHql\" value=\"#{jobParameters['reindexOnlyHql']}\" />\n"
                + "                <property name=\"reindexOnlyParameters\" value=\"#{jobParameters['reindexOnlyParameters']}\" />\n"
                + "            </properties>\n" + "        </listener>\n" + "    </listeners>\n" + "\n"
                + "    <step id=\"beforeChunk\" next=\"produceLuceneDoc\">\n"
                + "        <batchlet ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.beforechunk.impl.BeforeChunkBatchlet\">\n"
                + "            <properties>\n"
                + "                <property name=\"optimizeAfterPurge\" value=\"#{jobParameters['optimizeAfterPurge']}\" />\n"
                + "                <property name=\"purgeAllOnStart\" value=\"#{jobParameters['purgeAllOnStart']}\" />\n"
                + "                <property name=\"dropAndCreateSchemaOnStart\" value=\"#{jobParameters['dropAndCreateSchemaOnStart']}\" />\n"
                + "                <property name=\"tenantId\" value=\"#{jobParameters['tenantId']}\" />\n"
                + "            </properties>\n" + "        </batchlet>\n" + "    </step>\n" + "\n"
                + "    <step id=\"produceLuceneDoc\" next=\"afterChunk\">\n" + "        <listeners>\n"
                + "            <listener ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.impl.StepProgressSetupListener\">\n"
                + "                <properties>\n"
                + "                    <property name=\"tenantId\" value=\"#{jobParameters['tenantId']}\" />\n"
                + "                    <property name=\"customQueryHQL\" value=\"#{jobParameters['customQueryHQL']}\" />\n"
                + "                </properties>\n" + "            </listener>\n" + "        </listeners>\n"
                + "        <!-- Here we use the property from the partition plan, so that defaults are correctly applied -->\n"
                + "        <chunk item-count=\"#{partitionPlan['checkpointInterval']}\">\n"
                + "            <reader ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.spi.EntityIdReader\">\n"
                + "                <properties>\n"
                + "                \t<!-- Used to re-create the job context data as necessary -->\n"
                + "\t                <property name=\"entityManagerFactoryNamespace\" value=\"#{jobParameters['entityManagerFactoryNamespace']}\" />\n"
                + "\t                <property name=\"entityManagerFactoryReference\" value=\"#{jobParameters['entityManagerFactoryReference']}\" />\n"
                + "\t                <property name=\"entityTypes\" value=\"#{jobParameters['entityTypes']}\" />\n"
                + "\t                <property name=\"customQueryCriteria\" value=\"#{jobParameters['customQueryCriteria']}\" />\n"
                + "\n" + "                    <property name=\"entityName\" value=\"#{partitionPlan['entityName']}\" />\n"
                + "                    <property name=\"partitionId\" value=\"#{partitionPlan['partitionId']}\" />\n"
                + "                    <property name=\"lowerBound\" value=\"#{partitionPlan['lowerBound']}\" />\n"
                + "                    <property name=\"upperBound\" value=\"#{partitionPlan['upperBound']}\" />\n"
                + "                    <property name=\"indexScope\" value=\"#{partitionPlan['indexScope']}\" />\n"
                + "                    <property name=\"tenantId\" value=\"#{jobParameters['tenantId']}\" />\n"
                + "                    <property name=\"idFetchSize\" value=\"#{jobParameters['entityFetchSize']}\" />\n"
                + "                    <!-- Here we use the property from the partition plan, so that defaults are correctly applied -->\n"
                + "                    <property name=\"checkpointInterval\" value=\"#{partitionPlan['checkpointInterval']}\" />\n"
                + "                    <property name=\"reindexOnlyHql\" value=\"#{jobParameters['reindexOnlyHql']}\" />\n"
                + "                    <property name=\"reindexOnlyParameters\" value=\"#{jobParameters['reindexOnlyParameters']}\" />\n"
                + "                    <property name=\"maxResultsPerEntity\" value=\"#{jobParameters['maxResultsPerEntity']}\" />\n"
                + "                </properties>\n" + "            </reader>\n"
                + "            <writer ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.impl.EntityWriter\">\n"
                + "                <properties>\n"
                + "                    <property name=\"entityName\" value=\"#{partitionPlan['entityName']}\" />\n"
                + "                    <property name=\"partitionId\" value=\"#{partitionPlan['partitionId']}\" />\n"
                + "                    <property name=\"tenantId\" value=\"#{jobParameters['tenantId']}\" />\n"
                + "                    <property name=\"cacheMode\" value=\"#{jobParameters['cacheMode']}\" />\n"
                + "                    <!-- Here we use the property from the partition plan, so that defaults are correctly applied -->\n"
                + "                    <property name=\"checkpointInterval\" value=\"#{partitionPlan['checkpointInterval']}\" />\n"
                + "                    <property name=\"entityFetchSize\" value=\"#{jobParameters['entityFetchSize']}\" />\n"
                + "                </properties>\n" + "            </writer>\n" + "        </chunk>\n" + "        <partition>\n"
                + "            <mapper ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.impl.HibernateSearchPartitionMapper\">\n"
                + "                <properties>\n"
                + "                    <property name=\"tenantId\" value=\"#{jobParameters['tenantId']}\" />\n"
                + "                    <property name=\"reindexOnlyHql\" value=\"#{jobParameters['reindexOnlyHql']}\" />\n"
                + "                    <property name=\"reindexOnlyParameters\" value=\"#{jobParameters['reindexOnlyParameters']}\" />\n"
                + "                    <property name=\"maxThreads\" value=\"#{jobParameters['maxThreads']}\" />\n"
                + "                    <property name=\"maxResultsPerEntity\" value=\"#{jobParameters['maxResultsPerEntity']}\" />\n"
                + "                    <property name=\"checkpointInterval\" value=\"#{jobParameters['checkpointInterval']}\" />\n"
                + "                    <property name=\"rowsPerPartition\" value=\"#{jobParameters['rowsPerPartition']}\" />\n"
                + "                </properties>\n" + "            </mapper>\n"
                + "            <collector ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.impl.ProgressCollector\" />\n"
                + "            <analyzer ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.impl.ProgressAggregator\" />\n"
                + "        </partition>\n" + "    </step>\n" + "\n" + "    <step id=\"afterChunk\">\n"
                + "        <batchlet ref=\"org.hibernate.search.jakarta.batch.core.massindexing.step.afterchunk.impl.AfterChunkBatchlet\">\n"
                + "            <properties>\n"
                + "                <property name=\"optimizeOnFinish\" value=\"#{jobParameters['optimizeOnFinish']}\" />\n"
                + "                <property name=\"tenantId\" value=\"#{jobParameters['tenantId']}\" />\n"
                + "            </properties>\n" + "        </batchlet>\n" + "    </step>\n" + "\n" + "</job>\n");
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

    private static Asset manifest() {
        String manifest = Descriptors.create(ManifestDescriptor.class)
                .attribute("Dependencies", "org.hibernate.search.jakarta.batch").exportAsString();
        return new StringAsset(manifest);
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
