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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.DomainModel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * A Domain controller instance.
 *
 * @author John Bailey
 */
public class DomainController implements Service<DomainController> {
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller");
    private DomainModel domainModel;
    private final ConcurrentMap<String, DomainControllerClient> clients = new ConcurrentHashMap<String, DomainControllerClient>();
    private final InjectedValue<XMLMapper> xmlMapper = new InjectedValue<XMLMapper>();
    private final InjectedValue<File> domainConfigDir = new InjectedValue<File>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorService = new InjectedValue<ScheduledExecutorService>();
    private ScheduledFuture<?> pollingFuture;

    /**
     * Start the domain controller with configuration.  This will launch required service for the domain controller.
     */
    public synchronized void start(final StartContext context) {
        log.info("Starting Domain Controller");

        log.info("Parsing Domain Configuration");
        domainModel = parseDomain(xmlMapper.getValue(), domainConfigDir.getValue());
        pollingFuture = scheduledExecutorService.getValue().scheduleAtFixedRate(new Runnable() {
            public void run() {
                for(DomainControllerClient client : clients.values()) {
                    if(!client.isActive()) {
                        log.warnf("Registered Server Manager [%s] is no longer active", client.getId());
                    }
                }
            }
        }, 30L, 30L, TimeUnit.SECONDS);
    }

    /**
     * Stop the domain controller
     */
    public synchronized void stop(final StopContext stopContext) {
        log.info("Stopping Domain Controller");
        domainModel = null;
        if(pollingFuture != null) {
            pollingFuture.cancel(true);
        }
    }

    public DomainController getValue() throws IllegalStateException {
        return this;
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
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " is not writable. A DomainController cannot be launched without a writable domain.xml");
        }

        try {
            final List<AbstractDomainModelUpdate<?>> domainUpdates = new ArrayList<AbstractDomainModelUpdate<?>>();
            mapper.parseDocument(domainUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(domainXML))));
            final DomainModel domainModel = new DomainModel();
            for(final AbstractDomainModelUpdate<?> update : domainUpdates) {
                domainModel.update(update);
            }
            return domainModel;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of domain.xml", e);
        }
    }

    public Injector<XMLMapper> getXmlMapperInjector() {
        return xmlMapper;
    }

    public Injector<File> getDomainConfigDirInjector() {
        return domainConfigDir;
    }

    public Injector<ScheduledExecutorService> getScheduledExecutorServiceInjector() {
        return scheduledExecutorService;
    }
}
