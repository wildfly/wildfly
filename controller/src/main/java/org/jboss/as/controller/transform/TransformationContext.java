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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
*/
public interface TransformationContext {

    /**
     * Get the transformation target.
     *
     * @return the transformation target
     */
    TransformationTarget getTarget();


//    /**
//     * Get the original (untransformed) model passed into the transformers.
//     *
//     * The return value depends on the type of transformation {@link #isOperationTransformation()}.
//     *
//     * In case of operation transfomrations this will return the operation model. In case this is part of resource
//     * transformation this is a shortcut to {@link #readResource(org.jboss.as.controller.PathAddress)}.
//     *
//     * @return the untransformed original model
//     */
//    ModelNode getOriginal();

    /**
     * Get the type of process in which this operation is executing.
     *
     * @return the process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the running mode of the process.
     *
     * @return   the running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Get the management resource registration.
     *
     * @param address the path address
     * @return the registration
     */
    ImmutableManagementResourceRegistration getResourceRegistration(PathAddress address);

    /**
     * Get the management resource registration.
     *
     * @param address the path address
     * @return the registration
     */
    ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address);

    /**
     * Read a model resource.
     *
     * @param address the path address
     * @return a read-only resource
     */
    Resource readResource(PathAddress address);

    /**
     * Read a model resource from the root.
     *
     * @param address the path address
     * @return the read-only resource
     */
    Resource readResourceFromRoot(PathAddress address);

    /**
     * Resolve an expression.
     *
     * In some cases an outdated target might not understand expressions for a given attribute, therefore it needs to be
     * resolve before being sent to the target.
     *
     * @param node the node
     * @return the resolved expression
     * @throws OperationFailedException
     *
     * @deprecated expressions cannot be reliably resolved during transformation
     */
    @Deprecated
    ModelNode resolveExpressions(ModelNode node) throws OperationFailedException;

}
