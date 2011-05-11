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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.domain.controller.DomainContentRepository;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerService;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.HostRegistryService;
import org.jboss.as.domain.controller.LocalHostModel;
import org.jboss.as.domain.controller.MasterDomainControllerClient;
import org.jboss.as.host.controller.ConfigurationPersisterFactory;
import org.jboss.as.host.controller.HostController;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.RemoteDomainConnectionService;
import org.jboss.as.host.controller.mgmt.DomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationService;
import org.jboss.as.server.deployment.api.ContentRepository;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;

/**
 * Utility class to handle the add operations for both local and remote domain controllers.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Emanuel Muckenhuber
 */
public class DomainControllerAddUtil {

    static final ServiceName HOST_CONTROLLER_SERVICE_NAME_BASE = ServiceName.JBOSS.append("host", "controller");

    static void installLocalDomainController(final HostControllerEnvironment environment, final ModelNode host,
                                             final ServiceTarget serviceTarget, final boolean isSlave,
                                             final FileRepository fileRepository, final DomainModelImpl domainModel) {
        final String hostName = host.get(NAME).asString();
        final String mgmtNetwork = host.get(MANAGEMENT_INTERFACES, NATIVE_INTERFACE, INTERFACE).asString();
        final int mgmtPort = host.get(MANAGEMENT_INTERFACES, NATIVE_INTERFACE, PORT).asInt();
        final boolean backupDomainFiles = environment.isBackupDomainFiles();
        final boolean useCachedDc = environment.isUseCachedDc();

        serviceTarget.addService(HostRegistryService.SERVICE_NAME, new HostRegistryService()).install();

        final File configDir = environment.getDomainConfigurationDir();
        final ConfigurationFile configurationFile = environment.getDomainConfigurationFile();
        final ExtensibleConfigurationPersister domainConfigurationPersister = createDomainConfigurationPersister(configDir, configurationFile, isSlave);
        ContentRepository contentRepository = new DomainContentRepository(environment.getDomainDeploymentDir());
        final DomainControllerService dcService = new DomainControllerService(domainConfigurationPersister, hostName, mgmtPort, contentRepository, fileRepository, backupDomainFiles, useCachedDc, domainModel);
        ServiceBuilder<DomainController> builder = serviceTarget.addService(DomainController.SERVICE_NAME, dcService);
        if (isSlave) {
            builder.addDependency(MasterDomainControllerClient.SERVICE_NAME, MasterDomainControllerClient.class, dcService.getMasterDomainControllerClientInjector());
        }
        builder.addDependency(HOST_CONTROLLER_SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, dcService.getScheduledExecutorServiceInjector())
                .addDependency(HostController.SERVICE_NAME, LocalHostModel.class, dcService.getHostControllerServiceInjector())
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, dcService.getInterfaceInjector())
                .addDependency(HostRegistryService.SERVICE_NAME, HostRegistryService.class, dcService.getHostRegistryInjector())
                .install();

        //Install the domain controller operation handler
        DomainControllerOperationHandlerService operationHandlerService = new DomainControllerOperationHandlerService(isSlave);
        serviceTarget.addService(DomainControllerOperationHandlerService.SERVICE_NAME, operationHandlerService)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, operationHandlerService.getManagementCommunicationServiceValue())
                .addDependency(DomainController.SERVICE_NAME, ModelController.class, operationHandlerService.getModelControllerValue())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    static void installRemoteDomainControllerConnection(final ModelNode host, final ServiceTarget serviceTarget, final FileRepository repository) {

        String name;
        try {
            name = host.require(NAME).asString();
        } catch (NoSuchElementException e1) {
            throw new IllegalArgumentException("A host connecting to a remote domain controller must have its name attribute set");
        }

        final ModelNode dc = host.require(DOMAIN_CONTROLLER).require(REMOTE);
        InetAddress addr;
        try {
            addr = InetAddress.getByName(dc.require(HOST).asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        int port = dc.require(PORT).asInt();
        final RemoteDomainConnectionService service = new RemoteDomainConnectionService(name, addr, port, repository);
        serviceTarget.addService(MasterDomainControllerClient.SERVICE_NAME, service)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, service.getManagementCommunicationServiceInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    /**
     * Create the domain.xml configuration persister, in case the DC is running in process.
     *
     * @param configDir the domain configuration directory
     * @param isSlave true if we are a slave
     * @return the configuration persister
     */
    static ExtensibleConfigurationPersister createDomainConfigurationPersister(final File configDir, final ConfigurationFile configurationFile, boolean isSlave) {
        if (isSlave) {
            return ConfigurationPersisterFactory.createCachedRemoteDomainXmlConfigurationPersister(configDir);
        }
        return ConfigurationPersisterFactory.createDomainXmlConfigurationPersister(configDir, configurationFile);
    }

}
