/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface LegacyKernelServicesInitializer {

    LegacyKernelServicesInitializer initializerCreateModelResource(PathAddress parentAddress, PathElement relativeResourceAddress, ModelNode model);

    /**
     * If called, will not use boot operations rather ship across the model via ApplyRemoteMasterDomainHandler
     */
    LegacyKernelServicesInitializer setDontUseBootOperations();

    /**
     * By default all operations sent into the model controller will be validated on boot. Operations matching what is
     * set up here will not be validated. This is mainly because the {@link OperationValidator} used in 7.1.x did not handle expressions very well
     * when checking ranges. If there is a problem you should try to call {@link #addOperationValidationResolve(String, PathAddress)}
     * first.
     *
     * @param name the name of the operation, or {@code *} as a wildcard capturing all names
     * @param pathAddress the address of the operation, the pathAddress may use {@code *} as a wildcard for both the key and the value of {@link PathElement}s
     */
    LegacyKernelServicesInitializer addOperationValidationExclude(String name, PathAddress pathAddress);

    /**
     * By default all operations sent into the model controller will be validated on boot. Operations matching what is
     * set up here will not be validated. This is mainly because the {@link OperationValidator} used in 7.1.x did not handle expressions very well
     * when checking ranges.
     *
     * @param name the name of the operation, or {@code *} as a wildcard capturing all names
     * @param pathAddress the address of the operation, the pathAddress may use {@code *} as a wildcard for both the key and the value of {@link PathElement}s
     */
    LegacyKernelServicesInitializer addOperationValidationResolve(String name, PathAddress pathAddress);
}
