/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class AbstractRuntimeTestCase {
    protected static ModelNode composite(final ModelNode... operations) {
        final ModelNode compositeOperation = Util.getEmptyOperation(ModelDescriptionConstants.COMPOSITE, new ModelNode());
        final ModelNode steps = compositeOperation.get(ModelDescriptionConstants.STEPS);
        for (ModelNode operation : operations) {
            steps.add(operation);
        }
        return compositeOperation;
    }

    protected static ModelNode remove(final String name) {
        return Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, name));
    }

    protected static ModelNode undeploy(final String name) {
        return Util.getEmptyOperation(ModelDescriptionConstants.UNDEPLOY, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, name));
    }
}
