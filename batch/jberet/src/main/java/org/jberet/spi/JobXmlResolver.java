/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jberet.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * An interface used to resolve job XML content.
 *
 * <p>
 * Both the {@link #getJobXmlNames(ClassLoader)} and {@link #resolveJobName(String, ClassLoader)} methods are optional.
 * The intention is implementations can use the values returned to query information about specific the job XML files.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) remove this when JBeret is upgraded
public interface JobXmlResolver {

    /**
     * The default {@code META-INF/batch-jobs/} path.
     */
    String DEFAULT_PATH = "META-INF/batch-jobs/";

    /**
     * Locates the job XML and creates a stream to the contents.
     *
     * @param jobXml      the name of the job XML with a {@code .xml} suffix
     * @param classLoader the class loader for the application
     *
     * @return a stream of the job XML or {@code null} if the job XML content was not found
     *
     * @throws java.io.IOException if an error occurs creating the stream
     */
    InputStream resolveJobXml(String jobXml, ClassLoader classLoader) throws IOException;

    /**
     * Optionally returns a list of job XML names that are allowed to be used.
     *
     * <p>
     * An empty collection should be returned if job names can not be resolved.
     * </p>
     *
     * @param classLoader the class loader for the application
     *
     * @return the job XML names or an empty collection
     */
    Collection<String> getJobXmlNames(final ClassLoader classLoader);

    /**
     * Optionally resolves the job name from the job XML.
     *
     * <p>
     * A {@code null} value can be returned if the name cannot be resolved.
     * </p>
     *
     * @param jobXml      the name of the xml XML with a {@code .xml} suffix
     * @param classLoader the class loader for the application
     *
     * @return the name of the job if found or {@code null} if not found
     */
    String resolveJobName(String jobXml, ClassLoader classLoader);
}
