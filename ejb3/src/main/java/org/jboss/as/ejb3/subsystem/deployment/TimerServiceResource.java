/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem.deployment;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.dmr.ModelNode;

/**
 * @author baranowb
 *
 */
public class TimerServiceResource implements Resource {

    private Resource delegate = Resource.Factory.create(true);

    /**
     * @return
     * @see org.jboss.as.controller.registry.Resource#getModel()
     */
    public ModelNode getModel() {
        return delegate.getModel();
    }

    /**
     * @param newModel
     * @see org.jboss.as.controller.registry.Resource#writeModel(org.jboss.dmr.ModelNode)
     */
    public void writeModel(ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    /**
     * @return
     * @see org.jboss.as.controller.registry.Resource#isModelDefined()
     */
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    /**
     * @param element
     * @return
     * @see org.jboss.as.controller.registry.Resource#hasChild(org.jboss.as.controller.PathElement)
     */
    public boolean hasChild(PathElement element) {
        return delegate.hasChild(element);
    }

    /**
     * @param element
     * @return
     * @see org.jboss.as.controller.registry.Resource#getChild(org.jboss.as.controller.PathElement)
     */
    public Resource getChild(PathElement element) {
        return delegate.getChild(element);
    }

    /**
     * @param element
     * @return
     * @see org.jboss.as.controller.registry.Resource#requireChild(org.jboss.as.controller.PathElement)
     */
    public Resource requireChild(PathElement element) {
        return delegate.requireChild(element);
    }

    /**
     * @param childType
     * @return
     * @see org.jboss.as.controller.registry.Resource#hasChildren(java.lang.String)
     */
    public boolean hasChildren(String childType) {
        return delegate.hasChildren(childType);
    }

    /**
     * @param address
     * @return
     * @see org.jboss.as.controller.registry.Resource#navigate(org.jboss.as.controller.PathAddress)
     */
    public Resource navigate(PathAddress address) {
        return delegate.navigate(address);
    }

    /**
     * @return
     * @see org.jboss.as.controller.registry.Resource#getChildTypes()
     */
    public Set<String> getChildTypes() {
        return delegate.getChildTypes();
    }

    /**
     * @param childType
     * @return
     * @see org.jboss.as.controller.registry.Resource#getChildrenNames(java.lang.String)
     */
    public Set<String> getChildrenNames(String childType) {
        return delegate.getChildrenNames(childType);
    }

    /**
     * @param childType
     * @return
     * @see org.jboss.as.controller.registry.Resource#getChildren(java.lang.String)
     */
    public Set<ResourceEntry> getChildren(String childType) {
        return delegate.getChildren(childType);
    }

    /**
     * @param address
     * @param resource
     * @see org.jboss.as.controller.registry.Resource#registerChild(org.jboss.as.controller.PathElement,
     *      org.jboss.as.controller.registry.Resource)
     */
    public void registerChild(PathElement address, Resource resource) {
        delegate.registerChild(address, resource);
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        throw EjbLogger.ROOT_LOGGER.indexedChildResourceRegistrationNotAvailable(address);
    }

    /**
     * @param address
     * @return
     * @see org.jboss.as.controller.registry.Resource#removeChild(org.jboss.as.controller.PathElement)
     */
    public Resource removeChild(PathElement address) {
        return delegate.removeChild(address);
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    /**
     * @return
     * @see org.jboss.as.controller.registry.Resource#isRuntime()
     */
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    /**
     * @return
     * @see org.jboss.as.controller.registry.Resource#isProxy()
     */
    public boolean isProxy() {
        return delegate.isProxy();
    }

    /**
     * @return
     * @see org.jboss.as.controller.registry.Resource#clone()
     */
    public Resource clone() {
        return this;
    }

    public void timerCreated(String id) {
        PathElement address = PathElement.pathElement(EJB3SubsystemModel.TIMER, id);
        this.delegate.registerChild(address, Resource.Factory.create());
    }

    public void timerRemoved(String id) {
        PathElement address = PathElement.pathElement(EJB3SubsystemModel.TIMER, id);
        this.delegate.removeChild(address);
    }
}
