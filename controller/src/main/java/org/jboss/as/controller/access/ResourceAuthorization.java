/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.access;

/**
 * Encapsulates the {@link AuthorizationResult}s for a given caller's access to a particular resource.
 */
public interface ResourceAuthorization {

    /**
     * Get the authorization result for the entire resource for the given effect.
     * @param actionEffect the action effect
     * @return the authorization result
     */
    AuthorizationResult getResourceResult(Action.ActionEffect actionEffect);

    /**
     * Get the authorization result for an individual attribute.
     * @param attribute the attribute
     * @param actionEffect the action effect
     * @return the authorization result
     */
    AuthorizationResult getAttributeResult(String attribute, Action.ActionEffect actionEffect);

    /**
     * Get the authorization result for an individual operation.
     * @param operationName the operation name
     * @param addressabilityOnly  whether only resource addressibility is relevant
     * @return the authorization result
     */
    AuthorizationResult getOperationResult(String operationName, boolean addressabilityOnly);
}
