/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.analyzer.transactiontimeout;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.JobExecutionMarshaller;
import org.jboss.as.test.integration.batch.common.StartBatchServlet;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.batch.runtime.JobExecution;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Test for JBEAP-4862
 *
 * - uses the {@link TransactionTimeoutSetupStep} to set up small transaction timeout.
 * - the batch job is slow and takes more than the timeout failing the analyzer
 * - using jberet.analyzer.txDisabled makes the test pass
 */
@RunWith(Arquillian.class)
@ServerSetup(TransactionTimeoutSetupStep.class)
public class AnalyzerTransactionTimeoutTestCase extends AbstractBatchTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("timeout-analyzer.war", AnalyzerTransactionTimeoutTestCase.class.getPackage(), "analyzer-job.xml")
                .addPackage(AnalyzerTransactionTimeoutTestCase.class.getPackage())
                .addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller\n"), "META-INF/MANIFEST.MF");
    }


    @Test
    @RunAsClient
    public void testTransactionTimeoutDisabled(@ArquillianResource URL url) throws Exception {
        final UrlBuilder builder = UrlBuilder.of(url, "start");
        builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "analyzer-job");

        final String result = performCall(builder.build(), 20);

        final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);

        assertEquals("COMPLETED", jobExecution.getBatchStatus().name());
    }
}
