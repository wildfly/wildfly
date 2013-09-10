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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import javax.batch.runtime.JobExecution;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
