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

package org.wildfly.extension.batch.jberet.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobParser;
import org.jberet.spi.JobXmlResolver;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.wildfly.extension.batch.jberet._private.BatchLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@linkplain JobXmlResolver job XML resolver} for WildFly. A deployments resolvers are loaded via a
 * {@link ServiceLoader} and processed before XML found in the deployment itself.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class WildFlyJobXmlResolver implements JobXmlResolver {

    private final Set<JobXmlResolver> jobXmlResolvers;
    private final Map<String, String> resolvedJobs;
    private final Map<String, VirtualFile> jobXmlFiles;

    private WildFlyJobXmlResolver(final Map<String, VirtualFile> jobXmlFiles) {
        resolvedJobs = new LinkedHashMap<>();
        jobXmlResolvers = new LinkedHashSet<>();
        this.jobXmlFiles = jobXmlFiles;
    }

    public static WildFlyJobXmlResolver of(final ClassLoader classLoader, final List<ResourceRoot> resources) throws DeploymentUnitProcessingException {
        final Map<String, VirtualFile> foundJobXmlFiles = new LinkedHashMap<>();
        for (ResourceRoot r : resources) {
            final VirtualFile root = r.getRoot();
            try {
                addJobXmlFiles(foundJobXmlFiles, root.getChild(DEFAULT_PATH));
            } catch (IOException e) {
                throw BatchLogger.LOGGER.errorProcessingBatchJobsDir(e);
            }
        }

        final WildFlyJobXmlResolver jobXmlResolver = new WildFlyJobXmlResolver(foundJobXmlFiles);
        // Initialize this instance
        jobXmlResolver.init(classLoader);
        return jobXmlResolver;
    }

    public static void merge(final WildFlyJobXmlResolver target, final WildFlyJobXmlResolver toCopy) {
        toCopy.resolvedJobs.forEach(target.resolvedJobs::putIfAbsent);
        toCopy.jobXmlFiles.forEach(target.jobXmlFiles::putIfAbsent);
        target.jobXmlResolvers.addAll(toCopy.jobXmlResolvers);
    }

    @Override
    public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
        if (jobXmlFiles.isEmpty() && jobXmlResolvers.isEmpty()) {
            return null;
        }
        for (JobXmlResolver resolver : jobXmlResolvers) {
            final InputStream in = resolver.resolveJobXml(jobXml, classLoader);
            if (in != null) {
                return in;
            }
        }
        final VirtualFile file = jobXmlFiles.get(jobXml);
        if (file == null) {
            return null;
        }
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<InputStream>) () -> {
                try {
                    return file.openStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return file.openStream();
    }

    @Override
    public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
        return new ArrayList<>(resolvedJobs.keySet());
    }

    @Override
    public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        return resolvedJobs.get(jobXml);
    }

    private static void addJobXmlFiles(final Map<String, VirtualFile> foundJobXmlFiles, final VirtualFile jobsDir) throws IOException {
        if (jobsDir != null && jobsDir.exists()) {
            // We may have some job XML files
            final Map<String, VirtualFile> xmlFiles = jobsDir.getChildren(JobXmlFilter.INSTANCE)
                    .stream()
                    .collect(Collectors.toMap(VirtualFile::getName, (f) -> f));
            foundJobXmlFiles.putAll(xmlFiles);
        }
    }

    /**
     * Initializes the state of an instance
     */
    private void init(final ClassLoader classLoader) {
        // Load the user defined resolvers
        for (JobXmlResolver resolver : ServiceLoader.load(JobXmlResolver.class, classLoader)) {
            jobXmlResolvers.add(resolver);
            for (String jobXml : resolver.getJobXmlNames(classLoader)) {
                resolvedJobs.put(jobXml, resolver.resolveJobName(jobXml, classLoader));
            }
        }

        // Load the default names
        for (Map.Entry<String, VirtualFile> entry : jobXmlFiles.entrySet()) {
            try {
                // Parsing the entire job XML seems excessive to just get the job name. There are two reasons for this:
                //  1) If an error occurs during parsing there's no real need to consider this a valid job
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
                resolvedJobs.put(entry.getKey(), job.getId());
            } catch (XMLStreamException | IOException e) {
                // Report the possible error as we don't want to fail the deployment. The job may never be run.
                BatchLogger.LOGGER.invalidJobXmlFile(entry.getKey());
            }
        }
    }

    private static class JobXmlFilter implements VirtualFileFilter {

        static final JobXmlFilter INSTANCE = new JobXmlFilter();

        @Override
        public boolean accepts(final VirtualFile file) {
            return file.isFile() && file.getName().endsWith(".xml");
        }
    }
}
