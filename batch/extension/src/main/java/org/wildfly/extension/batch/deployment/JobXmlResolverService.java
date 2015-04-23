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

package org.wildfly.extension.batch.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobParser;
import org.jberet.spi.JobXmlResolver;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.batch._private.BatchLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JobXmlResolverService implements Service<JobXmlResolver>, JobXmlResolver {
    private final Set<JobXmlResolver> jobXmlResolvers;
    private final Map<String, String> cachedJobInfo;
    private final Map<String, VirtualFile> jobXmlFiles;
    private final ClassLoader classLoader;

    public JobXmlResolverService() {
        classLoader = null;
        cachedJobInfo = Collections.emptyMap();
        jobXmlResolvers = Collections.emptySet();
        jobXmlFiles = Collections.emptyMap();
    }

    public JobXmlResolverService(final ClassLoader classLoader, final Collection<VirtualFile> jobXmlFiles) {
        this.classLoader = classLoader;
        cachedJobInfo = new LinkedHashMap<>();
        jobXmlResolvers = new LinkedHashSet<>();
        this.jobXmlFiles = new LinkedHashMap<>(jobXmlFiles.size());
        for (VirtualFile jobXmlFile : jobXmlFiles) {
            this.jobXmlFiles.put(jobXmlFile.getName(), jobXmlFile);
        }
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        if (classLoader != null) {
            // Load the user defined resolvers
            for (JobXmlResolver resolver : ServiceLoader.load(JobXmlResolver.class, classLoader)) {
                jobXmlResolvers.add(resolver);
                for (String jobXml : resolver.getJobXmlNames(classLoader)) {
                    cachedJobInfo.put(jobXml, resolver.resolveJobName(jobXml, classLoader));
                }
            }

            // Load the default names
            for (Map.Entry<String, VirtualFile> entry : jobXmlFiles.entrySet()) {
                try {
                    // Parsing the entire job XML seems excessive to just get the job name. There are two reasons for this:
                    //  1) If an error occurs during parsing there's no real need to register the job resource
                    //  2) Using the implementation parser seems less error prone for future-proofing
                    final Job job = JobParser.parseJob(entry.getValue().openStream(), classLoader, new XMLResolver() {
                        // this is essentially what JBeret does, but it's ugly. JBeret might need an API to handle this
                        @Override
                        public Object resolveEntity(final String publicID, final String systemID, final String baseURI, final String namespace) throws XMLStreamException {
                            try {
                                return (jobXmlFiles.containsKey(systemID) ? jobXmlFiles.get(systemID).openStream() : null);
                            } catch (IOException e) {
                                throw new XMLStreamException(e);
                            }
                        }
                    });
                    cachedJobInfo.put(entry.getKey(), job.getId());
                } catch (XMLStreamException | IOException e) {
                    // Report the possible error as we don't want to fail the deployment. The job may never be run.
                    BatchLogger.LOGGER.invalidJobXmlFile(entry.getKey());
                }
            }
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        jobXmlResolvers.clear();
        jobXmlFiles.clear();
        cachedJobInfo.clear();
    }

    @Override
    public JobXmlResolver getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        synchronized (this) {
            if (jobXmlFiles.isEmpty() && jobXmlResolvers.isEmpty()) {
                return null;
            }
            for (JobXmlResolver resolver : jobXmlResolvers) {
                final InputStream in = resolver.resolveJobXml(jobXml, classLoader);
                if (in != null) {
                    return in;
                }
            }
        }
        final VirtualFile file;
        synchronized (this) {
            file = jobXmlFiles.get(jobXml);
        }
        if (file == null) {
            return null;
        }
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    try {
                        return file.openStream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return file.openStream();
    }

    @Override
    public synchronized Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        return cachedJobInfo.keySet();
    }

    @Override
    public synchronized String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        return cachedJobInfo.get(jobXml);
    }
}
