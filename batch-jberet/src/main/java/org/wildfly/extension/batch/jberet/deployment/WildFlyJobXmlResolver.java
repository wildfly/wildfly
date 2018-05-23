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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobParser;
import org.jberet.spi.JobXmlResolver;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
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
    private final Map<String, String> jobXmlNames;
    private final Map<String, VirtualFile> jobXmlFiles;
    private final Map<String, Set<String>> jobNames;

    private WildFlyJobXmlResolver(final Map<String, VirtualFile> jobXmlFiles) {
        jobXmlNames = new LinkedHashMap<>();
        jobXmlResolvers = new LinkedHashSet<>();
        jobNames = new LinkedHashMap<>();
        this.jobXmlFiles = jobXmlFiles;
    }

    /**
     * Creates the {@linkplain JobXmlResolver resolver} for the deployment inheriting any visible resolvers and job XML
     * files from dependencies.
     *
     * @param deploymentUnit the deployment to process
     *
     * @return the resolve
     *
     * @throws DeploymentUnitProcessingException if an error occurs processing the deployment
     */
    public static WildFlyJobXmlResolver forDeployment(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        // If this deployment unit already has a resolver, just use it
        if (deploymentUnit.hasAttachment(BatchAttachments.JOB_XML_RESOLVER)) {
            return deploymentUnit.getAttachment(BatchAttachments.JOB_XML_RESOLVER);
        }
        // Get the module for it's class loader
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();
        WildFlyJobXmlResolver resolver;
        // If we're an EAR we need to skip sub-deployments as they'll be process later, however all sub-deployments have
        // access to the EAR/lib directory so those resources need to be processed
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            // Create a new WildFlyJobXmlResolver without jobs from sub-deployments as they'll be processed later
            final List<ResourceRoot> resources = new ArrayList<>();
            for (ResourceRoot r : deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS)) {
                if (! SubDeploymentMarker.isSubDeployment(r)) {
                    resources.add(r);
                }
            }
            resolver = create(classLoader, resources);
            deploymentUnit.putAttachment(BatchAttachments.JOB_XML_RESOLVER, resolver);
        } else {
            // Create a new resolver for this deployment
            if (deploymentUnit.hasAttachment(Attachments.RESOURCE_ROOTS)) {
                resolver = create(classLoader, deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS));
            } else {
                resolver = create(classLoader, Collections.singletonList(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT)));
            }
            deploymentUnit.putAttachment(BatchAttachments.JOB_XML_RESOLVER, resolver);
            // Process all accessible sub-deployments
            final List<DeploymentUnit> accessibleDeployments = deploymentUnit.getAttachmentList(Attachments.ACCESSIBLE_SUB_DEPLOYMENTS);
            for (DeploymentUnit subDeployment : accessibleDeployments) {
                // Skip our self
                if (deploymentUnit.equals(subDeployment)) {
                    continue;
                }
                if (subDeployment.hasAttachment(BatchAttachments.JOB_XML_RESOLVER)) {
                    final WildFlyJobXmlResolver toCopy = subDeployment.getAttachment(BatchAttachments.JOB_XML_RESOLVER);
                    WildFlyJobXmlResolver.merge(resolver, toCopy);
                } else {
                    // We need to create a resolver for the sub-deployment and merge the two
                    final WildFlyJobXmlResolver toCopy = forDeployment(subDeployment);
                    subDeployment.putAttachment(BatchAttachments.JOB_XML_RESOLVER, toCopy);
                    WildFlyJobXmlResolver.merge(resolver, toCopy);
                }
            }
        }
        return resolver;
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
        return new ArrayList<>(jobXmlNames.keySet());
    }

    @Override
    public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
        return jobXmlNames.get(jobXml);
    }

    /**
     * Validates whether or not the job name exists for this deployment.
     *
     * @param jobName the job name to check
     *
     * @return {@code true} if the job exists, otherwise {@code false}
     */
    boolean isValidJobName(final String jobName) {
        return jobNames.containsKey(jobName);
    }

    /**
     * Returns the job XML file names which contain the job name.
     *
     * @param jobName the job name to find the job XML files for
     *
     * @return the set of job XML files the job can be run from
     */
    Set<String> getJobXmlNames(final String jobName) {
        if (jobNames.containsKey(jobName)) {
            return Collections.unmodifiableSet(jobNames.get(jobName));
        }
        return Collections.emptySet();
    }

    /**
     * Returns all the job names available from this resolver.
     *
     * @return the job names available from this resolver
     */
    Set<String> getJobNames() {
        return new LinkedHashSet<>(jobNames.keySet());
    }

    /**
     * Validates whether or not the job XML descriptor exists for this deployment.
     *
     * @param jobXmlName the job XML descriptor name
     *
     * @return {@code true} if the job XML descriptor exists for this deployment, otherwise {@code false}
     */
    boolean isValidJobXmlName(final String jobXmlName) {
        return jobXmlNames.containsKey(jobXmlName);
    }

    private static WildFlyJobXmlResolver create(final ClassLoader classLoader, final List<ResourceRoot> resources) throws DeploymentUnitProcessingException {
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

    private static void addJobXmlFiles(final Map<String, VirtualFile> foundJobXmlFiles, final VirtualFile jobsDir) throws IOException {
        if (jobsDir != null && jobsDir.exists()) {
            // We may have some job XML files
            final Map<String, VirtualFile> xmlFiles = new HashMap<>();
            for (VirtualFile f : jobsDir.getChildren(JobXmlFilter.INSTANCE)) {
                if (xmlFiles.put(f.getName(), f) != null) {
                    throw new IllegalStateException("Duplicate key");
                }
            }
            foundJobXmlFiles.putAll(xmlFiles);
        }
    }

    private static void merge(final WildFlyJobXmlResolver target, final WildFlyJobXmlResolver toCopy) {
        for (Map.Entry<String, Set<String>> entry : toCopy.jobNames.entrySet()) {
            if (target.jobNames.containsKey(entry.getKey())) {
                target.jobNames.get(entry.getKey()).addAll(entry.getValue());
            } else {
                target.jobNames.put(entry.getKey(), entry.getValue());
            }
        }
        toCopy.jobXmlNames.forEach(target.jobXmlNames::putIfAbsent);
        toCopy.jobXmlFiles.forEach(target.jobXmlFiles::putIfAbsent);
        target.jobXmlResolvers.addAll(toCopy.jobXmlResolvers);
    }

    /**
     * Initializes the state of an instance
     */
    private void init(final ClassLoader classLoader) {
        // Load the user defined resolvers
        for (JobXmlResolver resolver : ServiceLoader.load(JobXmlResolver.class, classLoader)) {
            jobXmlResolvers.add(resolver);
            for (String jobXml : resolver.getJobXmlNames(classLoader)) {
                addJob(jobXml, resolver.resolveJobName(jobXml, classLoader));
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
                addJob(entry.getKey(), job.getId());
            } catch (XMLStreamException | IOException e) {
                // Report the possible error as we don't want to fail the deployment. The job may never be run.
                BatchLogger.LOGGER.invalidJobXmlFile(entry.getKey());
            }
        }
    }

    private void addJob(final String jobXmlName, final String jobName) {
        jobXmlNames.put(jobXmlName, jobName);
        final Set<String> xmlDescriptors = jobNames.computeIfAbsent(jobName, s -> new LinkedHashSet<>());
        xmlDescriptors.add(jobXmlName);
    }

    private static class JobXmlFilter implements VirtualFileFilter {

        static final JobXmlFilter INSTANCE = new JobXmlFilter();

        @Override
        public boolean accepts(final VirtualFile file) {
            return file.isFile() && file.getName().endsWith(".xml");
        }
    }
}
