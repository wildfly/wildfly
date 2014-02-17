/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.batch.runtime.JobExecution;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.shared.TimeoutUtil;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("/start")
public class StartBatchServlet extends AbstractBatchServlet {

    protected static final int DEFAULT_TIMEOUT = 60000;
    protected static final String TIMEOUT_PARAM = "timeout";
    public static final String WAIT_FOR_COMPLETION = "wait";

    @EJB
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
