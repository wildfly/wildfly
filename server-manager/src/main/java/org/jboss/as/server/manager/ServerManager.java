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

/**
 * 
 */
package org.jboss.as.server.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.model.Domain;
import org.jboss.as.model.Element;
import org.jboss.as.model.Host;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.model.Standalone;
import org.jboss.as.process.ProcessManagerSlave;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLMapper;

/**
 * A ServerManager.
 * 
 * @author Brian Stansberry
 */
public class ServerManager {
    
    private static final Logger log = Logger.getLogger("org.jboss.server.manager");
    
    private final ServerManagerEnvironment environment;    
    private final StandardElementReaderRegistrar extensionRegistrar;
    private final File hostXML;
    private final MessageHandler messageHandler;
    private ProcessManagerSlave processManagerSlave;
    private Host hostConfig;
    private Domain domainConfig;
    // TODO figure out concurrency controls
//    private final Lock hostLock = new ReentrantLock();
//    private final Lock domainLock = new ReentrantLock();
    private final Map<String, Server> servers = new HashMap<String, Server>();
    
    public ServerManager(ServerManagerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        this.hostXML = new File(environment.getDomainConfigurationDir(), "host.xml");
        this.extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
        this.messageHandler = new MessageHandler(this);
    }
    
    /**
     * Starts the ServerManager. This brings this ServerManager to the point where
     * it has processed it's own configuration file, registered with the DomainController
     * (including starting one if the host configuration specifies that),
     * obtained the domain configuration, and launched any systems needed to make
     * this process manageable by remote clients.
     */
    public void start() {
        
        this.hostConfig = parseHost();
        
        // TODO set up logging for this process based on config in Host
        
        // Start communication with the ProcessManager. This also
        // creates a daemon thread to keep this process alive
        launchProcessManagerSlave();
        
        if (hostConfig.getLocalDomainControllerElement() != null) {
            initiateDomainController();
        }
        
        // DC does not know about this host. Inform it of our existence
        registerWithDomainController();
    }
    
    public void startServers() {
        
        // TODO figure out concurrency controls
//        hostLock.lock(); // should this be domainLock?
//        try {
        ServerMaker serverMaker = new ServerMaker(environment, processManagerSlave, messageHandler);
        for (ServerElement serverEl : hostConfig.getServers()) {
            // TODO take command line input on what servers to start
            if (serverEl.isStart()) {
                log.info("Starting server " + serverEl.getName());
                Standalone serverConf = new Standalone(domainConfig, hostConfig, serverEl.getName());
                JvmElement jvmElement = getServerJvmElement(domainConfig, hostConfig, serverEl.getName());
                try {
                    Server server = serverMaker.makeServer(serverConf, jvmElement);
                    servers.put(serverConf.getServerName(), server);
                    server.start(serverConf);
                } catch (IOException e) {
                    // FIXME handle failure to start server
                    log.error("Failed to start server " + serverEl.getName(), e);
                }
            }
            else log.info("Server " + serverEl.getName() + " is configured to not be started");
        }
//        }
//        finally {
//            hostLock.unlock();
//        }
        
    }

    public void stop() {
        for (Map.Entry<String, Server> entry : servers.entrySet()) {
            try {
                entry.getValue().stop();
                processManagerSlave.removeProcess(entry.getKey());
            }
            catch (Exception e) {
                // FIXME handle exception stopping server
            }
        }
        
        // FIXME stop any local DomainController, stop other internal SM services
    }

    private void launchProcessManagerSlave() {
        this.processManagerSlave = ProcessManagerSlaveFactory.getInstance().getProcessManagerSlave(environment, hostConfig, messageHandler);
        Thread t = new Thread(this.processManagerSlave.getController(), "Server Manager Process");
        t.start();
    }
    
    private void registerWithDomainController() {
        // FIXME -- parsing s/b in initiateDomainController; 
        // here we should discover a DC, provide our Host to it, ask it for 
        // current Domain. But for now we are cheating by using
        this.domainConfig = parseDomain();
    }

    private void initiateDomainController() {
        // FIXME implement initiateDomainController()
        //Domain domain = parseDomain();
        // create Standalone for DC
        // tell PM to start DC process
        // tell PM to give Standalone config to DC
    }

    private Host parseHost() {
        
        if (!hostXML.exists()) {
            throw new IllegalStateException("File " + hostXML.getAbsolutePath() + " does not exist.");
        }
        else if (! hostXML.canWrite()) {
            throw new IllegalStateException("File " + hostXML.getAbsolutePath() + " is not writeable.");
        }
        
        try {
            XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardHostReaders(mapper);
            ParseResult<Host> parseResult = new ParseResult<Host>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(this.hostXML))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of host.xml", e);
        }
    }
    
    private Domain parseDomain() {
        
        File domainXML = new File(environment.getDomainConfigurationDir(), "domain.xml");
        if (!domainXML.exists()) {
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " does not exist. A DomainController cannot be launched without a valid domain.xml");
        }
        else if (! domainXML.canWrite()) {
            throw new IllegalStateException("File " + domainXML.getAbsolutePath() + " is not writeable. A DomainController cannot be launched without a writable domain.xml");
        }
        
        try {
            XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardDomainReaders(mapper);
            ParseResult<Domain> parseResult = new ParseResult<Domain>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(domainXML))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of domain.xml", e);
        }
    }
    
    /**
     * Combines information from the domain, server group, host and server levels
     * to come up with an overall JVM configuration for a server.
     * 
     * @param domain the domain configuration object
     * @param host the host configuration object
     * @param serverName the name of the server
     * @return the JVM configuration object
     */
    private JvmElement getServerJvmElement(Domain domain, Host host, String serverName) {
        
        ServerElement server = host.getServer(serverName);
        if (server == null)
            throw new IllegalStateException("Server " + serverName + " is not listed in Host");
        
        String serverGroupName = server.getServerGroup();
        ServerGroupElement serverGroup = domain.getServerGroup(serverGroupName);
        if (serverGroup == null)
            throw new IllegalStateException("Server group" + serverGroupName + " is not listed in Domain");
        
        JvmElement serverVM = server.getJvm();
        String serverVMName = serverVM != null ? serverVM.getName() : null;
        
        JvmElement groupVM = serverGroup.getJvm();
        String groupVMName = groupVM != null ? groupVM.getName() : null;
        
        String ourVMName = serverVMName != null ? serverVMName : groupVMName;
        if (ourVMName == null) {
            throw new IllegalStateException("Neither " + Element.SERVER_GROUP.getLocalName() + 
                    " nor " + Element.SERVER.getLocalName() + " has declared a JVM configuration; one or the other must");
        }
        
        if (!ourVMName.equals(groupVMName)) {
            // the server setting replaced the group, so ignore group
            groupVM = null;
        }
        JvmElement hostVM = host.getJvm(ourVMName);
        
        return new JvmElement(groupVM, hostVM, serverVM);
    }
}
