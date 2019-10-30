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
