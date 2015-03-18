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

package org.jboss.as.clustering.controller;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;

/**
 * Describes the common properties of a remove operation handler.
 * @author Paul Ferraro
 */
public interface AddStepHandlerDescriptor extends RemoveStepHandlerDescriptor {
    /**
     * Attributes of the add operation.
     * @return a collection of attributes
     */
    Collection<AttributeDefinition> getAttributes();

    /**
     * Extra parameters (not specified by {@link #getAttributes()}) for the add operation.  These parameters are not part of the persistent model.
     * @return a collection of attributes
     */
    Collection<AttributeDefinition> getExtraParameters();
}
