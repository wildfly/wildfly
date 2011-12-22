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

package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Utility class to handle the add operations for both local and remote domain controllers.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Emanuel Muckenhuber
 */
public class DomainControllerAddUtil {

    static Collection<ServiceController<?>> installLocalDomainController(final ModelNode host,
                                                                         final ServiceTarget serviceTarget,
                                                                         final boolean isSlave,
                                                                         final ServiceVerificationHandler verificationHandler) {
        //THIS NEVER GETS CALLED
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();

        final String hostName = host.get(NAME).asString();
        final String mgmtNetwork = host.get(MANAGEMENT_INTERFACE, NATIVE_INTERFACE, INTERFACE).asString();
        final int mgmtPort = host.get(MANAGEMENT_INTERFACE, NATIVE_INTERFACE, PORT).asInt();

//        controllers.add(serviceTarget.addService(HostRegistryService.SERVICE_NAME, new HostRegistryService()).addListener(verificationHandler).install());
//
//        final DomainControllerService dcService = new DomainControllerService(domainConfigurationPersister, hostName, mgmtPort, contentRepository, fileRepository, backupDomainFiles, useCachedDc, domainModel);
//        ServiceBuilder<DomainController> builder = serviceTarget.addService(DomainController.SERVICE_NAME, dcService);
//        if (isSlave) {
//            builder.addDependency(MasterDomainControllerClient.SERVICE_NAME, MasterDomainControllerClient.class, dcService.getMasterDomainControllerClientInjector());
//        }
//        controllers.add(builder.addDependency(HOST_CONTROLLER_SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, dcService.getScheduledExecutorServiceInjector())
//                .addDependency(HostController.SERVICE_NAME, LocalHostModel.class, dcService.getHostControllerServiceInjector())
//                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, dcService.getInterfaceInjector())
//                .addDependency(HostRegistryService.SERVICE_NAME, HostRegistryService.class, dcService.getHostRegistryInjector())
//                .addListener(verificationHandler)
//                .install());

//        RemotingServices.installDomainControllerManagementChannelServices(serviceTarget,
//                new NewModelControllerClientOperationHandlerService(),
//                DomainModelControllerService.SERVICE_NAME,
//                NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork),
//                mgmtPort,
//                verificationHandler,
//                controllers);
//
//        //TODO move into bootstrap
//        if (!isSlave) {
//            RemotingServices.installChannelServices(serviceTarget, new MasterDomainControllerOperationHandlerService(), DomainModelControllerService.SERVICE_NAME, "domain", verificationHandler, controllers);
//        }
        return controllers;
    }

    static ServiceController<?> installRemoteDomainControllerConnection(final ModelNode host, final ServiceTarget serviceTarget, final FileRepository localFileRepository) {

        final String name;
        try {
            name = host.require(NAME).asString();
        } catch (NoSuchElementException e1) {
            throw MESSAGES.noNameAttributeOnHost();
        }

        final ModelNode dc = host.require(DOMAIN_CONTROLLER).require(REMOTE);
        final InetAddress addr;
        try {
            addr = InetAddress.getByName(dc.require(HOST).resolve().asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        final int port = dc.require(PORT).resolve().asInt();
//        final RemoteDomainConnectionService service = new RemoteDomainConnectionService(name, addr, port, localFileRepository);
//        return serviceTarget.addService(MasterDomainControllerClient.SERVICE_NAME, service)
//                .setInitialMode(ServiceController.Mode.ACTIVE)
//                .install();
        return null;
    }

}
