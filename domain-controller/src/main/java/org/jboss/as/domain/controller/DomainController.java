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

import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.as.model.LocalDomainControllerElement;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.as.threads.ThreadFactoryExecutorService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * A Domain controller instance.
 *
 * @author John Bailey
 */
public class DomainController {
    static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("domain", "controller");
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private DomainModel domainConfig;
    private final AtomicBoolean started = new AtomicBoolean();
    private ConcurrentMap<String, DomainControllerClient> clients = new ConcurrentHashMap<String, DomainControllerClient>();

    /**
     * Start the domain controller with configuration.  This will launch required service for the domain controller.
     *
     * @param hostConfig The host configuration
     */
    public synchronized void start(final HostModel hostConfig, final ServiceContainer serviceContainer, final XMLMapper xmlMapper, final File domainConfigDir) {
        if(started.compareAndSet(false, true)) {
            log.info("Starting Domain Controller");

            log.info("Parsing Domain Configuration");
            domainConfig = parseDomain(xmlMapper, domainConfigDir);

            final LocalDomainControllerElement localDomainControllerElement = hostConfig.getLocalDomainControllerElement();
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
            final Set<ServerInterfaceElement> dcInterfaces = localDomainControllerElement.getInterfaces();
            if(dcInterfaces != null) {
                for(ServerInterfaceElement interfaceElement : dcInterfaces) {
                    interfaces.put(interfaceElement.getName(), interfaceElement);
                }
            }
            for(ServerInterfaceElement interfaceElement : interfaces.values()) {
                interfaceElement.activate(serviceActivatorContext);
            }

            // Setup the domain controller executor
            final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
            batchBuilder.addService(threadFactoryServiceName, new ThreadFactoryService());
            final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");
            final ThreadFactoryExecutorService executorService = new ThreadFactoryExecutorService(localDomainControllerElement.getMaxThreads(), false);
            batchBuilder.addService(executorServiceName, executorService)
                .addDependency(threadFactoryServiceName, ThreadFactory.class, executorService.getThreadFactoryInjector());

            //  Add the server manager communication service
            final ServerManagerCommunicationService serverManagerCommunicationService = new ServerManagerCommunicationService(this, localDomainControllerElement);
            batchBuilder.addService(ServerManagerCommunicationService.SERVICE_NAME, serverManagerCommunicationService)
                .addListener(new DomainControllerStartupListener())
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(localDomainControllerElement.getInterfaceName()), NetworkInterfaceBinding.class, serverManagerCommunicationService.getInterfaceInjector())
                .addDependency(executorServiceName, ExecutorService.class, serverManagerCommunicationService.getExecutorServiceInjector())
                .setInitialMode(ServiceController.Mode.IMMEDIATE);

            try {
                batchBuilder.install();
            } catch (ServiceRegistryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stop the domain controller
     */
    public synchronized void stop() {
        if(started.compareAndSet(true, false)) {
            log.info("Stopping Domain Controller");
        }
    }

    public void addClient(final DomainControllerClient domainControllerClient) {
        if(clients.putIfAbsent(domainControllerClient.getId(), domainControllerClient) != null) {
            // TODO: Handle
        }
        domainControllerClient.updateDomain(domainConfig);
    }

    public void removeClient(final DomainControllerClient domainControllerClient) {
        if(clients.remove(domainControllerClient.getId(), domainControllerClient)) {
            // TODO: Handle
        }
    }

    private DomainModel parseDomain(final XMLMapper mapper,  final File domainConfigDir) {
        final File domainXML = new File(domainConfigDir, "domain.xml");
        if (!domainXML.exists()) {
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " does not exist. A DomainController cannot be launched without a valid domain.xml");
        }
        else if (! domainXML.canWrite()) {
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " is not writeable. A DomainController cannot be launched without a writable domain.xml");
        }

        try {
            final ParseResult<DomainModel> parseResult = new ParseResult<DomainModel>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(domainXML))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of domain.xml", e);
        }
    }

    private class DomainControllerStartupListener extends AbstractServiceListener<Void> {
        @Override
        public void serviceStarted(ServiceController<? extends Void> serviceController) {
            log.info("Domain Controller Started");
        }

        @Override
        public void serviceFailed(ServiceController<? extends Void> serviceController, StartException reason) {
            log.error("Failed to start server manger communication service", reason);
        }
    }
}
