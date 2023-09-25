/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.as.xts.XTSSubsystemAdd.ContextInfo;
import org.jboss.dmr.ModelNode;


/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
class XTSSubsystemRemove extends AbstractRemoveStepHandler {

    static final XTSSubsystemRemove INSTANCE = new XTSSubsystemRemove();

    private XTSSubsystemRemove() {
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        for (ContextInfo contextInfo : XTSSubsystemAdd.getContextDefinitions(context, model)) {
            String contextName = contextInfo.contextPath;
            context.removeService(WSServices.ENDPOINT_PUBLISH_SERVICE.append(contextName));
        }

        context.removeService(XTSServices.JBOSS_XTS_MAIN);
        context.removeService(XTSServices.JBOSS_XTS_TXBRIDGE_INBOUND_RECOVERY);
        context.removeService(XTSServices.JBOSS_XTS_TXBRIDGE_OUTBOUND_RECOVERY);
    }
}
