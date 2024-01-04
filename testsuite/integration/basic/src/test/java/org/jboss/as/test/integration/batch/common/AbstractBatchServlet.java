/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import jakarta.batch.runtime.JobExecution;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractBatchServlet extends HttpServlet {

    public static final String JOB_XML_PARAMETER = "jobXml";

    protected Properties parseParams(final HttpServletRequest request, final Collection<String> ignore) {
        final Collection<String> localIgnore = new ArrayList<String>(Arrays.asList(JOB_XML_PARAMETER));
        localIgnore.addAll(ignore);
        final Properties params = new Properties();
        final Enumeration<String> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            final String name = e.nextElement();
            if (localIgnore.contains(name)) continue;
            final String value = request.getParameter(name);
            params.setProperty(name, value);
        }
        return params;
    }

    protected void write(final HttpServletResponse response, final JobExecution jobExecution) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(JobExecutionMarshaller.marshall(jobExecution));
    }
}
