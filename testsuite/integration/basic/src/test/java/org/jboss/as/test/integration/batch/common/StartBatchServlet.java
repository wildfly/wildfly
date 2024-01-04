/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.shared.TimeoutUtil;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("/start")
public class StartBatchServlet extends AbstractBatchServlet {

    protected static final int DEFAULT_TIMEOUT = 60000;
    protected static final String TIMEOUT_PARAM = "timeout";
    public static final String WAIT_FOR_COMPLETION = "wait";

    @Inject
    private BatchExecutionService service;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        // Get the batch file name
        final String jobXml = req.getParameter(JOB_XML_PARAMETER);

        final String timeoutString = req.getParameter(TIMEOUT_PARAM);
        final String wait = req.getParameter(WAIT_FOR_COMPLETION);

        if (jobXml == null) {
            throw new IllegalStateException(String.format("%s is a required parameter", JOB_XML_PARAMETER));
        }

        final Properties params = parseParams(req, Arrays.asList(TIMEOUT_PARAM, WAIT_FOR_COMPLETION));

        final JobExecution jobExecution = service.start(jobXml, params);
        long timeout = TimeoutUtil.adjust(timeoutString == null ? DEFAULT_TIMEOUT : Integer.parseInt(timeoutString));
        long sleep = 100L;
        final boolean waitForCompletion = (wait == null || Boolean.parseBoolean(wait));
        boolean b = waitForCompletion;
        while (b) {
            switch (jobExecution.getBatchStatus()) {
                case STARTED:
                case STARTING:
                case STOPPING:
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleep);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    timeout -= sleep;
                    sleep = Math.max(sleep / 2, 100L);
                    break;
                default:
                    b = false;
                    break;
            }
            if (timeout <= 0) {
                throw new IllegalStateException(String.format("Batch job '%s' did not complete within allotted time.", jobXml));
            }
        }
        if (waitForCompletion)
            write(resp, jobExecution);
    }
}
