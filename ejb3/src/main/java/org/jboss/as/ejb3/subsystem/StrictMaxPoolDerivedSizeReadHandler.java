/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class StrictMaxPoolDerivedSizeReadHandler extends AbstractRuntimeOnlyHandler{

    @Override
    protected void executeRuntimeStep(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName serviceName = StrictMaxPoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName);
        final ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> sc = registry.getService(serviceName);
        if (sc != null) {
            StrictMaxPoolConfigService smpc = (StrictMaxPoolConfigService) sc.getService();
            if (smpc != null) {
                context.getResult().set(smpc.getDerivedSize());
                return;
            }
        }
        throw EjbLogger.ROOT_LOGGER.cannotReadStrictMaxPoolDerivedSize(serviceName);
    }
}
