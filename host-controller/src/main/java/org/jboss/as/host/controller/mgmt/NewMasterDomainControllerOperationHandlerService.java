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
package org.jboss.as.host.controller.mgmt;

import org.jboss.as.controller.remote.NewAbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.NewModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.controller.NewDomainController;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

/**
 * Installs {@link NewMasterDomainControllerOperationHandlerImpl} which handles requests from slave DC to master DC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewMasterDomainControllerOperationHandlerService extends NewAbstractModelControllerOperationHandlerFactoryService<NewMasterDomainControllerOperationHandlerImpl> {
    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    public static final ServiceName SERVICE_NAME = NewDomainController.SERVICE_NAME.append(NewModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);

    private final NewDomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    public NewMasterDomainControllerOperationHandlerService(final NewDomainController domainController, final UnregisteredHostChannelRegistry registry) {
        this.domainController = domainController;
        this.registry = registry;
    }

    @Override
    public NewMasterDomainControllerOperationHandlerImpl createOperationHandler() {
        return new NewMasterDomainControllerOperationHandlerImpl(getExecutor(), getController(), registry, domainController);
    }

}
