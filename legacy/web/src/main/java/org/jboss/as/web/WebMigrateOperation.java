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

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.security.KeystoreAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.wildfly.extension.io.IOExtension;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;

/**
 * Operation to migrate from the legacy web subsystem to the new undertow subsystem.
 * <p/>
 * This operation must be performed when the server is in admin-only mode.
 * Internally, the operation:
 * <p/>
 * <ul>
 * <li>query the description of all the web subsystem by invoking the :describe operation.
 * This returns a list of :add operations for each web resources.</li>
 * <li>:add the new org.widlfy.extension.undertow extension</li>
 * <li>for each web resources, transform the :add operations to add the
 * corresponding resource to the new undertow subsystem.
 * In this step, changes to the resources model are taken into account</li>
 * <li>:remove the messaging subsystem</li>
 * </ul>
 * <p/>
 * The companion <code>:describe-migration</code> operation will return a list of all the actual operations that would be
 * performed during the invocation of the <code>:migrate</code> operation.
 * <p/>
 * Note that all new operation addresses are generated for standalone mode. If this is a domain mode server
 * then the addresses are fixed after they have been generated
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
    public static final PathElement DEFAULT_SERVER_PATH = pathElement(Constants.SERVER, "default");

    private final boolean describe;

    private WebMigrateOperation(boolean describe) {

        this.describe = describe;
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder("migrate", resourceDescriptionResolver)
                        .setRuntimeOnly()
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .build(),
                WebMigrateOperation.MIGRATE_INSTANCE);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder("describe-migration", resourceDescriptionResolver)
                        .setReplyType(ModelType.LIST).setReplyValueType(ModelType.OBJECT)
                        .setRuntimeOnly()
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .build(),
                WebMigrateOperation.DESCRIBE_MIGRATION_INSTANCE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw UndertowLogger.ROOT_LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        // node containing the description (list of add operations) of the legacy subsystem
        final ModelNode legacyModelAddOps = new ModelNode();
        //we don't preserve order, instead we sort by address length
        //TODO: is this ok in every case?
        final Map<PathAddress, ModelNode> migrationOperations = new TreeMap<>(new Comparator<PathAddress>() {
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
        addExtension(context, migrationOperations, describe, UNDERTOW_EXTENSION);
        addExtension(context, migrationOperations, describe, IO_EXTENSION);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                addDefaultResources(migrationOperations, legacyModelAddOps);

                //create the new IO subsystem
                createIoSubsystem(context, migrationOperations);

                createWelcomeContentHandler(migrationOperations);

                // transform the legacy add operations and put them in migrationOperations
                transformResources(context, legacyModelAddOps, migrationOperations);

                // put the /subsystem=messaging:remove operation
                removeWebSubsystem(migrationOperations);

                fixAddressesForDomainMode(pathAddress(operation.get(ADDRESS)), migrationOperations);

                if (describe) {
                    // :describe-migration operation

                    // for describe-migration operation, do nothing and return the list of operations that would
                    // be executed in the composite operation
                    context.getResult().set(migrationOperations.values());
                } else {
                    // :migrate operation
                    // invoke an OSH on a composite operation with all the migration operations
                    migrateSubsystems(context, migrationOperations);
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
    private SSLInformation createSecurityRealm(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, ModelNode legacyModelAddOps, String connector) {
        ModelNode legacyAddOp = findResource(pathAddress(WebExtension.SUBSYSTEM_PATH, pathElement(WebExtension.CONNECTOR_PATH.getKey(), connector), pathElement("configuration", "ssl")), legacyModelAddOps);
        if (legacyAddOp == null) {
            return null;
        }
        //we have SSL
        //now we need to find a unique name
        //in domain mode different profiles could have different SSL configurations
        //but the realms are not scoped to a profile
        //if we hard coded a name migration would fail when migrating domains with multiple profiles
        int counter = 1;
        String realmName = REALM_NAME + counter;
        boolean ok = false;
        do {
            Resource root = context.readResourceFromRoot(pathAddress(CORE_SERVICE, MANAGEMENT), false);
            if (root.getChildrenNames(SECURITY_REALM).contains(realmName)) {
                counter++;
                realmName = REALM_NAME + counter;
            } else {
                ok = true;
            }
        } while (!ok);

        //we have a unique realm name
        //add the realm
        PathAddress addres = pathAddress(pathElement(CORE_SERVICE, MANAGEMENT), pathElement(SECURITY_REALM, realmName));
        migrationOperations.put(addres, createAddOperation(addres));

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
        ModelNode sslProtocol = legacyAddOp.get(WebSSLDefinition.SSL_PROTOCOL.getName());

        //now lets add the trust store
        addres = pathAddress(pathElement(CORE_SERVICE, MANAGEMENT), pathElement(SECURITY_REALM, realmName), pathElement(AUTHENTICATION, TRUSTSTORE));
        ModelNode addOp = createAddOperation(addres);
        addOp.get(KeystoreAttributes.KEYSTORE_PATH.getName()).set(caCertificateFile);
        addOp.get(KeystoreAttributes.KEYSTORE_PASSWORD.getName()).set(caCertificatePassword);
        addOp.get(KeystoreAttributes.KEYSTORE_PROVIDER.getName()).set(trustStoreType);
        migrationOperations.put(addres, addOp);


        //now lets add the key store
        addres = pathAddress(pathElement(CORE_SERVICE, MANAGEMENT), pathElement(SECURITY_REALM, realmName), pathElement(SERVER_IDENTITY, SSL));
        addOp = createAddOperation(addres);
        addOp.get(KeystoreAttributes.KEYSTORE_PATH.getName()).set(certificateKeyFile);
        addOp.get(KeystoreAttributes.KEYSTORE_PASSWORD.getName()).set(password);
        addOp.get(KeystoreAttributes.KEYSTORE_PROVIDER.getName()).set(keystoreType);
        addOp.get(KeystoreAttributes.ALIAS.getName()).set(keyAlias);
        addOp.get(PROTOCOL).set(protocol);
        //addOp.get(KeystoreAttributes.KEY_PASSWORD.getName()).set(password); //TODO: is this correct? both key and keystore have same password?

        if(verifyDepth.isDefined()) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.VERIFY_DEPTH.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
        }
        if(certificateFile.isDefined()) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.CERTIFICATE_FILE.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
        }

        if(csRevocationURL.isDefined()) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSLDefinition.CA_REVOCATION_URL.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
        }

        migrationOperations.put(addres, addOp);

        return new SSLInformation(realmName, verifyClient, sessionCacheSize, sessionTimeout, sslProtocol, cipherSuite);
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
    private void createIoSubsystem(OperationContext context, Map<PathAddress, ModelNode> migrationOperations) {
        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        if (root.getChildrenNames(SUBSYSTEM).contains(IOExtension.SUBSYSTEM_NAME)) {
            // subsystem is already added, do nothing
            return;
        }

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

    private void addDefaultResources(Map<PathAddress, ModelNode> migrationOperations, final ModelNode legacyModelDescription) {
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
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.SENDFILE.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    sendfile = node;
                }
                node = legacyAddOp.get(WebStaticResources.FILE_ENCODING.getName());
                if (node.isDefined()) {
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.FILE_ENCODING.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    fileEncoding = node;
                }
                node = legacyAddOp.get(WebStaticResources.READ_ONLY.getName());
                if (node.isDefined()) {
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.READ_ONLY.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    readOnly = node;
                }
                node = legacyAddOp.get(WebStaticResources.WEBDAV.getName());
                if (node.isDefined()) {
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.WEBDAV.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    webdav = node;
                }
                node = legacyAddOp.get(WebStaticResources.SECRET.getName());
                if (node.isDefined()) {
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.SECRET.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    secret = node;
                }
                node = legacyAddOp.get(WebStaticResources.MAX_DEPTH.getName());
                if (node.isDefined()) {
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.MAX_DEPTH.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    maxDepth = node;
                }
                node = legacyAddOp.get(WebStaticResources.DISABLED.getName());
                if (node.isDefined()) {
                    UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebStaticResources.DISABLED.getName(), pathAddress(legacyAddOp.get(ADDRESS)));
                    disabled = node;
                }
            }
        }

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

    private void removeWebSubsystem(Map<PathAddress, ModelNode> migrationOperations) {
        PathAddress subsystemAddress = pathAddress(WebExtension.SUBSYSTEM_PATH);
        ModelNode removeOperation = createRemoveOperation(subsystemAddress);
        migrationOperations.put(subsystemAddress, removeOperation);
    }

    private void migrateSubsystems(OperationContext context, final Map<PathAddress, ModelNode> migrationOperations) {
        ModelNode compositeOp = createOperation(COMPOSITE, EMPTY_ADDRESS);
        compositeOp.get(STEPS).set(migrationOperations.values());
        context.addStep(compositeOp, CompositeOperationHandler.INSTANCE, MODEL);
    }

    private void transformResources(final OperationContext context, final ModelNode legacyModelDescription, final Map<PathAddress, ModelNode> newAddOperations) throws OperationFailedException {
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
                migrateConnector(context, newAddOperations, newAddOp, address, legacyModelDescription);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH))) {
                migrateVirtualHost(newAddOperations, newAddOp, address);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH, WebExtension.ACCESS_LOG_PATH))) {
                migrateAccessLog(newAddOperations, newAddOp, address, legacyModelDescription);
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH, WebExtension.ACCESS_LOG_PATH, WebExtension.DIRECTORY_PATH))) {
                //ignore, handled by access-log
            } else if (wildcardEquals(address, pathAddress(WebExtension.SUBSYSTEM_PATH, WebExtension.HOST_PATH, WebExtension.SSO_PATH))) {
                migrateSso(newAddOperations, newAddOp, address);
            } else {
                UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(legacyAddOp);
            }

        }
    }

    private void migrateSso(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address) {
        PathAddress newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, address.getElement(address.size() - 2).getValue()), UndertowExtension.PATH_SSO);
        ModelNode add = createAddOperation(newAddress);

        add.get(Constants.DOMAIN).set(newAddOp.get(WebSSODefinition.DOMAIN.getName()).clone());
        add.get(Constants.HTTP_ONLY).set(newAddOp.get(WebSSODefinition.HTTP_ONLY.getName()).clone());

        if (newAddOp.hasDefined(WebSSODefinition.CACHE_CONTAINER.getName())) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSODefinition.CACHE_CONTAINER.getName(), pathAddress(newAddOp.get(ADDRESS)));
        }
        if (newAddOp.hasDefined(WebSSODefinition.REAUTHENTICATE.getName())) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSODefinition.REAUTHENTICATE.getName(), pathAddress(newAddOp.get(ADDRESS)));
        }
        if (newAddOp.hasDefined(WebSSODefinition.CACHE_NAME.getName())) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebSSODefinition.CACHE_NAME.getName(), pathAddress(newAddOp.get(ADDRESS)));
        }

        newAddOperations.put(newAddress, add);
    }

    private void migrateAccessLog(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address, ModelNode legacyAddOps) {
        PathAddress newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, address.getElement(address.size() - 2).getValue()), UndertowExtension.PATH_ACCESS_LOG);
        ModelNode add = createAddOperation(newAddress);

        //TODO: parse the pattern and modify to Undertow version
        add.get(Constants.PATTERN).set(newAddOp.get(WebAccessLogDefinition.PATTERN.getName()).clone());
        add.get(Constants.PREFIX).set(newAddOp.get(WebAccessLogDefinition.PREFIX.getName()).clone());
        add.get(Constants.ROTATE).set(newAddOp.get(WebAccessLogDefinition.ROTATE.getName()).clone());
        if (newAddOp.hasDefined(WebAccessLogDefinition.RESOLVE_HOSTS.getName())) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebAccessLogDefinition.RESOLVE_HOSTS.getName(), pathAddress(newAddOp.get(ADDRESS)));
        }
        //TODO: extended access log
        if (newAddOp.hasDefined(WebAccessLogDefinition.EXTENDED.getName())) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebAccessLogDefinition.EXTENDED.getName(), pathAddress(newAddOp.get(ADDRESS)));
        }

        ModelNode directory = findResource(pathAddress(pathAddress(newAddOp.get(ADDRESS)), WebExtension.DIRECTORY_PATH), legacyAddOps);
        if(directory != null){
            newAddOp.get(Constants.DIRECTORY).set(directory.get(PATH));
            newAddOp.get(Constants.RELATIVE_TO).set(directory.get(RELATIVE_TO));
        }

        newAddOperations.put(newAddress, add);
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

    private void migrateVirtualHost(Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address) {
        PathAddress newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HOST, address.getLastElement().getValue()));
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

    private void migrateConnector(OperationContext context, Map<PathAddress, ModelNode> newAddOperations, ModelNode newAddOp, PathAddress address, ModelNode legacyModelAddOps) throws OperationFailedException {
        String protocol = newAddOp.get(WebConnectorDefinition.PROTOCOL.getName()).asString();
        String scheme = null;
        if (newAddOp.hasDefined(WebConnectorDefinition.SCHEME.getName())) {
            scheme = newAddOp.get(WebConnectorDefinition.SCHEME.getName()).asString();
        }
        final PathAddress newAddress;
        final ModelNode addConnector;
        switch (protocol) {
            case "HTTP/1.1":
                if (scheme == null || scheme.equals("http")) {
                    newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HTTP_LISTENER, address.getLastElement().getValue()));
                    addConnector = createAddOperation(newAddress);
                } else if (scheme.equals("https")) {
                    newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.HTTPS_LISTENER, address.getLastElement().getValue()));
                    addConnector = createAddOperation(newAddress);

                    SSLInformation sslInfo = createSecurityRealm(context, newAddOperations, legacyModelAddOps, newAddress.getLastElement().getValue());
                    if (sslInfo == null) {
                        throw UndertowLogger.ROOT_LOGGER.noSslConfig();
                    } else {
                        addConnector.get(Constants.SECURITY_REALM).set(sslInfo.realmName);
                        addConnector.get(Constants.VERIFY_CLIENT).set(sslInfo.verifyClient);
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
            case "AJP":
                newAddress = pathAddress(UndertowExtension.SUBSYSTEM_PATH, DEFAULT_SERVER_PATH, pathElement(Constants.AJP_LISTENER, address.getLastElement().getValue()));
                addConnector = createAddOperation(newAddress);
                break;
            default:
                newAddress = null;
                addConnector = null;
        }
        if (newAddress == null) {
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(newAddOp);
            return;
        }
        addConnector.get(Constants.SOCKET_BINDING).set(newAddOp.get(SOCKET_BINDING));
        addConnector.get(Constants.SECURE).set(newAddOp.get(WebConnectorDefinition.SECURE.getName()));
        addConnector.get(Constants.REDIRECT_SOCKET).set(newAddOp.get(WebConnectorDefinition.REDIRECT_BINDING.getName()));
        addConnector.get(Constants.ENABLED).set(newAddOp.get(WebConnectorDefinition.ENABLED.getName()));
        addConnector.get(Constants.RESOLVE_PEER_ADDRESS).set(newAddOp.get(WebConnectorDefinition.ENABLE_LOOKUPS.getName()));
        addConnector.get(Constants.MAX_POST_SIZE).set(newAddOp.get(WebConnectorDefinition.MAX_POST_SIZE.getName()));
        addConnector.get(Constants.REDIRECT_SOCKET).set(newAddOp.get(WebConnectorDefinition.REDIRECT_BINDING.getName()));
        //TODO: secure
        //TODO: max save post size
        //TODO: proxy binding
        if (newAddOp.hasDefined(WebConnectorDefinition.EXECUTOR.getName())) {
            //TODO: migrate executor to worker
            UndertowLogger.ROOT_LOGGER.couldNotMigrateResource(WebConnectorDefinition.EXECUTOR.getName(), pathAddress(newAddOp.get(ADDRESS)));
        }
        //TODO: expose max connections
        newAddOperations.put(pathAddress(newAddOp.get(OP_ADDR)), addConnector);
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
        newAddOperations.put(pathAddress(newAddOp.get(OP_ADDR)), newAddOp);
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
