/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.flow;

import java.net.URL;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.JobExecutionMarshaller;
import org.jboss.as.test.integration.batch.common.StartBatchServlet;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class FlowTestCase extends AbstractBatchTestCase {
    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("batch-flow.war", FlowTestCase.class.getPackage(), "flow.xml")
                .addPackage(FlowTestCase.class.getPackage());
    }

    @Test
    public void flow() throws Exception {
        final UrlBuilder builder = UrlBuilder.of(url, "start");
        builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "flow");
        builder.addParameter("job-param", "job-param");
        final String result = performCall(builder.build());
        final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
    }

}
