/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.mina.util.AvailablePortFinder;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} implementation, which starts embedded LDAP server (ApacheDS). The implementation is based on
 * AbstractServerTestcase class from the ApacheDS.
 * 
 * @author Josef Cacek
 * @see #configureDirectoryService()
 * @see #configureLdapServer()
 * @see #getPort()
 */
public class LDAPServerSetupTask implements ServerSetupTask {
    private static final Logger LOGGER = Logger.getLogger(LDAPServerSetupTask.class);
    private static final List<LdifEntry> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<LdifEntry>(0));

    /** the context root for the rootDSE */
    protected CoreSession rootDSE;

    /** the context root for the system partition */
    protected LdapContext sysRoot;

    /** the context root for the schema */
    protected LdapContext schemaRoot;

    /** flag whether to delete database files for each test or not */
    protected boolean doDelete = true;

    protected int port = -1;

    protected DirectoryService directoryService;
    protected LdapServer ldapServer;

    // Public methods --------------------------------------------------------

    /**
     * Configures and starts ApacheDS LDAP server.
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void setup(ManagementClient managementClient, String containerId) throws Exception {
        directoryService = new DefaultDirectoryService();
        directoryService.setShutdownHookEnabled(false);
        port = getPort();
        LOGGER.info("Creating LDAP server on port " + port);

        ldapServer = new LdapServer();
        ldapServer.setTransports(new TcpTransport(port));
        ldapServer.setDirectoryService(directoryService);

        setupSaslMechanisms();

        doDelete(directoryService.getWorkingDirectory());
        configureDirectoryService();
        LOGGER.debug("Starting directory service");
        directoryService.startup();

        rootDSE = directoryService.getAdminSession();

        configureLdapServer();

        ldapServer.addExtendedOperationHandler(new StartTlsHandler());
        ldapServer.addExtendedOperationHandler(new StoredProcedureExtendedOperationHandler());

        LOGGER.debug("Starting LDAP server");
        ldapServer.start();

        setContexts(ServerDNConstants.ADMIN_SYSTEM_DN, "secret");
    }

    /**
     * Stops the LDAP server and directory service and sets the system context root to null.
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        LOGGER.info("Stopping LDAP server");
        ldapServer.stop();
        LOGGER.info("Shutting down DirectoryService");
        directoryService.shutdown();
        sysRoot = null;
    }

    // Protected methods -----------------------------------------------------

    /**
     * Returns port number on which the LDAP server should run.
     * 
     * @return port number
     */
    protected int getPort() {
        return port > 0 ? port : AvailablePortFinder.getNextAvailable(1024);
    }

    /**
     * Method called just before the directory service is started. Use it to change the directory service configuration.
     * 
     * @throws Exception
     * @see #configureLdapServer()
     */
    protected void configureDirectoryService() throws Exception {
        //nothing to do here, user can override the method
    }

    /**
     * Method called after the directory service is started, but before the LDAP is started. Use it to import LDIF for instance.
     * 
     * @throws Exception
     * @see #configureDirectoryService()
     * @see #importLdif(InputStream)
     */
    protected void configureLdapServer() throws Exception {
        //nothing to do here, user can override the method
    }

    /**
     * If there is an LDIF file with the same name as the test class but with the .ldif extension then it is read and the
     * entries it contains are added to the server. It appears as though the administrator adds these entries to the server.
     * 
     * @param verifyEntries whether or not all entry additions are checked to see if they were in fact correctly added to the
     *        server
     * @return a list of entries added to the server in the order they were added
     * @throws NamingException of the load fails
     */
    protected final List<LdifEntry> loadTestLdif(boolean verifyEntries) throws Exception {
        return loadLdif(getClass().getResourceAsStream(getClass().getSimpleName() + ".ldif"), verifyEntries);
    }

    /**
     * Loads an LDIF from an input stream and adds the entries it contains to the server. It appears as though the administrator
     * added these entries to the server.
     * 
     * @param in the input stream containing the LDIF entries to load
     * @param verifyEntries whether or not all entry additions are checked to see if they were in fact correctly added to the
     *        server
     * @return a list of entries added to the server in the order they were added
     * @throws NamingException of the load fails
     */
    protected final List<LdifEntry> loadLdif(InputStream in, boolean verifyEntries) throws Exception {
        if (in == null) {
            return EMPTY_LIST;
        }

        LdifReader ldifReader = new LdifReader(in);
        List<LdifEntry> entries = new ArrayList<LdifEntry>();

        for (LdifEntry entry : ldifReader) {
            rootDSE.add(new DefaultServerEntry(directoryService.getRegistries(), entry.getEntry()));

            if (verifyEntries) {
                verify(entry);
                LOGGER.info("Successfully verified addition of entry " + entry.getDn());
            } else {
                LOGGER.info("Added entry without verification " + entry.getDn());
            }

            entries.add(entry);
        }

        return entries;
    }

    /**
     * Verifies that an entry exists in the directory with the specified attributes.
     * 
     * @param entry the entry to verify
     * @throws NamingException if there are problems accessing the entry
     */
    protected final void verify(LdifEntry entry) throws Exception {
        Entry readEntry = rootDSE.lookup(entry.getDn());

        for (EntryAttribute readAttribute : readEntry) {
            String id = readAttribute.getId();
            EntryAttribute origAttribute = entry.getEntry().get(id);

            for (Value<?> value : origAttribute) {
                if (!readAttribute.contains(value)) {
                    LOGGER.error("Failed to verify entry addition of " + entry.getDn() + ". " + id + " attribute in original "
                            + "entry missing from read entry.");
                    throw new AssertionFailedError("Failed to verify entry addition of " + entry.getDn());
                }
            }
        }
    }

    /**
     * Adds a partition with given ID and suffix to the directory.
     * 
     * @param id partition ID, e.g. "jboss"
     * @param suffix partition suffix, e.g. "dc=jboss,dc=org"
     * @throws Exception
     */
    protected final void addPartition(final String id, final String suffix) throws Exception {
        final JdbmPartition tcPartition = new JdbmPartition();
        tcPartition.setId(id);
        tcPartition.setCacheSize(1000);

        // Create some indices
        Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
        indexedAttrs.add(new JdbmIndex<Object, ServerEntry>("objectClass"));
        indexedAttrs.add(new JdbmIndex<Object, ServerEntry>("o"));
        tcPartition.setIndexedAttributes(indexedAttrs);

        //Add suffix
        tcPartition.setSuffix(suffix);
        tcPartition.init(directoryService);
        directoryService.addPartition(tcPartition);
    }

    /**
     * Deletes the Eve working directory.
     * 
     * @param wkdir the directory to delete
     * @throws IOException if the directory cannot be deleted
     */
    protected final void doDelete(File wkdir) throws IOException {
        if (doDelete) {
            if (wkdir.exists()) {
                FileUtils.deleteDirectory(wkdir);
            }

            if (wkdir.exists()) {
                throw new IOException("Failed to delete: " + wkdir);
            }
        }
    }

    /**
     * Sets the contexts for this base class. Values of user and password used to set the respective JNDI properties. These
     * values can be overriden by the overrides properties.
     * 
     * @param user the username for authenticating as this user
     * @param passwd the password of the user
     * @throws NamingException if there is a failure of any kind
     */
    protected final void setContexts(String user, String passwd) throws Exception {
        final Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(DirectoryService.JNDI_KEY, directoryService);
        env.put(Context.SECURITY_PRINCIPAL, user);
        env.put(Context.SECURITY_CREDENTIALS, passwd);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName());

        env.put(Context.PROVIDER_URL, ServerDNConstants.SYSTEM_DN);
        sysRoot = new InitialLdapContext(env, null);
        env.put(Context.PROVIDER_URL, ServerDNConstants.OU_SCHEMA_DN);
        schemaRoot = new InitialLdapContext(env, null);
    }

    /**
     * Imports the LDIF entries packaged with the Eve JNDI provider jar into the newly created system partition to prime it up
     * for operation. Note that only ou=system entries will be added - entries for other partitions cannot be imported and will
     * blow chunks.
     * 
     * @throws NamingException if there are problems reading the ldif file and adding those entries to the system partition
     * @param in the input stream with the ldif
     */
    protected final void importLdif(InputStream in) throws NamingException {
        try {
            for (LdifEntry ldifEntry : new LdifReader(in)) {
                rootDSE.add(new DefaultServerEntry(rootDSE.getDirectoryService().getRegistries(), ldifEntry.getEntry()));
            }
        } catch (Exception e) {
            String msg = "failed while trying to parse system ldif file";
            NamingException ne = new LdapConfigurationException(msg);
            ne.setRootCause(e);
            throw ne;
        }
    }

    /**
     * Inject an ldif String into the server. DN must be relative to the root.
     * 
     * @param ldif the entries to inject
     * @throws NamingException if the entries cannot be added
     */
    protected final void injectEntries(String ldif) throws Exception {
        LdifReader reader = new LdifReader();
        List<LdifEntry> entries = reader.parseLdif(ldif);

        for (LdifEntry entry : entries) {
            rootDSE.add(new DefaultServerEntry(rootDSE.getDirectoryService().getRegistries(), entry.getEntry()));
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Configures supported SASL mechanisms handlers for the LDAP.
     */
    private void setupSaslMechanisms() {
        Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String, MechanismHandler>();

        mechanismHandlerMap.put(SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler());

        CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
        mechanismHandlerMap.put(SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler);

        DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
        mechanismHandlerMap.put(SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler);

        GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
        mechanismHandlerMap.put(SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler);

        NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
        // TODO - set some sort of default NtlmProvider implementation here
        // ntlmMechanismHandler.setNtlmProvider( provider );
        // TODO - or set FQCN of some sort of default NtlmProvider implementation here
        // ntlmMechanismHandler.setNtlmProviderFqcn( "com.foo.BarNtlmProvider" );
        mechanismHandlerMap.put(SupportedSaslMechanisms.NTLM, ntlmMechanismHandler);
        mechanismHandlerMap.put(SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler);

        ldapServer.setSaslMechanismHandlers(mechanismHandlerMap);
    }

}
