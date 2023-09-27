/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class JcaSubSystemRemove extends AbstractRemoveStepHandler {

    static final OperationStepHandler INSTANCE = new JcaSubSystemRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.removeService(ConnectorServices.CONNECTOR_CONFIG_SERVICE);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }

}
