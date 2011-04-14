/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.DomainControllerSlaveClient;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ReadResourceHandler extends org.jboss.as.controller.operations.global.GlobalOperationHandlers.ReadResourceHandler {

    private final DomainModelImpl domainModelImpl;

    public ReadResourceHandler(DomainModelImpl domainModelImpl) {
        this.domainModelImpl = domainModelImpl;
    }

    @Override
    protected void addProxyNodes(final OperationContext context, final PathAddress address, final ModelNode originalOperation,
            final ModelNode result, final ModelNodeRegistration registry) {

        super.addProxyNodes(context, address, originalOperation, result, registry);

        try {
            if (address.size() == 0 && domainModelImpl.isMaster()) {
                for (Map.Entry<String, DomainControllerSlaveClient> entry : domainModelImpl.getRemoteHosts().entrySet()) {
                    final ModelNode operation = originalOperation.clone();
                    final PathAddress hostAddr = PathAddress.pathAddress(PathElement.pathElement(HOST, entry.getKey()));
                    operation.get(OP_ADDR).set(hostAddr.toModelNode());

                    ModelNode hostResult = entry.getValue().execute(OperationBuilder.Factory.copy(context, operation).build());
                    addProxyResultToMainResult(hostAddr, result, hostResult);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleNonRecursiveProxyEntries(final OperationContext context, final PathAddress address, final ModelNode originalOperation, final ModelNode result, final ModelNodeRegistration registry) {
        if (address.size() == 0 && domainModelImpl.isMaster()) {
            for (Map.Entry<String, DomainControllerSlaveClient> entry : domainModelImpl.getRemoteHosts().entrySet()) {
                result.get(HOST, entry.getKey());
            }
        }
    }

    @Override
    protected void addProxyResultToMainResult(final PathAddress address, final ModelNode mainResult, final ModelNode proxyResult) {
        PathAddress addr = !domainModelImpl.isMaster() && address.size() > 0 && address.getElement(0).getKey().equals(HOST) ?
                address.subAddress(1) : address;
        super.addProxyResultToMainResult(addr, mainResult, proxyResult);
    }
}
