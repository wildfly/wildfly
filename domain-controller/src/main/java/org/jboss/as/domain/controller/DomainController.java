/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import org.jboss.as.model.Domain;
import org.jboss.as.model.Host;
import org.jboss.as.model.LocalDomainControllerElement;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.as.server.manager.DomainControllerConfig;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Domain controller instance.
 *
 * @author John Bailey
 */
public class DomainController {
    static final String DOMAIN_CONTROLLER_PROCESS_NAME = "domain-controller";
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final DomainControllerEnvironment environment;
    private final ProcessMessageHandler messageHandler = new ProcessMessageHandler(this);
    private ProcessCommunicationHandler communicationHandler;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
    private final StandardElementReaderRegistrar extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
    private Domain domainConfig;

    /**
     * Create an instance with an environment.
     *
     * @param environment The DomainController environment
     */
    public DomainController(DomainControllerEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Start the domain controller
     */
    void start() {
        launchCommunicationHandler();
        sendMessage(ServerManagerProtocolCommand.SERVER_AVAILABLE);
    }


    /**
     * Start the domain controller with configuration.  This will launch required service for the domain controller.
     *
     * @param domainControllerConfig The domain controller configuration
     */
    synchronized void start(final DomainControllerConfig domainControllerConfig) {
        log.info("Starting Domain Controller");

        log.info("Parsing Domain Configuration");
        domainConfig = parseDomain();

        final LocalDomainControllerElement localDomainControllerElement = domainControllerConfig.getDomainControllerElement();
        final Host hostConfig = domainControllerConfig.getHost();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContextImpl(batchBuilder);

        // Activate Interfaces
        final Map<String, ServerInterfaceElement> interfaces = new HashMap<String, ServerInterfaceElement>();
        final Set<ServerInterfaceElement> hostInterfaces = hostConfig.getInterfaces();
        if(hostInterfaces != null) {
            for(ServerInterfaceElement interfaceElement : hostInterfaces) {
                interfaces.put(interfaceElement.getName(), interfaceElement);
            }
        }
        final Map<String, ServerInterfaceElement> dcInterfaces = localDomainControllerElement.getInterfaces();
        if(dcInterfaces != null)
        for(Map.Entry<String, ServerInterfaceElement> interfaceElement : dcInterfaces.entrySet()) {
            interfaces.put(interfaceElement.getKey(), interfaceElement.getValue());
        }
        for(ServerInterfaceElement interfaceElement : interfaces.values()) {
            interfaceElement.activate(serviceActivatorContext);
        }

        //  Add the server manager communication service
        final ServerManagerCommunicationService serverManagerCommunicationService = new ServerManagerCommunicationService(this, localDomainControllerElement);
        batchBuilder.addService(ServerManagerCommunicationService.SERVICE_NAME, serverManagerCommunicationService)
            .addListener(new DomainControllerStartupListener())
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(localDomainControllerElement.getInterfaceName()), NetworkInterfaceBinding.class, serverManagerCommunicationService.getInterfaceInjector())
            .setInitialMode(ServiceController.Mode.IMMEDIATE);

        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the domain controller
     */
    synchronized void stop() {
        log.info("Stopping Domain Controller");
        serviceContainer.shutdown();
    }

    public Domain getDomain() {
        return domainConfig;
    }

    private void launchCommunicationHandler() {
        communicationHandler = new ProcessCommunicationHandler(environment.getProcessManagerAddress(), environment.getProcessManagerPort(), messageHandler);
        Thread t = new Thread(communicationHandler.getController(), "DomainController Process");
        t.start();
    }

    private Domain parseDomain() {
        final File domainXML = new File(environment.getDomainConfigurationDir(), "domain.xml");
        if (!domainXML.exists()) {
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " does not exist. A DomainController cannot be launched without a valid domain.xml");
        }
        else if (! domainXML.canWrite()) {
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " is not writeable. A DomainController cannot be launched without a writable domain.xml");
        }

        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardDomainReaders(mapper);
            final ParseResult<Domain> parseResult = new ParseResult<Domain>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(domainXML))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of domain.xml", e);
        }
    }

    private void sendMessage(ServerManagerProtocolCommand command) {
        try {
            byte[] bytes = command.createCommandBytes(null);
            communicationHandler.sendMessage(bytes);
        } catch (IOException e) {
            log.error("Failed to send message to Server Manager [" + command + "]", e);
        }
    }

    private class DomainControllerStartupListener extends AbstractServiceListener<Void> {
        @Override
        public void serviceStarted(ServiceController<? extends Void> serviceController) {
            DomainController.this.sendMessage(ServerManagerProtocolCommand.SERVER_STARTED);
        }

        @Override
        public void serviceFailed(ServiceController<? extends Void> serviceController, StartException reason) {
            log.error("Failed to start server manger communication service", reason);
            DomainController.this.sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED);
        }
    }
}
