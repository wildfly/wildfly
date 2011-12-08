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

package org.jboss.as.controller.registry;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationClientException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A addressable resource in the management model, representing a local model and child resources.
 * <p>Instances of this class are <b>not</b> thread-safe and need to be synchronized externally.
 *
 * @author Emanuel Muckenhuber
 */
public interface Resource extends Cloneable {

    /**
     * Get the local model.
     *
     * @return the model
     */
    ModelNode getModel();

    /**
     * Write the model.
     *
     * @param newModel the new model
     */
    void writeModel(ModelNode newModel);

    /**
     * Determine whether the model of this resource is defined.
     *
     * @return {@code true} if the local model is defined
     */
    boolean isModelDefined();

    /**
     * Determine whether this resource has a child with the given address. In case the {@code PathElement} has
     * a wildcard as value, it will determine whether this resource has any resources of a given type.
     *
     * @param element the path element
     * @return {@code true} if there is child with the given address
     */
    boolean hasChild(PathElement element);

    /**
     * Get a single child of this resource with the given address. If no such child exists this will return {@code null}.
     *
     * @param element the path element
     * @return the resource, {@code null} if there is no such child resource
     */
    Resource getChild(PathElement element);

    /**
     * Get a single child of this resource with the given address. If no such child exists a, an exception is thrown.
     *
     * @param element the path element
     * @return the resource
     * @throws java.util.NoSuchElementException if the child does not exist
     *
     * @see NoSuchResourceException
     */
    Resource requireChild(PathElement element);

    /**
     * Determine whether this resource has any child of a given type.
     *
     * @param childType the child type
     * @return {@code true} if there is any child of the given type
     */
    boolean hasChildren(String childType);

    /**
     * Navigate the resource tree.
     *
     * @param address the address
     * @return the resource
     * @throws java.util.NoSuchElementException if any resource in the path does not exist
     *
     * @see NoSuchResourceException
     */
    Resource navigate(PathAddress address);

    /**
     * Get a list of registered child types for this resource.
     *
     * @return the registered child types
     */
    Set<String> getChildTypes();

    /**
     * Get the children names for a given type.
     *
     * @param childType the child type
     * @return the names of registered child resources
     */
    Set<String> getChildrenNames(String childType);

    /**
     * Get the children for a given type.
     *
     * @param childType the child type
     * @return the registered children
     */
    Set<ResourceEntry> getChildren(String childType);

    /**
     * Register a child resource.
     *
     * @param address the address
     * @param resource the resource
     * @throws IllegalStateException for a duplicate entry
     */
    void registerChild(PathElement address, Resource resource);

    /**
     * Remove a child resource.
     *
     * @param address the address
     * @return the resource
     */
    Resource removeChild(PathElement address);

    boolean isRuntime();
    boolean isProxy();

    Resource clone();

    public interface ResourceEntry extends Resource {

        /**
         * Get the name this resource was registered under.
         *
         * @return the resource name
         */
        String getName();

        /**
         * Get the path element this resource was registered under.
         *
         * @return the path element
         */
        PathElement getPathElement();

    }

    public static class Factory {

        private Factory() { }

        /**
         * Create a default resource implementation.
         *
         * @return the resource
         */
        public static Resource create() {
            return new BasicResource();
        }
    }

    public static class Tools {

        public static ResourceFilter ALL_BUT_RUNTIME_AND_PROXIES_FILTER = new ResourceFilter() {
            @Override
            public boolean accepts(PathAddress address, Resource resource) {
                if(resource.isRuntime() || resource.isProxy()) {
                    return false;
                }
                return true;
            }
        };

        private Tools() { }

                /**
         * Recursively read the local model.
         *
         * @param resource the root resource
         * @return the model
         */
        public static ModelNode readModel(final Resource resource) {
            return readModel(resource, -1);
        }

        /**
         * Read the local model.
         *
         * @param resource the model
         * @param level the recursion level
         * @return the model
         */
        public static ModelNode readModel(final Resource resource, final int level) {
            return readModel(resource, level, ALL_BUT_RUNTIME_AND_PROXIES_FILTER);
        }

        /**
         * Read the local model.
         *
         * @param resource the model
         * @param level the recursion level
         * @param filter a resource filter
         * @return the model
         */
        public static ModelNode readModel(final Resource resource, final int level, final ResourceFilter filter) {
            if(filter.accepts(PathAddress.EMPTY_ADDRESS, resource)) {
                return readModel(PathAddress.EMPTY_ADDRESS, resource, level, filter);
            } else {
                return new ModelNode();
            }
        }

        static ModelNode readModel(final PathAddress address, final Resource resource, final int level, final ResourceFilter filter) {
            final ModelNode model = resource.getModel().clone();
            final boolean recursive = level == -1 ? true : level > 0;
            if(recursive) {
                final int newLevel = level == -1 ? -1 : level - 1;
                for(final String childType : resource.getChildTypes()) {
                    model.get(childType).setEmptyObject();
                    for(final ResourceEntry entry : resource.getChildren(childType)) {
                        if(filter.accepts(address.append(entry.getPathElement()), resource)) {
                            model.get(childType, entry.getName()).set(readModel(entry, newLevel));
                        }
                    }
                }
            }
            return model;
        }

        /**
         * Navigate from a parent {@code resource} to the descendant resource at the given relative {@code addresss}.
         * <p>
         * {@link Resource#navigate(PathAddress)} implementations can use this as a standard implementation.
         * </p>
         *
         * @param resource the resource the resource. Cannot be {@code null}
         * @param address the address the address relative to {@code resource}'s address. Cannot be {@code null}
         * @return the resource the descendant resource. Will not be {@code null}
         * @throws java.util.NoSuchElementException if there is no descendant resource at {@code address}
         */
        public static Resource navigate(final Resource resource, final PathAddress address) {
            Resource r = resource;
            for(final PathElement element : address) {
                r = r.requireChild(element);
            }
            return r;
        }

    }

    /**
     * A {@link NoSuchElementException} variant that can be thrown by {@link Resource#requireChild(PathElement)} and
     * {@link Resource#navigate(PathAddress)} implementations to indicate a client error when invoking a
     * management operation.
     */
    public static class NoSuchResourceException extends NoSuchElementException implements OperationClientException {

        public NoSuchResourceException(PathElement childPath) {
            super(ControllerMessages.MESSAGES.childResourceNotFound(childPath));
        }

        @Override
        public ModelNode getFailureDescription() {
            return new ModelNode(getLocalizedMessage());
        }
    }

}
