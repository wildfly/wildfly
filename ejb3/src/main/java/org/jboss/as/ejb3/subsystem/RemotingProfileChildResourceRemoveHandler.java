/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 **/

public class RemotingProfileChildResourceRemoveHandler extends RemotingProfileChildResourceHandlerBase{
    protected void updateModel(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        // verify that the resource exist before removing it
        context.readResource(PathAddress.EMPTY_ADDRESS, false);

        context.removeResource(PathAddress.EMPTY_ADDRESS);
    }
}
