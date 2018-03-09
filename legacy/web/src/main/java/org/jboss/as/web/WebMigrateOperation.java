/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import static io.undertow.util.Headers.X_FORWARDED_FOR_STRING;
import static io.undertow.util.Headers.X_FORWARDED_PROTO_STRING;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.MultistepUtil;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.wildfly.extension.io.IOExtension;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.filters.CustomFilterDefinition;
import org.wildfly.extension.undertow.filters.ExpressionFilterDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.wildfly.extension.undertow.UndertowExtension.PATH_FILTERS;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Operation to migrate from the legacy web subsystem to the new undertow subsystem.
 * <p/>
 * This operation must be performed when the server is in admin-only mode. Internally, the operation:
 * <p/>
 * <ul>
 * <li>query the description of all the web subsystem by invoking the :describe operation. This returns a list of :add
 * operations for each web resources.</li>
 * <li>:add the new org.widlfy.extension.undertow extension</li>
 * <li>:add the new org.wildfly.extension.io</li>
 * <li>for each web resources, transform the :add operations to add the corresponding resource to the new undertow
 * subsystem. In this step, changes to the resources model are taken into account</li>
 * <li>:remove the web subsystem</li>
 * </ul>
 * <p/>
 * The companion <code>:describe-migration</code> operation will return a list of all the actual operations that would
 * be performed during the invocation of the <code>:migrate</code> operation.
 * <p/>
 * Note that all new operation addresses are generated for standalone mode. If this is a domain mode server then the
 * addresses are fixed after they have been generated
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 * @author Stuart Douglas
 */
public class WebMigrateOperation implements OperationStepHandler {

    private static final String UNDERTOW_EXTENSION = "org.wildfly.extension.undertow";
    private static final String IO_EXTENSION = "org.wildfly.extension.io";

    private static final String REALM_NAME = "jbossweb-migration-security-realm";

    private static final OperationStepHandler DESCRIBE_MIGRATION_INSTANCE = new WebMigrateOperation(true);
    private static final OperationStepHandler MIGRATE_INSTANCE = new WebMigrateOperation(false);
    public static final PathElement DEFAULT_SERVER_PATH = pathElement(Constants.SERVER, "default-server");
    public static final PathAddress EXTENSION_ADDRESS = pathAddress(pathElement(EXTENSION, "org.jboss.as.web"));
    private static final PathAddress VALVE_ACCESS_LOG_ADDRESS = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST), UndertowExtension.PATH_ACCESS_LOG);
    public static final String MIGRATE = "migrate";
    public static final String MIGRATION_WARNINGS = "migration-warnings";
    public static final String MIGRATION_ERROR = "migration-error";
    public static final String MIGRATION_OPERATIONS = "migration-operations";
    public static final String DESCRIBE_MIGRATION = "describe-migration";

    private static final Pattern ACCESS_LOG_PATTERN = Pattern.compile("%\\{(.*?)\\}(\\w)");

    public static final StringListAttributeDefinition MIGRATION_WARNINGS_ATTR = new StringListAttributeDefinition.Builder(MIGRATION_WARNINGS)
            .setRequired(false)
            .build();

    public static final SimpleMapAttributeDefinition MIGRATION_ERROR_ATTR = new SimpleMapAttributeDefinition.Builder(MIGRATION_ERROR, ModelType.OBJECT, true)
            .setValueType(ModelType.OBJECT)
            .setRequired(false)
            .build();

    private final boolean describe;

    private WebMigrateOperation(boolean describe) {

        this.describe = describe;
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(MIGRATE, resourceDescriptionResolver)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .build(),
                WebMigrateOperation.MIGRATE_INSTANCE);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(DESCRIBE_MIGRATION, resourceDescriptionResolver)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR)
                        .setReadOnly()
                        .build(),
                WebMigrateOperation.DESCRIBE_MIGRATION_INSTANCE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw WebLogger.ROOT_LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        final List<String> warnings = new ArrayList<>();

        // node containing the description (list of add operations) of the legacy subsystem
        final ModelNode legacyModelAddOps = new ModelNode();
        //we don't preserve order, instead we sort by address length
        final Map<PathAddress, ModelNode> sortedMigrationOperations = new TreeMap<>(new Comparator<PathAddress>() {
            @Override
            public int compare(PathAddress o1, PathAddress o2) {
                final int compare = Integer.compare(o1.size(), o2.size());
                if (compare != 0) {
                    return compare;
                }
                return o1.toString().compareTo(o2.toString());
            }
        });

        // invoke an OSH to describe the legacy messaging subsystem
        describeLegacyWebResources(context, legacyModelAddOps);
        // invoke an OSH to add the messaging-activemq extension
        // FIXME: this does not work it the extension :add is added to the migrationOperations directly (https://issues.jboss.org/browse/WFCORE-323)
        addExtension(context, sortedMigrationOperations, describe, UNDERTOW_EXTENSION);
        addExtension(context, sortedMigrationOperations, describe, IO_EXTENSION);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                addDefaultResources(sortedMigrationOperations, legacyModelAddOps, warnings);
                // transform the legacy add operations and put them in migrationOperations
                ProcessType processType = context.getCallEnvironment().getProcessType();
                boolean domainMode = processType != ProcessType.STANDALONE_SERVER && processType != ProcessType.SELF_CONTAINED;
                PathAddress baseAddres;
                if(domainMode) {
                    baseAddres = pathAddress(operation.get(ADDRESS)).getParent();
                } else {
                    baseAddres = pathAddress();
                }
                //create the new IO subsystem
                createIoSubsystem(context, sortedMigrationOperations, baseAddres);

                createWelcomeContentHandler(sortedMigrationOperations);

                transformResources(context, legacyModelAddOps, sortedMigrationOperations, warnings, domainMode);

                fixAddressesForDomainMode(pathAddress(operation.get(ADDRESS)), sortedMigrationOperations);

                // put the /subsystem=web:remove operation as first
                LinkedHashMap<PathAddress, ModelNode> orderedMigrationOperations = new LinkedHashMap<>();
                removeWebSubsystem(orderedMigrationOperations, context.getProcessType() == ProcessType.STANDALONE_SERVER, pathAddress(operation.get(ADDRESS)));
                orderedMigrationOperations.putAll(sortedMigrationOperations);

                if (describe) {
                    // :describe-migration operation

                    // for describe-migration operation, do nothing and return the list of operations that would
                    // be executed in the composite operation
                    final Collection<ModelNode> values = orderedMigrationOperations.values();
                    ModelNode result = new ModelNode();
                    if(!warnings.isEmpty()) {
                        ModelNode rw = new ModelNode().setEmptyList();
                        for (String warning : warnings) {
                            rw.add(warning);
                        }
                        result.get(MIGRATION_WARNINGS).set(rw);
                    }

                    result.get(MIGRATION_OPERATIONS).set(values);

                    context.getResult().set(result);
                } else {
                    // :migrate operation
                    // invoke an OSH on a composite operation with all the migration operations
                    final Map<PathAddress, ModelNode> migrateOpResponses = migrateSubsystems(context, orderedMigrationOperations);

                    context.completeStep(new OperationContext.ResultHandler() {
                        @Override
                        public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                            final ModelNode result = new ModelNode();
                            ModelNode rw = new ModelNode().setEmptyList();
                            for (String warning : warnings) {
                                rw.add(warning);
                            }
                            result.get(MIGRATION_WARNINGS).set(rw);
                            if (resultAction == OperationContext.ResultAction.ROLLBACK) {
                                for (Map.Entry<PathAddress, ModelNode> entry : migrateOpResponses.entrySet()) {
                                    if (entry.getValue().hasDefined(FAILURE_DESCRIPTION)) {
                                        //we check for failure description, as every node has 'failed', but one
                                        //the real error has a failure description
                                        //we break when we find the first one, as there will only ever be one failure
                                        //as the op stops after the first failure
                                        ModelNode desc = new ModelNode();
                                        desc.get(OP).set(orderedMigrationOperations.get(entry.getKey()));
                                        desc.get(RESULT).set(entry.getValue());
                                        result.get(MIGRATION_ERROR).set(desc);
                                        break;
                                    }
                                }
                                context.getFailureDescription().set(new ModelNode(WebLogger.ROOT_LOGGER.migrationFailed()));
                            }

                            context.getResult().set(result);
                        }
                    });
                }
            }
        }, MODEL);
    }

    /**
     * Creates the security realm
     *
     * @param context
     * @param migrationOperations
     * @return
     */
    private SSLInformation createSecurityRealm(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, ModelNode legacyModelAddOps, String connector, List<String> warnings, boolean domainMode) {
        ModelNode legacyAddOp = findResource(pathAddress(WebExtension.SUBSYSTEM_PATH, pathElement(WebExtension.CONNECTOR_PATH.getKey(), connector), pathElement("configuration", "ssl")), legacyModelAddOps);
        if (legacyAddOp == null) {
            return null;
        }
        //we have SSL


        //read all the info from the SSL definition
        ModelNode keyAlias = legacyAddOp.get(WebSSLDefinition.KEY_ALIAS.getName());
        ModelNode password = legacyAddOp.get(WebSSLDefinition.PASSWORD.getName());
        ModelNode certificateKeyFile = legacyAddOp.get(WebSSLDefinition.CERTIFICATE_KEY_FILE.getName());
        ModelNode cipherSuite = legacyAddOp.get(WebSSLDefinition.CIPHER_SUITE.getName());
        ModelNode protocol = legacyAddOp.get(WebSSLDefinition.PROTOCOL.getName());
        ModelNode verifyClient = legacyAddOp.get(WebSSLDefinition.VERIFY_CLIENT.getName());
        ModelNode verifyDepth = legacyAddOp.get(WebSSLDefinition.VERIFY_DEPTH.getName());
        ModelNode certificateFile = legacyAddOp.get(WebSSLDefinition.CERTIFICATE_FILE.getName());
        ModelNode caCertificateFile = legacyAddOp.get(WebSSLDefinition.CA_CERTIFICATE_FILE.getName());
        ModelNode caCertificatePassword = legacyAddOp.get(WebSSLDefinition.CA_CERTIFICATE_PASSWORD.getName());
        ModelNode csRevocationURL = legacyAddOp.get(WebSSLDefinition.CA_REVOCATION_URL.getName());
        ModelNode trustStoreType = legacyAddOp.get(WebSSLDefinition.TRUSTSTORE_TYPE.getName());
        ModelNode keystoreType = legacyAddOp.get(WebSSLDefinition.KEYSTORE_TYPE.getName());
        ModelNode sessionCacheSize = legacyAddOp.get(WebSSLDefinition.SESSION_CACHE_SIZE.getName());
        ModelNode sessionTimeout = legacyAddOp.get(WebSSLDefinition.SESSION_TIMEOUT.getName());
        ModelNode sslProvider = legacyAddOp.get(WebSSLDefinition.SSL_PROTOCOL.getName());

        if(verifyDepth.isDefined()) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.VERIFY_DEPTH.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
        }
        if(certificateFile.isDefined()) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.CERTIFICATE_FILE.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
        }
        if(sslProvider.isDefined()) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.SSL_PROTOCOL.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
        }

        if(csRevocationURL.isDefined()) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.CA_REVOCATION_URL.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
        }
        String realmName;
        PathAddress managementCoreService;
        if(domainMode) {
            Set<String> hosts = new HashSet<>();
            Resource hostResource = context.readResourceFromRoot(pathAddress(), false);
            hosts.addAll(hostResource.getChildrenNames(HOST));

            //now we need to find a unique name
            //in domain mode different profiles could have different SSL configurations
            //but the realms are not scoped to a profile
            //if we hard coded a name migration would fail when migrating domains with multiple profiles
            int counter = 1;
            realmName = REALM_NAME + counter;
            while(true) {
                boolean hostOk = true;
                for(String host : hosts) {
                    Resource root = context.readResourceFromRoot(pathAddress(pathElement(HOST, host), pathElement(CORE_SERVICE, MANAGEMENT)), false);
                    if (root.getChildrenNames(SECURITY_REALM).contains(realmName)) {
                        counter++;
                        realmName = REALM_NAME + counter;
                        hostOk = false;
                        break;
                    }
                }
                if(hostOk) {
                    break;
                }
            }
            for (String host : hosts) {
                createHostSSLConfig(realmName, migrationOperations, keyAlias, password, certificateKeyFile, protocol, caCertificateFile, caCertificatePassword, trustStoreType, keystoreType, pathAddress(pathElement(HOST, host), pathElement(CORE_SERVICE, MANAGEMENT)));
            }

        } else {
            managementCoreService = pathAddress(CORE_SERVICE, MANAGEMENT);
            //now we need to find a unique name
            //in domain mode different profiles could have different SSL configurations
            //but the realms are not scoped to a profile
            //if we hard coded a name migration would fail when migrating domains with multiple profiles
            int counter = 1;
            realmName = REALM_NAME + counter;
            boolean ok = false;
            do {
                Resource root = context.readResourceFromRoot(managementCoreService, false);
                if (root.getChildrenNames(SECURITY_REALM).contains(realmName)) {
                    counter++;
                    realmName = REALM_NAME + counter;
                } else {
                    ok = true;
                }
            } while (!ok);

            //we have a unique realm name
            createHostSSLConfig(realmName, migrationOperations, keyAlias, password, certificateKeyFile, protocol, caCertificateFile, caCertificatePassword, trustStoreType, keystoreType, managementCoreService);

        }



        return new SSLInformation(realmName, verifyClient, sessionCacheSize, sessionTimeout, protocol, cipherSuite);
    }

    private String createHostSSLConfig(String realmName, Map<PathAddress, ModelNode> migrationOperations, ModelNode keyAlias, ModelNode password, ModelNode certificateKeyFile, ModelNode protocol, ModelNode caCertificateFile, ModelNode caCertificatePassword, ModelNode trustStoreType, ModelNode keystoreType, PathAddress managementCoreService) {

        //add the realm
        PathAddress addres = pathAddress(managementCoreService, pathElement(SECURITY_REALM, realmName));
        migrationOperations.put(addres, createAddOperation(addres));

        //now lets add the trust store
        addres = pathAddress(managementCoreService, pathElement(SECURITY_REALM, realmName), pathElement(AUTHENTICATION, TRUSTSTORE));
        ModelNode addOp = createAddOperation(addres);
        addOp.get(ModelDescriptionConstants.KEYSTORE_PATH).set(caCertificateFile);
        addOp.get(ModelDescriptionConstants.KEYSTORE_PASSWORD).set(caCertificatePassword);
        addOp.get(ModelDescriptionConstants.KEYSTORE_PROVIDER).set(trustStoreType);
        migrationOperations.put(addres, addOp);


        //now lets add the key store
        addres = pathAddress(managementCoreService, pathElement(SECURITY_REALM, realmName), pathElement(SERVER_IDENTITY, SSL));
        addOp = createAddOperation(addres);
        addOp.get(ModelDescriptionConstants.KEYSTORE_PATH).set(certificateKeyFile);
        addOp.get(ModelDescriptionConstants.KEYSTORE_PASSWORD).set(password);
        addOp.get(ModelDescriptionConstants.KEYSTORE_PROVIDER).set(keystoreType);
        addOp.get(ModelDescriptionConstants.ALIAS).set(keyAlias);
        addOp.get(PROTOCOL).set(protocol);
        //addOp.get(KeystoreAttributes.KEY_PASSWORD.getName()).set(password); //TODO: is this correct? both key and keystore have same password?

        migrationOperations.put(addres, addOp);
        return realmName;
    }

    private void fixAddressesForDomainMode(PathAddress migrateAddress, Map<PathAddress, ModelNode> migrationOperations) {
        int i = 0;
        while (i < migrateAddress.size()) {
            if (migrateAddress.getElement(i).equals(WebExtension.SUBSYSTEM_PATH)) {
                break;
            }
            ++i;
        }
        if (i == 0) {
            //not domain mode, no need for a prefix
            return;
        }
        PathAddress prefix = migrateAddress.subAddress(0, i);
        Map<PathAddress, ModelNode> old = new HashMap<>(migrationOperations);
        migrationOperations.clear();
        for (Map.Entry<PathAddress, ModelNode> e : old.entrySet()) {
            if (e.getKey().getElement(0).getKey().equals(SUBSYSTEM)) {
                final PathAddress oldAddress = pathAddress(e.getValue().get(ADDRESS));
                List<PathElement> elements = new ArrayList<>();
                for (PathElement j : prefix) {
                    elements.add(j);
                }
                for (PathElement j : oldAddress) {
                    elements.add(j);
                }
                PathAddress newAddress = pathAddress(elements);
                e.getValue().get(ADDRESS).set(newAddress.toModelNode());
                migrationOperations.put(newAddress, e.getValue());
            } else {
                //not targeted at a subsystem
                migrationOperations.put(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * We need to create the IO subsystem, if it does not already exist
     */
    private void createIoSubsystem(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, PathAddress baseAddress) {
        Resource root = context.readResourceFromRoot(baseAddress, false);
        if (root.getChildrenNames(SUBSYSTEM).contains(IOExtension.SUBSYSTEM_NAME)) {
            // subsystem is already added, do nothing
            return;
        }

        //these addresses will be fixed later, no need to use the base address
        PathAddress address = pathAddress(pathElement(SUBSYSTEM, IOExtension.SUBSYSTEM_NAME));
        migrationOperations.put(address, createAddOperation(address));
        address = pathAddress(pathElement(SUBSYSTEM, IOExtension.SUBSYSTEM_NAME), pathElement("worker", "default"));
        migrationOperations.put(address, createAddOperation(address));
        address = pathAddress(pathElement(SUBSYSTEM, IOExtension.SUBSYSTEM_NAME), pathElement("buffer-pool", "default"));
        migrationOperations.put(address, createAddOperation(address));

    }


    /**
     * create a handler for serving welcome content
     */
    private void createWelcomeContentHandler(Map<PathAddress, ModelNode> migrationOperations) {

        PathAddress address = pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), pathElement(Constants.CONFIGURATION, Constants.HANDLER));
        migrationOperations.put(address, createAddOperation(address));

        address = pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), pathElement(Constants.CONFIGURATION, Constants.HANDLER), pathElement(Constants.FILE, "welcome-content"));
        final ModelNode add = createAddOperation(address);
        add.get(Constants.PATH).set(new ModelNode(new ValueExpression("${jboss.home.dir}/welcome-content")));
        migrationOperations.put(address, add);
    }

    private void addDefaultResources(Map<PathAddress, ModelNode> migrationOperations, final ModelNode legacyModelDescription, List<String> warnings) {
        //add the default server
        PathAddress address = pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), DEFAULT_SERVER_PATH);
        ModelNode add = createAddOperation(address);

        ModelNode defaultSessionTimeout = null;

        //static resources
        ModelNode directoryListing = null;
        //todo: add support for some of these
        ModelNode sendfile = null;
        ModelNode fileEncoding = null;
        ModelNode readOnly = null;
        ModelNode webdav = null;
        ModelNode secret = null;
        ModelNode maxDepth = null;
        ModelNode disabled = null;

        for (ModelNode legacyAddOp : legacyModelDescription.get(RESULT).asList()) {
            final PathAddress la = pathAddress(legacyAddOp.get(ADDRESS));
            if (la.equals(pathAddress(WebExtension.SUBSYSTEM_PATH))) {
                ModelNode defaultHost = legacyAddOp.get(WebDefinition.DEFAULT_VIRTUAL_SERVER.getName());
                if (defaultHost.isDefined()) {
                    add.get(Constants.DEFAULT_HOST).set(defaultHost.clone());
                }
                ModelNode sessionTimeout = legacyAddOp.get(WebDefinition.DEFAULT_SESSION_TIMEOUT.getName());
                if (sessionTimeout.isDefined()) {
                    defaultSessionTimeout = sessionTimeout;
                }
            } else if (la.equals(pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.STATIC_RESOURCES_PATH))) {
                ModelNode node = legacyAddOp.get(WebStaticResources.LISTINGS.getName());
                if (node.isDefined()) {
                    directoryListing = node;
                }
                node = legacyAddOp.get(WebStaticResources.SENDFILE.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.SENDFILE.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    sendfile = node;
                }
                node = legacyAddOp.get(WebStaticResources.FILE_ENCODING.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.FILE_ENCODING.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    fileEncoding = node;
                }
                node = legacyAddOp.get(WebStaticResources.READ_ONLY.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.READ_ONLY.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    readOnly = node;
                }
                node = legacyAddOp.get(WebStaticResources.WEBDAV.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.WEBDAV.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    webdav = node;
                }
                node = legacyAddOp.get(WebStaticResources.SECRET.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.SECRET.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    secret = node;
                }
                node = legacyAddOp.get(WebStaticResources.MAX_DEPTH.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.MAX_DEPTH.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    maxDepth = node;
                }
                node = legacyAddOp.get(WebStaticResources.DISABLED.getName());
                if (node.isDefined()) {
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.DISABLED.getName(), pathAddress(legacyAddOp.get(ADDRESS))));
                    disabled = node;
                }
            }
        }

        migrationOperations.put(address, add);

        address = pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), pathElement(Constants.BUFFER_CACHE, "default"));
        add = createAddOperation(address);
        migrationOperations.put(address, add);

        address = pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), pathElement(Constants.SERVLET_CONTAINER, "default"));
        add = createAddOperation(address);
        if (defaultSessionTimeout != null) {
            add.get(Constants.DEFAULT_SESSION_TIMEOUT).set(defaultSessionTimeout.clone());
        }
        if (directoryListing != null) {
            add.get(Constants.DIRECTORY_LISTING).set(directoryListing);
        }
        migrationOperations.put(address, add);
    }

    /**
     * It's possible that the extension is already present. In that case, this method does nothing.
     */
    private void addExtension(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, boolean describe, String extension) {
        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        if (root.getChildrenNames(EXTENSION).contains(extension)) {
            // extension is already added, do nothing
            return;
        }
        PathAddress extensionAddress = pathAddress(EXTENSION, extension);
        OperationEntry addEntry = context.getRootResourceRegistration().getOperationEntry(extensionAddress, ADD);
        ModelNode addOperation = createAddOperation(extensionAddress);
        addOperation.get(MODULE).set(extension);
        if (describe) {
            migrationOperations.put(extensionAddress, addOperation);
        } else {
            context.addStep(context.getResult().get(extensionAddress.toString()), addOperation, addEntry.getOperationHandler(), MODEL);
        }
    }

    private void removeWebSubsystem(Map<PathAddress, ModelNode> migrationOperations, boolean standalone, PathAddress subsystemAddress) {
        ModelNode removeOperation = createRemoveOperation(subsystemAddress);
        migrationOperations.put(subsystemAddress, removeOperation);

        //only add this if we are describing
        //to maintain the order we manually execute this last
        if(standalone) {
            removeOperation = createRemoveOperation(EXTENSION_ADDRESS);
            migrationOperations.put(EXTENSION_ADDRESS, removeOperation);
        }
    }

    private Map<PathAddress, ModelNode> migrateSubsystems(OperationContext context, final Map<PathAddress, ModelNode> migrationOperations) throws OperationFailedException {
        final Map<PathAddress, ModelNode> result = new LinkedHashMap<>();
        MultistepUtil.recordOperationSteps(context, migrationOperations, result);
        return result;
    }

    private void transformResources(final OperationContext context, final ModelNode legacyModelDescription, final Map<PathAddress, ModelNode> newAddOperations, List<String> warnings, boolean domainMode) throws OperationFailedException {
        Set<String> hosts = new LinkedHashSet<>();
        for (ModelNode legacyAddOp : legacyModelDescription.get(RESULT).asList()) {
            final ModelNode newAddOp = legacyAddOp.clone();
            PathAddress address = pathAddress(newAddOp.get(ADDRESS));

            if (address.size() == 1) {
                //subsystem
                migrateSubsystem(newAddOperations, newAddOp);
            } else if (address.equals(pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.STATIC_RESOURCES_PATH))) {
                //covered in the servlet container add, so just ignore
            } else if (address.equals(pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.JSP_CONFIGURATION_PATH))) {
                migrateJSPConfig(newAddOperations, newAddOp);
            } else if (address.equals(pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.CONTAINER_PATH))) {
                migrateMimeMapping(newAddOperations, newAddOp);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.CONNECTOR_PATH))) {
                migrateConnector(context, newAddOperations, newAddOp, address, legacyModelDescription, warnings, domainMode);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.CONNECTOR_PATH, WebExtension.SSL_PATH))) {
                // ignore, handled as part of connector migration
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH))) {
                String host = address.getLastElement().getValue();
                hosts.add(host);
                migrateVirtualHost(newAddOperations, newAddOp, host);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.VALVE_PATH))) {
                migrateValves(newAddOperations, newAddOp, address, warnings);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH, WebExtension.ACCESS_LOG_PATH))) {
                migrateAccessLog(newAddOperations, newAddOp, address, legacyModelDescription, warnings);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH, WebExtension.ACCESS_LOG_PATH, WebExtension.DIRECTORY_PATH))) {
                //ignore, handled by access-log
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH, WebExtension.SSO_PATH))) {
                migrateSso(newAddOperations, newAddOp, address, warnings);
            } else {
                warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(legacyAddOp));
            }
        }
        if (!hosts.isEmpty()) {
            migrateVirtualHostChildren(newAddOperations, hosts);
        }
        newAddOperations.remove(VALVE_ACCESS_LOG_ADDRESS);
    }

    private void migrateSso(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address, List<String> warnings) {
        PathAddress newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, address.getElement(address.size() - 2).getValue()), UndertowExtension.PATH_SSO);
        ModelNode add = createAddOperation(newAddress);

        add.get(Constants.DOMAIN).set(newAddOp.get(WebSSODefinition.DOMAIN.getName()).clone());
        add.get(Constants.HTTP_ONLY).set(newAddOp.get(WebSSODefinition.HTTP_ONLY.getName()).clone());

        if (newAddOp.hasDefined(WebSSODefinition.CACHE_CONTAINER.getName())) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSODefinition.CACHE_CONTAINER.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }
        if (newAddOp.hasDefined(WebSSODefinition.REAUTHENTICATE.getName())) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSODefinition.REAUTHENTICATE.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }
        if (newAddOp.hasDefined(WebSSODefinition.CACHE_NAME.getName())) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSODefinition.CACHE_NAME.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }

        newAddOperations.put(newAddress, add);
    }

    private void migrateAccessLog(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address, ModelNode legacyAddOps, List<String> warnings) {
        PathAddress newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, address.getElement(address.size() - 2).getValue()), UndertowExtension.PATH_ACCESS_LOG);
        ModelNode add = createAddOperation(newAddress);

        //TODO: parse the pattern and modify to Undertow version
        ModelNode patternNode = newAddOp.get(WebAccessLogDefinition.PATTERN.getName());
        if(patternNode.isDefined()) {
            add.get(Constants.PATTERN).set(migrateAccessLogPattern(patternNode.asString()));
        }
        add.get(Constants.PREFIX).set(newAddOp.get(WebAccessLogDefinition.PREFIX.getName()).clone());
        add.get(Constants.ROTATE).set(newAddOp.get(WebAccessLogDefinition.ROTATE.getName()).clone());
        if (newAddOp.hasDefined(WebAccessLogDefinition.RESOLVE_HOSTS.getName())) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebAccessLogDefinition.RESOLVE_HOSTS.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }

        add.get(Constants.EXTENDED).set(newAddOp.get(WebAccessLogDefinition.EXTENDED.getName()).clone());

        ModelNode directory = findResource(pathAddress(pathAddress(newAddOp.get(ADDRESS)), WebExtension.DIRECTORY_PATH), legacyAddOps);
        if(directory != null){
            add.get(Constants.DIRECTORY).set(directory.get(PATH));
            add.get(Constants.RELATIVE_TO).set(directory.get(RELATIVE_TO));
        }

        newAddOperations.put(newAddress, add);
    }

    private String migrateAccessLogPattern(String legacyPattern) {
        Matcher m = ACCESS_LOG_PATTERN.matcher(legacyPattern);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (m.find()) {
            sb.append(legacyPattern.substring(lastIndex, m.start()));
            lastIndex = m.end();
            sb.append("%{");
            sb.append(m.group(2));
            sb.append(",");
            sb.append(m.group(1));
            sb.append("}");
        }
        sb.append(legacyPattern.substring(lastIndex));
        return sb.toString();
    }

    private void migrateAccessLogValve(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, String valveName, List<String> warnings) {
        ModelNode add = createAddOperation(VALVE_ACCESS_LOG_ADDRESS);
        final ModelNode params = newAddOp.get(WebValveDefinition.PARAMS.getName());
        //TODO: parse the pattern and modify to Undertow version
        final ModelNode patternNode = params.get(Constants.PATTERN);
        if(patternNode.isDefined()) {
            add.get(Constants.PATTERN).set(migrateAccessLogPattern(patternNode.asString()));
        }
        add.get(Constants.PREFIX).set(params.get(Constants.PREFIX).clone());
        add.get(Constants.SUFFIX).set(params.get(Constants.SUFFIX).clone());
        add.get(Constants.ROTATE).set(params.get("rotatable").clone());
        add.get(Constants.EXTENDED).set(newAddOp.get(Constants.EXTENDED).clone());
        if(params.hasDefined(Constants.DIRECTORY)){
            add.get(Constants.DIRECTORY).set(params.get(Constants.DIRECTORY).clone());
        }
        if(params.hasDefined("conditionIf")) {
             add.get(Constants.PREDICATE).set("exists(%{r," + params.get("conditionIf").asString() + "})");
        }
        if(params.hasDefined("conditionUnless")) {
            add.get(Constants.PREDICATE).set("not exists(%{r," + params.get("conditionUnless").asString() + "})");
        }
        if(params.hasDefined("condition")) {
            add.get(Constants.PREDICATE).set("not exists(%{r," + params.get("condition").asString() + "})");
        }
        final String[] unsupportedConfigParams = new String[] {"resolveHosts", "fileDateFormat", "renameOnRotate", "encoding",
            "locale", "requestAttributesEnabled", "buffered"};
        for(String unsupportedConfigParam : unsupportedConfigParams) {
            if(params.hasDefined(unsupportedConfigParam)) {
                warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute(unsupportedConfigParam, valveName));
            }
        }
        newAddOperations.put(VALVE_ACCESS_LOG_ADDRESS, add);
    }

    private boolean wildcardEquals(PathAddress a1, PathAddress a2) {
        if (a1.size() != a2.size()) {
            return false;
        }
        for (int i = 0; i < a1.size(); ++i) {
            PathElement p1 = a1.getElement(i);
            PathElement p2 = a2.getElement(i);
            if (!p1.getKey().equals(p2.getKey())) {
                return false;
            }
            if (!p1.isWildcard() && !p2.isWildcard()) {
                if (!p1.getValue().equals(p2.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void migrateVirtualHost(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, String host) {
        PathAddress newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, host));
        ModelNode add = createAddOperation(newAddress);

        if (newAddOp.hasDefined(WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.getName()) && newAddOp.get(WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.getName()).asBoolean()) {
            PathAddress welcomeAddress = pathAddress(newAddress, pathElement(Constants.LOCATION, "/"));
            ModelNode welcomeAdd = createAddOperation(welcomeAddress);
            welcomeAdd.get(Constants.HANDLER).set("welcome-content");
            newAddOperations.put(welcomeAddress, welcomeAdd);
        }
        add.get(Constants.ALIAS).set(newAddOp.get(WebVirtualHostDefinition.ALIAS.getName()).clone());
        add.get(Constants.DEFAULT_WEB_MODULE).set(newAddOp.get(WebVirtualHostDefinition.DEFAULT_WEB_MODULE.getName()));

        newAddOperations.put(newAddress, add);
    }

    private void migrateVirtualHostChildren(Map<PathAddress, ModelNode> newAddOperations, Set<String> hosts) {

        final PathAddress customFilterAddresses = pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS, pathElement(CustomFilterDefinition.INSTANCE.getPathElement().getKey()));
        final PathAddress expressionFilterAddresses = pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS, pathElement(ExpressionFilterDefinition.INSTANCE.getPathElement().getKey()));
        List<PathAddress> filterAddresses = new ArrayList<>();
        for(PathAddress a : newAddOperations.keySet()) {
            if(wildcardEquals(customFilterAddresses, a) || wildcardEquals(expressionFilterAddresses, a)) {
                filterAddresses.add(a);
            }
        }

        boolean hasAccessLogValve =  newAddOperations.containsKey(VALVE_ACCESS_LOG_ADDRESS);

        if (hasAccessLogValve || !filterAddresses.isEmpty()) {
            for (String host : hosts) {
                PathAddress hostAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, host));
                for (PathAddress filterAddress : filterAddresses) {
                    PathAddress filterRefAddress = pathAddress(hostAddress, pathElement(Constants.FILTER_REF, filterAddress.getLastElement().getValue()));
                    ModelNode filterRefAdd = createAddOperation(filterRefAddress);
                    newAddOperations.put(filterRefAddress, filterRefAdd);
                }
                if (hasAccessLogValve) {
                    PathAddress accessLogAddress = pathAddress(hostAddress, UndertowExtension.PATH_ACCESS_LOG);
                    if(!newAddOperations.containsKey(accessLogAddress)) {
                        ModelNode operation = newAddOperations.get(VALVE_ACCESS_LOG_ADDRESS).clone();
                        operation.get(OP_ADDR).set(accessLogAddress.toModelNode());
                        newAddOperations.put(accessLogAddress, operation);
                    }
                }
            }
        }
    }

    private void migrateValves(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address, List<String> warnings) {
        if (newAddOp.hasDefined(WebValveDefinition.CLASS_NAME.getName())) {
            String valveClassName = newAddOp.get(WebValveDefinition.CLASS_NAME.getName()).asString();
            String valveName = address.getLastElement().getValue();
            switch (valveClassName) {
                case "org.apache.catalina.valves.CrawlerSessionManagerValve":
                    PathAddress crawlerAddress = pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), pathElement(Constants.SERVLET_CONTAINER, "default"), pathElement(Constants.SETTING, Constants.CRAWLER_SESSION_MANAGEMENT));
                    ModelNode crawlerAdd = createAddOperation(crawlerAddress);
                    if (newAddOp.hasDefined(WebValveDefinition.PARAMS.getName())) {
                        ModelNode params = newAddOp.get(WebValveDefinition.PARAMS.getName());
                        if (params.hasDefined("crawlerUserAgents")) {
                            crawlerAdd.get(Constants.USER_AGENTS).set(params.get("crawlerUserAgents"));
                        }
                        if (params.hasDefined("sessionInactiveInterval")) {
                            crawlerAdd.get(Constants.SESSION_TIMEOUT).set(params.get("sessionInactiveInterval"));
                        }
                    }
                    newAddOperations.put(crawlerAddress, crawlerAdd);
                    break;
                case "org.apache.catalina.valves.RequestDumperValve":
                    newAddOperations.putIfAbsent(pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS), createAddOperation(pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS)));
                    PathAddress filterAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS, pathElement(ExpressionFilterDefinition.INSTANCE.getPathElement().getKey(), valveName));
                    ModelNode filterAdd = createAddOperation(filterAddress);
                    filterAdd.get(ExpressionFilterDefinition.EXPRESSION.getName()).set("dump-request");
                    newAddOperations.put(filterAddress, filterAdd);
                    break;
                case "org.apache.catalina.valves.StuckThreadDetectionValve":
                    newAddOperations.putIfAbsent(pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS), createAddOperation(pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS)));
                    PathAddress filterAddressStuckThread = pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS, pathElement(ExpressionFilterDefinition.INSTANCE.getPathElement().getKey(), valveName));
                    ModelNode filterAddStuckThread = createAddOperation(filterAddressStuckThread);
                    StringBuilder expressionStruckThread = new StringBuilder("stuck-thread-detector");
                    if (newAddOp.hasDefined(WebValveDefinition.PARAMS.getName())) {
                        ModelNode params = newAddOp.get(WebValveDefinition.PARAMS.getName());
                        if (params.hasDefined("threshold")) {
                            expressionStruckThread.append("(threshhold='").append(params.get("threshold").asInt()).append("')");
                        }
                        if (params.hasDefined("interruptThreadThreshold")) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("interruptThreadThreshold", valveName));
                        }
                    }
                    filterAddStuckThread.get(ExpressionFilterDefinition.EXPRESSION.getName()).set(expressionStruckThread.toString());
                    newAddOperations.put(filterAddressStuckThread, filterAddStuckThread);
                    break;
                case "org.apache.catalina.valves.AccessLogValve":
                    newAddOp.get(WebAccessLogDefinition.EXTENDED.getName()).set(false);
                    migrateAccessLogValve(newAddOperations, newAddOp, valveName, warnings);
                    break;
                case "org.apache.catalina.valves.ExtendedAccessLogValve":
                    newAddOp.get(WebAccessLogDefinition.EXTENDED.getName()).set(true);
                    migrateAccessLogValve(newAddOperations, newAddOp, valveName, warnings);
                    break;
                case "org.apache.catalina.valves.RemoteHostValve":
                    createAccesControlExpressionFilter(newAddOperations, warnings, valveName, "%h", newAddOp);
                    break;
                case "org.apache.catalina.valves.RemoteAddrValve":
                    createAccesControlExpressionFilter(newAddOperations, warnings, valveName, "%a", newAddOp);
                    break;
                case "org.apache.catalina.valves.RemoteIpValve":
                    if (newAddOp.hasDefined(WebValveDefinition.PARAMS.getName())) {
                        StringBuilder expression = new StringBuilder();
                        ModelNode params = newAddOp.get(WebValveDefinition.PARAMS.getName());
                        if(params.hasDefined("remoteIpHeader") && ! X_FORWARDED_FOR_STRING.equalsIgnoreCase(params.get("remoteIpHeader").asString())) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("remoteIpHeader", valveName));
                        }
                        if(params.hasDefined("protocolHeader") && ! X_FORWARDED_PROTO_STRING.equalsIgnoreCase(params.get("protocolHeader").asString())) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("protocolHeader", valveName));
                        }
                        if(params.hasDefined("httpServerPort")) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("httpServerPort", valveName));
                        }
                        if(params.hasDefined("httpsServerPort")) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("httpsServerPort", valveName));
                        }
                        if(params.hasDefined("proxiesHeader")) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("proxiesHeader", valveName));
                        }
                        if(params.hasDefined("protocolHeaderHttpsValue")) {
                            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValveAttribute("protocolHeaderHttpsValue", valveName));
                        }
                        boolean trustedProxies = false;
                        if (params.hasDefined("trustedProxies")) {
                            expression.append("regex(pattern=\"").append(params.get("trustedProxies").asString()).append("\", value=%{i,x-forwarded-for}, full-match=true)");
                            trustedProxies = true;
                        }
                        String internalProxies;
                        if (params.hasDefined("internalProxies")) {
                            internalProxies = params.get("internalProxies").asString();
                        } else {
                            internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3}|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
                        }
                        if (trustedProxies) {
                            expression.append(" and ");
                        }
                        expression.append("regex(pattern=\"").append(internalProxies).append("\", value=%{i,x-forwarded-for}, full-match=true)");
                        expression.append(" -> proxy-peer-address");
                        createExpressionFilter(newAddOperations, valveName, expression.toString());
                    }
                    break;
                default:
                    warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValve(valveName));
                    break;
            }
        }
    }

    private void createAccesControlExpressionFilter(Map<PathAddress, ModelNode> newAddOperations, List<String> warnings, String name, String attribute, ModelNode newAddOp) {
        if (newAddOp.hasDefined(WebValveDefinition.PARAMS.getName())) {
            StringBuilder expression = new StringBuilder();
            expression.append("access-control(acl={");
            ModelNode params = newAddOp.get(WebValveDefinition.PARAMS.getName());
            boolean isValid = false;
            if (params.hasDefined("deny")) {
                isValid = true;
                String[] denied = params.get("deny").asString().split(",");
                for (String deny : denied) {
                    expression.append('\'').append(deny.trim()).append(" deny\', ");
                }
            }
            if (params.hasDefined("allow")) {
                isValid = true;
                String[] allowed = params.get("allow").asString().split(",");
                for (String allow : allowed) {
                    expression.append('\'').append(allow.trim()).append(" allow\', ");
                }
            }
            if (isValid) {
                expression.delete(expression.length() - 2, expression.length());
                expression.append("} , attribute=").append(attribute.trim()).append(')');
                createExpressionFilter(newAddOperations, name, expression.toString());
            } else {
                warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValve(name));
            }
        } else {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateValve(name));
        }
    }

    private void createExpressionFilter(Map<PathAddress, ModelNode> newAddOperations, String name, String expression) {
        newAddOperations.putIfAbsent(pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS), createAddOperation(pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS)));
        PathAddress filterAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, PATH_FILTERS, pathElement(ExpressionFilterDefinition.INSTANCE.getPathElement().getKey(), name));
        ModelNode filterAdd = createAddOperation(filterAddress);
        filterAdd.get(ExpressionFilterDefinition.EXPRESSION.getName()).set(expression);
        newAddOperations.put(filterAddress, filterAdd);
    }

    private void migrateConnector(OperationContext context, Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address, ModelNode legacyModelAddOps, List<String> warnings, boolean domainMode) throws OperationFailedException {
        String protocol = newAddOp.get(WebConnectorDefinition.PROTOCOL.getName()).asString();
        String scheme = null;
        if (newAddOp.hasDefined(WebConnectorDefinition.SCHEME.getName())) {
            scheme = newAddOp.get(WebConnectorDefinition.SCHEME.getName()).asString();
        }
        final PathAddress newAddress;
        final ModelNode addConnector;
        switch (protocol) {
            case "org.apache.coyote.http11.Http11Protocol":
            case "org.apache.coyote.http11.Http11NioProtocol":
            case "org.apache.coyote.http11.Http11AprProtocol":
            case "HTTP/1.1":
                if (scheme == null || scheme.equals("http")) {
                    newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HTTP_LISTENER, address.getLastElement().getValue()));
                    addConnector = createAddOperation(newAddress);
                } else if (scheme.equals("https")) {
                    newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HTTPS_LISTENER, address.getLastElement().getValue()));
                    addConnector = createAddOperation(newAddress);

                    SSLInformation sslInfo = createSecurityRealm(context, newAddOperations, legacyModelAddOps, newAddress.getLastElement().getValue(), warnings, domainMode);
                    if (sslInfo == null) {
                        throw WebLogger.ROOT_LOGGER.noSslConfig();
                    } else {
                        addConnector.get(Constants.SECURITY_REALM).set(sslInfo.realmName);
                        ModelNode verify = sslInfo.verifyClient;
                        if(verify.isDefined()) {
                            if(verify.getType() == ModelType.EXPRESSION) {
                                warnings.add(WebLogger.ROOT_LOGGER.couldNotTranslateVerifyClientExpression(verify.toString()));
                                addConnector.get(Constants.VERIFY_CLIENT).set(verify);
                            } else {
                                String translated = translateVerifyClient(verify.asString(), warnings);
                                if(translated != null) {
                                    addConnector.get(Constants.VERIFY_CLIENT).set(translated);
                                }
                            }
                        }

                        addConnector.get(Constants.SSL_SESSION_CACHE_SIZE).set(sslInfo.sessionCacheSize);
                        addConnector.get(Constants.SSL_SESSION_TIMEOUT).set(sslInfo.sessionTimeout);
                        addConnector.get(Constants.ENABLED_PROTOCOLS).set(sslInfo.sslProtocol);
                        addConnector.get(Constants.ENABLED_CIPHER_SUITES).set(sslInfo.cipherSuites);
                    }
                } else {
                    newAddress = null;
                    addConnector = null;
                }
                break;
            case "org.apache.coyote.ajp.AjpAprProtocol":
            case "org.apache.coyote.ajp.AjpProtocol":
            case "AJP/1.3":
                newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.AJP_LISTENER, address.getLastElement().getValue()));
                addConnector = createAddOperation(newAddress);
                addConnector.get(Constants.SCHEME).set(newAddOp.get(Constants.SCHEME));
                break;
            default:
                newAddress = null;
                addConnector = null;
        }
        if (newAddress == null) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(newAddOp));
            return;
        }
        addConnector.get(Constants.SOCKET_BINDING).set(newAddOp.get(SOCKET_BINDING));
        addConnector.get(Constants.SECURE).set(newAddOp.get(WebConnectorDefinition.SECURE.getName()));
        addConnector.get(Constants.REDIRECT_SOCKET).set(newAddOp.get(WebConnectorDefinition.REDIRECT_BINDING.getName()));
        addConnector.get(Constants.ENABLED).set(newAddOp.get(WebConnectorDefinition.ENABLED.getName()));
        addConnector.get(Constants.RESOLVE_PEER_ADDRESS).set(newAddOp.get(WebConnectorDefinition.ENABLE_LOOKUPS.getName()));
        addConnector.get(Constants.MAX_POST_SIZE).set(newAddOp.get(WebConnectorDefinition.MAX_POST_SIZE.getName()));
        addConnector.get(Constants.REDIRECT_SOCKET).set(newAddOp.get(WebConnectorDefinition.REDIRECT_BINDING.getName()));
        addConnector.get(Constants.MAX_CONNECTIONS).set(newAddOp.get(WebConnectorDefinition.MAX_CONNECTIONS.getName()));
        addConnector.get(Constants.MAX_BUFFERED_REQUEST_SIZE).set(newAddOp.get(WebConnectorDefinition.MAX_SAVE_POST_SIZE.getName()));
        addConnector.get(Constants.SECURE).set(newAddOp.get(WebConnectorDefinition.SECURE.getName()));
        if(newAddOp.hasDefined(WebConnectorDefinition.REDIRECT_PORT.getName())) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebConnectorDefinition.REDIRECT_PORT.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }
        if(newAddOp.hasDefined(WebConnectorDefinition.PROXY_BINDING.getName())) {
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebConnectorDefinition.PROXY_BINDING.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }
        if (newAddOp.hasDefined(WebConnectorDefinition.EXECUTOR.getName())) {
            //TODO: migrate executor to worker
            warnings.add(WebLogger.ROOT_LOGGER.couldNotMigrateResource(WebConnectorDefinition.EXECUTOR.getName(), pathAddress(newAddOp.get(ADDRESS))));
        }

        newAddOperations.put(pathAddress(newAddOp.get(OP_ADDR)), addConnector);
    }

    private String translateVerifyClient(String s, List<String> warnings) {
        switch(s) {
            case "optionalNoCA":
            case "optional": {
                return "REQUESTED";
            }
            case "require" : {
                return "REQUIRED";
            }
            case "none": {
                return "NOT_REQUESTED";
            }
            case "true": {
                return "REQUIRED";
            }
            case "false": {
                return "NOT_REQUESTED";
            }
            default: {
                warnings.add(WebLogger.ROOT_LOGGER.couldNotTranslateVerifyClient(s));
                return null;
            }
        }
    }

    private void migrateMimeMapping(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp) {
        migrateWelcomeFiles(newAddOperations, newAddOp);
        ModelNode mime = newAddOp.get("mime-mapping");
        if (mime.isDefined()) {
            for (ModelNode w : mime.asList()) {
                PathAddress wa = pathAddress(pathAddress(UndertowExtension.SUBSYSTEM_PATH, pathElement(Constants.SERVLET_CONTAINER, "default"), pathElement(Constants.MIME_MAPPING, w.asProperty().getName())));
                ModelNode add = createAddOperation(wa);
                add.get(Constants.VALUE).set(w.asProperty().getValue());
                newAddOperations.put(wa, add);

            }
        }
    }

    private void migrateWelcomeFiles(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp) {
        ModelNode welcome = newAddOp.get("welcome-file");
        if (welcome.isDefined()) {
            for (ModelNode w : welcome.asList()) {
                PathAddress wa = pathAddress(pathAddress(UndertowExtension.SUBSYSTEM_PATH, pathElement(Constants.SERVLET_CONTAINER, "default"), pathElement(Constants.WELCOME_FILE, w.asString())));
                ModelNode add = createAddOperation(wa);
                newAddOperations.put(wa, add);
            }
        }
    }

    private void migrateJSPConfig(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp) {
        newAddOp.get(ADDRESS).set(pathAddress(UndertowExtension.SUBSYSTEM_PATH, pathElement(Constants.SERVLET_CONTAINER, "default"), UndertowExtension.PATH_JSP).toModelNode());
        newAddOperations.put(pathAddress(newAddOp.get(OP_ADDR)), newAddOp);
    }

    private void migrateSubsystem(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp) {
        newAddOp.get(ADDRESS).set(pathAddress(pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME)).toModelNode());
        PathAddress address = pathAddress(newAddOp.get(OP_ADDR));
        ModelNode subsystemAdd = createAddOperation(address);
        ModelNode defaultServer = newAddOperations.get(address.append(DEFAULT_SERVER_PATH));
        if (defaultServer.hasDefined(Constants.DEFAULT_HOST)){
            subsystemAdd.get(Constants.DEFAULT_VIRTUAL_HOST).set(defaultServer.get(Constants.DEFAULT_HOST));
        }
        newAddOperations.put(address, subsystemAdd);
    }

    private void describeLegacyWebResources(OperationContext context, ModelNode legacyModelDescription) {
        ModelNode describeLegacySubsystem = createOperation(GenericSubsystemDescribeHandler.DEFINITION, context.getCurrentAddress());
        context.addStep(legacyModelDescription, describeLegacySubsystem, GenericSubsystemDescribeHandler.INSTANCE, MODEL, true);
    }

    private static ModelNode findResource(PathAddress address, ModelNode legacyAddOps) {
        for (ModelNode legacyAddOp : legacyAddOps.get(RESULT).asList()) {
            final PathAddress la = pathAddress(legacyAddOp.get(ADDRESS));
            if (la.equals(address)) {
                return legacyAddOp;
            }
        }
        return null;
    }

    private class SSLInformation {
        final String realmName;
        final ModelNode verifyClient;
        final ModelNode sessionCacheSize;
        final ModelNode sessionTimeout;
        final ModelNode sslProtocol;
        final ModelNode cipherSuites;

        private SSLInformation(String realmName, ModelNode verifyClient, ModelNode sessionCacheSize, ModelNode sessionTimeout, ModelNode sslProtocol, ModelNode cipherSuites) {
            this.realmName = realmName;
            this.verifyClient = verifyClient;
            this.sessionCacheSize = sessionCacheSize;
            this.sessionTimeout = sessionTimeout;
            this.sslProtocol = sslProtocol;
            this.cipherSuites = cipherSuites;
        }
    }
}
