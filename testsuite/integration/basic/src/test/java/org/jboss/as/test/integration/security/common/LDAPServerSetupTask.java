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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.config.ConfigPartitionReader;
import org.apache.directory.server.config.LdifConfigExtractor;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.LdapServerBean;
import org.apache.directory.server.config.beans.TcpTransportBean;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.CsnSyntaxChecker;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.GeneralizedTimeSyntaxChecker;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.UuidSyntaxChecker;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.DateUtils;
import org.apache.directory.shared.util.exception.Exceptions;
import org.apache.mina.util.AvailablePortFinder;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} implementation, which starts embedded LDAP server (ApacheDS).
 * 
 * @author Josef Cacek
 * @see #configureDirectoryService()
 * @see #configureLdapServer()
 * @see #getPort()
 */
public class LDAPServerSetupTask implements ServerSetupTask {
    private static final Logger LOGGER = Logger.getLogger(LDAPServerSetupTask.class);

    protected int port = -1;

    private DirectoryService directoryService;

    /** The LDAP server instance */
    private LdapServer ldapServer;

    /** The Schema partition */
    private LdifPartition schemaLdifPartition;

    /** The SchemaManager instance */
    private SchemaManager schemaManager;

    /** The configuration partition */
    private SingleFileLdifPartition configPartition;

    /** The configuration reader instance */
    private ConfigPartitionReader cpReader;

    // variables used during the initial startup to update the mandatory operational
    // attributes
    /** The UUID syntax checker instance */
    private UuidSyntaxChecker uuidChecker = new UuidSyntaxChecker();

    /** The CSN syntax checker instance */
    private CsnSyntaxChecker csnChecker = new CsnSyntaxChecker();

    private GeneralizedTimeSyntaxChecker timeChecker = new GeneralizedTimeSyntaxChecker();

    private static final Map<String, AttributeTypeOptions> MANDATORY_ENTRY_ATOP_MAP = new HashMap<String, AttributeTypeOptions>();

    private boolean isConfigPartitionFirstExtraction = false;

    private boolean isSchemaPartitionFirstExtraction = false;

    private InstanceLayout instanceLayout = new InstanceLayout("workDir");

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
        FileUtils.deleteDirectory(instanceLayout.getInstanceDirectory());

        File partitionsDir = instanceLayout.getPartitionsDirectory();
        if (!partitionsDir.exists()) {
            if (!partitionsDir.mkdirs()) {
                throw new IOException(I18n.err(I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, partitionsDir));
            }
        }

        initSchemaManager();
        initSchemaLdifPartition();
        initConfigPartition();

        // Read the configuration
        cpReader = new ConfigPartitionReader(configPartition);

        ConfigBean configBean = cpReader.readConfig();

        DirectoryServiceBean directoryServiceBean = configBean.getDirectoryServiceBean();

        // Initialize the DirectoryService now
        initDirectoryService(directoryServiceBean);

        // start the LDAP server
        final LdapServerBean ldapServerBean = directoryServiceBean.getLdapServerBean();
        final TcpTransportBean tcpTransportBean = new TcpTransportBean();
        tcpTransportBean.setSystemPort(getPort());
        ldapServerBean.setTransports(tcpTransportBean);
        startLdap(ldapServerBean);
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
        // Stops the server
        if (ldapServer != null) {
            ldapServer.stop();
        }

        // We now have to stop the underlaying DirectoryService
        directoryService.shutdown();

        FileUtils.deleteDirectory(instanceLayout.getInstanceDirectory());
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
     * Adds a partition with given ID and suffix to the directory.
     * 
     * @param id partition ID, e.g. "jboss"
     * @param suffix partition suffix, e.g. "dc=jboss,dc=org"
     * @throws Exception
     */
    protected final void addPartition(final String id, final String suffix) throws Exception {
        final JdbmPartition tcPartition = new JdbmPartition(schemaManager);
        tcPartition.setId(id);
        tcPartition.setCacheSize(1000);
        tcPartition.setPartitionPath(new File(instanceLayout.getPartitionsDirectory(), id).toURI());

        Set<Index<?, Entry, Long>> indexedAttrs = new HashSet<Index<?, Entry, Long>>();
        indexedAttrs.add(new JdbmIndex<Object, Entry>("objectClass"));
        indexedAttrs.add(new JdbmIndex<Object, Entry>("o"));
        tcPartition.setIndexedAttributes(indexedAttrs);

        //Add suffix
        tcPartition.setSuffixDn(new Dn(suffix));
        tcPartition.initialize();

        directoryService.addPartition(tcPartition);
    }

    /**
     * Imports the LDIF entries packaged with the Eve JNDI provider jar into the newly created system partition to prime it up
     * for operation. Note that only ou=system entries will be added - entries for other partitions cannot be imported and will
     * blow chunks.
     * 
     * @throws LdapConfigurationException if there are problems reading the ldif file and adding those entries to the system
     *         partition
     * @param in the input stream with the ldif
     */
    protected final void importLdif(InputStream in) throws LdapConfigurationException {
        try {
            for (LdifEntry ldifEntry : new LdifReader(in)) {
                directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
            }
        } catch (Exception e) {
            LOGGER.error(e);
            String msg = "failed while trying to parse system ldif file";
            LdapConfigurationException ne = new LdapConfigurationException(msg, e);
            throw ne;
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Initialize the schema Manager by loading the schema LDIF files
     * 
     * @throws Exception in case of any problems while extracting and writing the schema files
     */
    private void initSchemaManager() throws Exception {
        File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), "schema");

        // Extract the schema on disk (a brand new one) and load the registries
        if (schemaPartitionDirectory.exists()) {
            LOGGER.info("schema partition already exists, skipping schema extraction");
        } else {
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory());
            extractor.extractOrCopy();
            isSchemaPartitionFirstExtraction = true;
        }

        SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
        schemaManager = new DefaultSchemaManager(loader);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if (errors.size() != 0) {
            throw new Exception(I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)));
        }
    }

    /**
     * Initialize the schema partition
     * 
     * @throws Exception in case of any problems while initializing the SchemaPartition
     */
    private void initSchemaLdifPartition() throws Exception {
        File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), "schema");

        // Init the LdifPartition
        schemaLdifPartition = new LdifPartition(schemaManager);
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());
    }

    /**
     * 
     * initializes a LDIF partition for configuration
     * 
     * @throws Exception in case of any issues while extracting the schema
     */
    private void initConfigPartition() throws Exception {
        File confFile = new File(instanceLayout.getConfDirectory(), LdifConfigExtractor.LDIF_CONFIG_FILE);

        if (confFile.exists()) {
            LOGGER.info("config partition already exists, skipping default config extraction");
        } else {
            LdifConfigExtractor.extractSingleFileConfig(instanceLayout.getConfDirectory(),
                    LdifConfigExtractor.LDIF_CONFIG_FILE, true);
            isConfigPartitionFirstExtraction = true;
        }

        configPartition = new SingleFileLdifPartition(schemaManager);
        configPartition.setId("config");
        configPartition.setPartitionPath(confFile.toURI());
        configPartition.setSuffixDn(new Dn(schemaManager, "ou=config"));
        configPartition.setSchemaManager(schemaManager);

        configPartition.initialize();
    }

    /**
     * Creates and initializes a {@link DirectoryService} instance.
     * 
     * @param directoryServiceBean
     * @throws Exception
     */
    private void initDirectoryService(DirectoryServiceBean directoryServiceBean) throws Exception {
        LOGGER.info("Initializing the DirectoryService...");

        directoryService = ApacheDirectoryServiceBuilder.createDirectoryService(directoryServiceBean, instanceLayout, schemaManager);

        // The schema partition
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        directoryService.setSchemaPartition(schemaPartition);

        directoryService.addPartition(configPartition);

        // Store the default directories
        directoryService.setInstanceLayout(instanceLayout);

        configureDirectoryService();

        directoryService.startup();

        AttributeType ocAt = schemaManager.lookupAttributeTypeRegistry(SchemaConstants.OBJECT_CLASS_AT);
        MANDATORY_ENTRY_ATOP_MAP.put(ocAt.getName(), new AttributeTypeOptions(ocAt));

        AttributeType uuidAt = schemaManager.lookupAttributeTypeRegistry(SchemaConstants.ENTRY_UUID_AT);
        MANDATORY_ENTRY_ATOP_MAP.put(uuidAt.getName(), new AttributeTypeOptions(uuidAt));

        AttributeType csnAt = schemaManager.lookupAttributeTypeRegistry(SchemaConstants.ENTRY_CSN_AT);
        MANDATORY_ENTRY_ATOP_MAP.put(csnAt.getName(), new AttributeTypeOptions(csnAt));

        AttributeType creatorAt = schemaManager.lookupAttributeTypeRegistry(SchemaConstants.CREATORS_NAME_AT);
        MANDATORY_ENTRY_ATOP_MAP.put(creatorAt.getName(), new AttributeTypeOptions(creatorAt));

        AttributeType createdTimeAt = schemaManager.lookupAttributeTypeRegistry(SchemaConstants.CREATE_TIMESTAMP_AT);
        MANDATORY_ENTRY_ATOP_MAP.put(createdTimeAt.getName(), new AttributeTypeOptions(createdTimeAt));

        if (isConfigPartitionFirstExtraction) {
            LOGGER.info("begining to update config partition LDIF files after modifying manadatory attributes");

            // disable writes to the disk upon every modification to improve performance
            configPartition.setEnableRewriting(false);

            // perform updates
            updateMandatoryOpAttributes(configPartition);

            // enable writes to disk, this will save the partition data first if found dirty
            configPartition.setEnableRewriting(true);

            LOGGER.info("config partition data was successfully updated");
        }

        if (isSchemaPartitionFirstExtraction) {
            LOGGER.info("begining to update schema partition LDIF files after modifying manadatory attributes");

            updateMandatoryOpAttributes(schemaLdifPartition);

            LOGGER.info("schema partition data was successfully updated");
        }

        LOGGER.info("DirectoryService initialized");

    }

    /**
     * Start the LDAP server
     */
    private void startLdap(LdapServerBean ldapServerBean) throws Exception {
        LOGGER.info("Starting the LDAP server");
        ldapServer = ApacheDirectoryServiceBuilder.createLdapServer(ldapServerBean, directoryService);

        if (ldapServer == null) {
            LOGGER.info("Cannot find any reference to the LDAP Server in the configuration : the server won't be started");
            return;
        }

        ldapServer.setDirectoryService(directoryService);

        configureLdapServer();

        // And start the server now
        ldapServer.start();
        LOGGER.info("LDAP server started");
    }

    /**
     * Adds mandatory operational attributes {@link #MANDATORY_ENTRY_ATOP_MAP} and updates all the LDIF files. WARN: this method
     * is only called for the first time when schema and config files are bootstrapped afterwards it is the responsibility of
     * the user to ensure correctness of LDIF files if modified by hand
     * 
     * Note: we do these modifications explicitly cause we have no idea if each entry's LDIF file has the correct values for all
     * these mandatory attributes
     * 
     * @param partition instance of the partition Note: should only be those which are loaded before starting the
     *        DirectoryService
     * @param dirService the DirectoryService instance
     * @throws Exception
     */
    private void updateMandatoryOpAttributes(Partition partition) throws Exception {
        CoreSession session = directoryService.getAdminSession();

        String adminDn = session.getEffectivePrincipal().getName();

        ExprNode filter = new PresenceNode(SchemaConstants.OBJECT_CLASS_AT);

        EntryFilteringCursor cursor = session.search(partition.getSuffixDn(), SearchScope.SUBTREE, filter,
                AliasDerefMode.NEVER_DEREF_ALIASES, new HashSet<AttributeTypeOptions>(MANDATORY_ENTRY_ATOP_MAP.values()));
        cursor.beforeFirst();

        List<Modification> mods = new ArrayList<Modification>();

        while (cursor.next()) {
            Entry entry = cursor.get();

            AttributeType atType = MANDATORY_ENTRY_ATOP_MAP.get(SchemaConstants.ENTRY_UUID_AT).getAttributeType();

            Attribute uuidAt = entry.get(atType);
            String uuid = (uuidAt == null ? null : uuidAt.getString());

            if (!uuidChecker.isValidSyntax(uuid)) {
                uuidAt = new DefaultAttribute(atType, UUID.randomUUID().toString());
            }

            Modification uuidMod = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, uuidAt);
            mods.add(uuidMod);

            atType = MANDATORY_ENTRY_ATOP_MAP.get(SchemaConstants.ENTRY_CSN_AT).getAttributeType();
            Attribute csnAt = entry.get(atType);
            String csn = (csnAt == null ? null : csnAt.getString());

            if (!csnChecker.isValidSyntax(csn)) {
                csnAt = new DefaultAttribute(atType, directoryService.getCSN().toString());
            }

            Modification csnMod = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, csnAt);
            mods.add(csnMod);

            atType = MANDATORY_ENTRY_ATOP_MAP.get(SchemaConstants.CREATORS_NAME_AT).getAttributeType();
            Attribute creatorAt = entry.get(atType);
            String creator = (creatorAt == null ? "" : creatorAt.getString().trim());

            if ((creator.length() == 0) || (!Dn.isValid(creator))) {
                creatorAt = new DefaultAttribute(atType, adminDn);
            }

            Modification creatorMod = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, creatorAt);
            mods.add(creatorMod);

            atType = MANDATORY_ENTRY_ATOP_MAP.get(SchemaConstants.CREATE_TIMESTAMP_AT).getAttributeType();
            Attribute createdTimeAt = entry.get(atType);
            String createdTime = (createdTimeAt == null ? null : createdTimeAt.getString());

            if (!timeChecker.isValidSyntax(createdTime)) {
                createdTimeAt = new DefaultAttribute(atType, DateUtils.getGeneralizedTime());
            }

            Modification createdMod = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, createdTimeAt);
            mods.add(createdMod);

            if (!mods.isEmpty()) {
                ModifyOperationContext modifyContext = new ModifyOperationContext(session);
                modifyContext.setEntry(entry);
                modifyContext.setDn(entry.getDn());
                modifyContext.setModItems(mods);
                partition.modify(modifyContext);
            }

            mods.clear();
        }

        cursor.close();
    }

}
