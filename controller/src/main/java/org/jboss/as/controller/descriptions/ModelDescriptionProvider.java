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
package org.jboss.as.controller.descriptions;

import org.jboss.as.controller.registry.DescriptionProviderRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Provides information (description, list of attributes, list of children)
 * describing the structure of an addressable model node. Each
 * {@code ModelDescriptionProvider} implementation is associated with a
 * single addressable model node.
 *
 * TODO document the structure of the description itself.
 *
 * @see DescriptionProviderRegistry#register(org.jboss.as.controller.PathAddress, ModelDescriptionProvider)
 *
 * @author Brian Stansberry
 */
public interface ModelDescriptionProvider {

    /**
     * Gets the descriptive information (human-friendly description, list of attributes,
     * list of children) describing a model node. The descriptive information
     * does not include any description of operations.
     * <p>
     * The implementation must assume that the caller intends to modify the
     * returned {@code ModelNode} (e.g. append a list of operation descriptions),
     * so it should not hand out a reference to any internal data structures.
     * </p>
     *
     * @param recursive {@code true} if full information for children is desired
     * (and for children's children, recursively); {@code false}
     * if only brief information describing the model's
     * relationship to its children is needed
     *
     * @return {@link ModelNode} describing the model node's
     */
    ModelNode getModelDescription(boolean recursive);
}
