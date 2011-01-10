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

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * A registry of model node information.  This registry is thread-safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class AbstractNodeRegistration implements ModelNodeRegistration {

    private final String valueString;
    private final NodeSubregistry parent;

    AbstractNodeRegistration(final String valueString, final NodeSubregistry parent) {
        this.valueString = valueString;
        this.parent = parent;
    }

    /** {@inheritDoc} */
    public abstract ModelNodeRegistration registerSubModel(final PathElement address, final DescriptionProvider descriptionProvider);

    /** {@inheritDoc} */
    public abstract void registerOperationHandler(final String operationName, final OperationHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited);

    /** {@inheritDoc} */
    public abstract void registerProxySubModel(final PathElement address, final OperationHandler handler) throws IllegalArgumentException;

    /**
     * Get a handler at a specific address.
     *
     * @param pathAddress the address
     * @param operationName the operation name
     * @return the operation handler, or {@code null} if none match
     */
    public final OperationHandler getOperationHandler(PathAddress pathAddress, String operationName) {
        return getHandler(pathAddress.iterator(), operationName);
    }

    abstract OperationHandler getHandler(ListIterator<PathElement> iterator, String operationName);

    /**
     * Get all the handlers at a specific address.
     *
     * @param address the address
     * @return the handlers
     */
    public Map<String, DescriptionProvider> getOperationDescriptions(PathAddress address) {
        return getOperationDescriptions(address.iterator());
    }

    abstract Map<String, DescriptionProvider> getOperationDescriptions(ListIterator<PathElement> iterator);

    /** {@inheritDoc} */
    public DescriptionProvider getOperationDescription(final PathAddress address, final String operationName) {
        return null;
    }

    abstract DescriptionProvider getOperationDescription(Iterator<PathElement> iterator, String operationName);

    /** {@inheritDoc} */
    public DescriptionProvider getModelDescription(final PathAddress address) {
        return null;
    }

    abstract DescriptionProvider getModelDescription(Iterator<PathElement> iterator);

    /** {@inheritDoc} */
    public ModelNode getNodeDescription(final boolean recursive) {
        final ModelNode node = new ModelNode();
        appendNodeDescription(node, recursive);
        return node;
    }

    abstract void appendNodeDescription(ModelNode node, boolean recursive);

    final String getLocationString() {
        if (parent == null) {
            return "";
        } else {
            return parent.getLocationString() + valueString + ")";
        }
    }
}
