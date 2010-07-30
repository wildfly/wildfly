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
import org.jboss.as.model.Host;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.Standalone;
import org.jboss.as.process.ProcessManagerSlave;
import org.jboss.staxmapper.XMLMapper;

/**
 * A ServerManager.
 * 
 * @author Brian Stansberry
 */
public class ServerManager {

    private final ServerManagerBootstrapConfig bootstrapConfig;    
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
    
    public ServerManager(ServerManagerBootstrapConfig bootstrapConfig) {
        if (bootstrapConfig == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.bootstrapConfig = bootstrapConfig;
        this.hostXML = new File(bootstrapConfig.getDomainConfigurationDir(), "host.xml");
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
        
        // FIXME temporary expedient to keep this process alive
    }

    private void launchProcessManagerSlave() {
        this.processManagerSlave = ProcessManagerSlaveFactory.getInstance().getProcessManagerSlave(bootstrapConfig, hostConfig, messageHandler);
        Thread t = new Thread(this.processManagerSlave.getController(), "Server Manager Process");
        t.setDaemon(true);
        t.start();
    }
    
    public void startServers() {
        
        // TODO figure out concurrency controls
//        hostLock.lock(); // should this be domainLock?
//        try {
        ServerMaker serverMaker = new ServerMaker(bootstrapConfig, processManagerSlave, messageHandler);
        for (ServerElement serverEl : hostConfig.getServers()) {
            // TODO take command line input on what servers to start
            if (serverEl.isStart()) {
                Standalone serverConf = new Standalone(domainConfig, hostConfig, serverEl.getName());
                
                try {
                    Server server = serverMaker.makeServer(serverConf);
                    servers.put(serverConf.getServerName(), server);
                    server.start(serverConf);
                } catch (IOException e) {
                    // FIXME handle failure to start server
                    e.printStackTrace();
                }
            }
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
        
        File domainXML = new File(bootstrapConfig.getDomainConfigurationDir(), "domain.xml");
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
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(this.hostXML))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of domain.xml", e);
        }
    }
}
