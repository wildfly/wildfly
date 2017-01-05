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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.PlaceholderResource.PlaceholderResourceEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * Represents a dynamic resource for batch {@link javax.batch.runtime.JobExecution job executions}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchJobExecutionResource implements Resource {

    private final Resource delegate;
    private final WildFlyJobOperator jobOperator;
    private final String jobName;
    // Should be guarded by it's instance
    private final Set<String> children = new LinkedHashSet<>();

    BatchJobExecutionResource(final WildFlyJobOperator jobOperator, final String jobName) {
        this(Factory.create(true), jobOperator, jobName);
    }

    private BatchJobExecutionResource(final Resource delegate, final WildFlyJobOperator jobOperator, final String jobName) {
        this.delegate = delegate;
        this.jobOperator = jobOperator;
        this.jobName = jobName;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(final ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(final PathElement element) {
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(element.getKey())) {
            return hasJobExecution(element.getValue());
        }
        return delegate.hasChild(element);
    }

    @Override
    public Resource getChild(final PathElement element) {
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(element.getKey())) {
            if (hasJobExecution(element.getValue())) {
                return PlaceholderResource.INSTANCE;
            }
            return null;
        }
        return delegate.getChild(element);
    }

    @Override
    public Resource requireChild(final PathElement element) {
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(element.getKey())) {
            if (hasJobExecution(element.getValue())) {
                return PlaceholderResource.INSTANCE;
            }
            throw new NoSuchResourceException(element);
        }
        return delegate.requireChild(element);
    }

    @Override
    public boolean hasChildren(final String childType) {
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(childType)) {
            return !getChildrenNames(BatchJobExecutionResourceDefinition.EXECUTION).isEmpty();
        }
        return delegate.hasChildren(childType);
    }

    @Override
    public Resource navigate(final PathAddress address) {
        if (address.size() > 0 && BatchJobExecutionResourceDefinition.EXECUTION.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return PlaceholderResource.INSTANCE;
        }
        return delegate.navigate(address);
    }

    @Override
    public Set<String> getChildTypes() {
        final Set<String> result = new LinkedHashSet<>(delegate.getChildTypes());
        result.add(BatchJobExecutionResourceDefinition.EXECUTION);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(final String childType) {
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(childType)) {
            synchronized (children) {
                refreshChildren();
                return new LinkedHashSet<>(children);
            }
        }
        return delegate.getChildrenNames(childType);
    }

    @Override
    public Set<ResourceEntry> getChildren(final String childType) {
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(childType)) {
            final Set<String> names = getChildrenNames(childType);
            final Set<ResourceEntry> result = new LinkedHashSet<>(names.size());
            for (String name : names) {
                result.add(new PlaceholderResourceEntry(BatchJobExecutionResourceDefinition.EXECUTION, name));
            }
            return result;
        }
        return delegate.getChildren(childType);
    }

    @Override
    public void registerChild(final PathElement address, final Resource resource) {
        final String type = address.getKey();
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(type)) {
            throw BatchLogger.LOGGER.cannotRemoveResourceOfType(type);
        }
        delegate.registerChild(address, resource);
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        throw BatchLogger.LOGGER.indexedChildResourceRegistrationNotAvailable(address);
    }

    @Override
    public Resource removeChild(final PathElement address) {
        final String type = address.getKey();
        if (BatchJobExecutionResourceDefinition.EXECUTION.equals(type)) {
            throw BatchLogger.LOGGER.cannotRemoveResourceOfType(type);
        }
        return delegate.removeChild(address);
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    @Override
    public boolean isProxy() {
        return delegate.isProxy();
    }

    @Override
    public Resource clone() {
        return new BatchJobExecutionResource(delegate.clone(), jobOperator, jobName);
    }

    private boolean hasJobExecution(final String executionName) {
        synchronized (children) {
            if (children.contains(executionName)) {
                return true;
            }
            // Load a cache of the names
            refreshChildren();
            return children.contains(executionName);
        }
    }

    /**
     * Note the access to the {@link #children} is <strong>not</strong> guarded here and needs to be externally
     * guarded.
     */
    private void refreshChildren() {
        final List<JobExecution> executions = new ArrayList<>();
        // Casting to (Supplier<List<JobInstance>>) is done here on purpose as a workaround for a bug in 1.8.0_45
        final List<JobInstance> instances = jobOperator.allowMissingJob((Supplier<List<JobInstance>>)() -> jobOperator.getJobInstances(jobName, 0, jobOperator.getJobInstanceCount(jobName))
                , Collections.emptyList());
        for (JobInstance instance : instances) {
            executions.addAll(jobOperator.getJobExecutions(instance));
        }
        for (JobExecution execution : executions) {
            final String name = Long.toString(execution.getExecutionId());
            if (!children.contains(name)) {
                children.add(name);
            }
        }
    }
}
