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
package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for ModelDescriptionProvider
 *
 * @author Brian Stansberry
 */
public interface ModelDescriptionProvider {

    /**
     * Get the descriptive information (human-friendly description, list of attributes,
     * list of children, list of operations) describing a model node.
     *
     * @param recursive {@code true} if full information for children is desired
     * (and for children's children, recursively); {@code false}
     * if only brief information describing the model's
     * relationship to its children is needed
     *
     * @return {@link ModelNode} describing the meta-information
     *
     * @throws IllegalArgumentException if there is no meta-information available
     * for the given {@code address}
     */
    ModelNode getMetaInformation(boolean recursive);


}
