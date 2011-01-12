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

import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ProxyNodeRegistration extends AbstractNodeRegistration {
    private final OperationHandler operationHandler;

    ProxyNodeRegistration(final String valueString, final NodeSubregistry parent, final OperationHandler operationHandler) {
        super(valueString, parent);
        this.operationHandler = operationHandler;
    }

    @Override
    OperationHandler getHandler(final ListIterator<PathElement> iterator, final String operationName) {
        return operationHandler;
    }

    @Override
    Map<String, DescriptionProvider> getOperationDescriptions(final ListIterator<PathElement> iterator) {
        return Collections.emptyMap();
    }

    @Override
    public ModelNodeRegistration registerSubModel(final PathElement address, final DescriptionProvider descriptionProvider) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerOperationHandler(final String operationName, final OperationHandler handler, final DescriptionProvider descriptionProvider, final boolean inherited) {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    public void registerProxySubModel(final PathElement address, final OperationHandler handler) throws IllegalArgumentException {
        throw new IllegalArgumentException("A proxy handler is already registered at location '" + getLocationString() + "'");
    }

    @Override
    DescriptionProvider getOperationDescription(final Iterator<PathElement> iterator, final String operationName) {
        // todo
        return null;
    }

    @Override
    DescriptionProvider getModelDescription(final Iterator<PathElement> iterator) {
        // todo
        return null;
    }

    @Override
    Set<String> getAttributeNames(Iterator<PathElement> iterator) {
        return null;
    }

    @Override
    Set<String> getChildNames(Iterator<PathElement> iterator) {
        return null;
    }
}
