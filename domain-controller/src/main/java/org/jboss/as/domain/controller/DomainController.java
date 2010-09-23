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
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private DomainModel domainModel;
    private final AtomicBoolean started = new AtomicBoolean();
    private ConcurrentMap<String, DomainControllerClient> clients = new ConcurrentHashMap<String, DomainControllerClient>();

    /**
     * Start the domain controller with configuration.  This will launch required service for the domain controller.
     *
     * @param hostConfig The host configuration
     */
    public synchronized void start(final HostModel hostConfig, final XMLMapper xmlMapper, final File domainConfigDir) {
        if(started.compareAndSet(false, true)) {
            log.info("Starting Domain Controller");

            log.info("Parsing Domain Configuration");
            domainModel = parseDomain(xmlMapper, domainConfigDir);
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
    }

    public void removeClient(final String id) {
        if(clients.remove(id) == null) {
            // TODO: Handle
        }
    }

    public synchronized DomainModel getDomainModel() {
        return domainModel;
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
}
