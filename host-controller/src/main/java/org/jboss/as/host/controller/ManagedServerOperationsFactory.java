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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_SEARCH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_TO_PRINCIPAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JSON_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRINCIPAL_TO_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_IS_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_TO_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.File;
import java.util.AbstractList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.services.path.PathAddHandler;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.access.SensitivityResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.domain.management.audit.FileAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.JsonAuditLogFormatterResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogHandlerResourceDefinition;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.operations.SetServerGroupHostHandler;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.services.net.BindingGroupAddHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Factory creating the boot operations for a {@linkplain ManagedServer}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Emanuel Muckenhuber
 */
public final class ManagedServerOperationsFactory {

    private static final ModelNode EMPTY = new ModelNode();

    static {
        EMPTY.setEmptyList();
        EMPTY.protect();
    }

    /**
     * Create a list of operations required to a boot a managed server.
     *
     * @param serverName the server name
     * @param domainModel the complete domain model
     * @param hostModel the local host model
     * @param domainController the domain controller
     * @return the list of boot operations
     */
    public static ModelNode createBootUpdates(final String serverName, final ModelNode domainModel, final ModelNode hostModel,
                                              final DomainController domainController, final ExpressionResolver expressionResolver) {
        final ManagedServerOperationsFactory factory = new ManagedServerOperationsFactory(serverName, domainModel,
                hostModel, domainController, expressionResolver);


        return factory.getBootUpdates();
    }

    private final String serverName;
    private final ModelNode domainModel;
    private final ModelNode hostModel;
    private final ModelNode serverModel;
    private final ModelNode serverGroup;
    private final String serverGroupName;
    private final String profileName;
    private final DomainController domainController;
    private final ExpressionResolver expressionResolver;

    ManagedServerOperationsFactory(final String serverName, final ModelNode domainModel, final ModelNode hostModel,
                                   final DomainController domainController, final ExpressionResolver expressionResolver) {
        this.serverName = serverName;
        this.domainModel = domainModel;
        this.hostModel = hostModel;
        this.domainController = domainController;
        this.expressionResolver = expressionResolver;
        this.serverModel = resolveExpressions(hostModel.require(SERVER_CONFIG).require(serverName));

        this.serverGroupName = serverModel.require(GROUP).asString();
        this.serverGroup = resolveExpressions(domainModel.require(SERVER_GROUP).require(serverGroupName));
        this.profileName = serverGroup.require(PROFILE).asString();
    }

    /**
     * Resolve expressions in the given model (if there are any)
     */
    private ModelNode resolveExpressions(final ModelNode unresolved) {
        if (unresolved == null) {
            return null;
        }
        try {
            return expressionResolver.resolveExpressions(unresolved.clone());
        } catch (OperationFailedException e) {
            // Fail
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    ModelNode getBootUpdates() {

        int portOffSet = 0;
        String socketBindingRef = null;

        if (serverGroup.hasDefined(SOCKET_BINDING_GROUP)) {
            socketBindingRef = serverGroup.get(SOCKET_BINDING_GROUP).asString();
        }
        if (serverModel.hasDefined(SOCKET_BINDING_GROUP)) {
            socketBindingRef = serverModel.get(SOCKET_BINDING_GROUP).asString();
        }
        if (serverGroup.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            portOffSet = serverGroup.get(SOCKET_BINDING_PORT_OFFSET).asInt();
        }
        if (serverModel.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            portOffSet = serverModel.get(SOCKET_BINDING_PORT_OFFSET).asInt();
        }
        if (socketBindingRef == null) {
            throw MESSAGES.undefinedSocketBinding(serverName);
        }

        final ModelNodeList updates = new ModelNodeList();

        setServerGroupHost(updates);
        addNamespaces(updates);
        addProfileName(updates);
        addSchemaLocations(updates);
        addExtensions(updates);
        addPaths(updates);
        addSystemProperties(updates);
        addVault(updates);
        addManagementSecurityRealms(updates);
        addAuditLog(updates);
        addManagementConnections(updates);
        addManagementAuthorization(updates);
        addInterfaces(updates);
        addSocketBindings(updates, portOffSet, socketBindingRef);
        addSubsystems(updates);
        addDeployments(updates);
        addDeploymentOverlays(updates);

        return updates.model;
    }

    private void setServerGroupHost(ModelNodeList updates) {
        ModelNode op = Util.createEmptyOperation(SetServerGroupHostHandler.OPERATION_NAME, null);
        op.get(SERVER_GROUP).set(serverGroupName);
        LocalHostControllerInfo lhci = domainController.getLocalHostInfo();
        op.get(HOST).set(lhci.getLocalHostName());

        updates.add(op);
    }

    private void addNamespaces(List<ModelNode> updates) {
        final Map<String, ModelNode> map = new LinkedHashMap<String, ModelNode>();
        addNamespaces(map, domainModel.get(NAMESPACES));
        addNamespaces(map, hostModel.get(NAMESPACES));
        updates.addAll(map.values());
    }

    private void addNamespaces(Map<String, ModelNode> map, ModelNode namespaces) {
        if (namespaces.isDefined()) {
            for (Property prop : namespaces.asPropertyList()) {
                map.put(prop.getName(), NamespaceAddHandler.getAddNamespaceOperation(EMPTY, prop.getName(), prop.getValue().asString()));
            }
        }
    }

    private void addProfileName(List<ModelNode> updates) {
        updates.add(Util.getWriteAttributeOperation(EMPTY, PROFILE_NAME, profileName));
    }

    private void addSchemaLocations(List<ModelNode> updates) {
        final Map<String, ModelNode> map = new LinkedHashMap<String, ModelNode>();
        addSchemaLocations(map, domainModel.get(SCHEMA_LOCATIONS));
        addSchemaLocations(map, hostModel.get(SCHEMA_LOCATIONS));
        updates.addAll(map.values());
    }

    private void addSchemaLocations(Map<String, ModelNode> map, ModelNode namespaces) {
        if (namespaces.isDefined()) {
            for (Property prop : namespaces.asPropertyList()) {
                map.put(prop.getName(), SchemaLocationAddHandler.getAddSchemaLocationOperation(EMPTY, prop.getName(), prop.getValue().asString()));
            }
        }
    }

    private void addExtensions(List<ModelNode> updates) {
        final Set<String> extensionNames = new LinkedHashSet<String>();
        addExtensions(extensionNames, domainModel.get(EXTENSION));
        addExtensions(extensionNames, hostModel.get(EXTENSION));

        for (String name : extensionNames) {
            updates.add(Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(EXTENSION, name))));
        }
    }

    private void addExtensions(Set<String> extensionNames, ModelNode extensions) {
        if (extensions.isDefined()) {
            extensionNames.addAll(extensions.keys());
        }
    }

    private void addPaths(List<ModelNode> updates) {
        final Map<String, ModelNode> paths = new LinkedHashMap<String, ModelNode>();
        addPaths(paths, domainModel.get(PATH));
        addPaths(paths, hostModel.get(PATH));
        addPaths(paths, serverModel.get(PATH));

        for (Map.Entry<String, ModelNode> entry : paths.entrySet()) {
            updates.add(PathAddHandler.getAddPathOperation(PathAddress.pathAddress(PathElement.pathElement(PATH, entry.getKey())), entry.getValue().get(PATH), entry.getValue().get(RELATIVE_TO)));
        }
    }

    private void addPaths(Map<String, ModelNode> map, ModelNode path) {
        if (path.isDefined()) {
            for (Property prop : path.asPropertyList()) {
                //TODO merge rather than replace existing?
                map.put(prop.getName(), prop.getValue());
            }
        }
    }

    private void addSystemProperties(List<ModelNode> updates) {
        Map<String, String> props = getAllSystemProperties(false);

        for (Map.Entry<String, String> entry : props.entrySet()) {
            ModelNode address = new ModelNode();
            address.add(SYSTEM_PROPERTY, entry.getKey());
            ModelNode op = Util.getEmptyOperation(SystemPropertyAddHandler.OPERATION_NAME, address);
            if (entry.getValue() != null) {
                op.get(VALUE).set(entry.getValue());
            }
            updates.add(op);
        }
    }

    private Map<String, String> getAllSystemProperties(boolean boottimeOnly) {
        Map<String, String> props = new LinkedHashMap<String, String>();

        addSystemProperties(domainModel, props, boottimeOnly);
        addSystemProperties(serverGroup, props, boottimeOnly);
        addSystemProperties(hostModel, props, boottimeOnly);
        addSystemProperties(serverModel, props, boottimeOnly);

        return props;
    }

    private void addSystemProperties(final ModelNode source, final Map<String, String> props, boolean boottimeOnly) {
        if (source.hasDefined(SYSTEM_PROPERTY)) {
            for (Property prop : source.get(SYSTEM_PROPERTY).asPropertyList()) {
                ModelNode propResource = prop.getValue();
                try {
                    if (boottimeOnly && !SystemPropertyResourceDefinition.BOOT_TIME.resolveModelAttribute(domainController.getExpressionResolver(), propResource).asBoolean()) {
                        continue;
                    }
                } catch (OperationFailedException e) {
                    throw new IllegalStateException(e);
                }
                String val = propResource.hasDefined(VALUE) ? propResource.get(VALUE).asString() : null;
                props.put(prop.getName(), val);
            }
        }
    }

    private void addVault(List<ModelNode> updates) {
        if (hostModel.get(CORE_SERVICE).isDefined()) {
            addVault(updates, hostModel.get(CORE_SERVICE).get(VAULT));
        }
    }

    private void addVault(List<ModelNode> updates, ModelNode vaultNode) {
        if (vaultNode.isDefined()) {
            ModelNode vault = new ModelNode();
            ModelNode codeNode = vaultNode.get(Attribute.CODE.getLocalName());
            if (codeNode.isDefined()) {
                vault.get(Attribute.CODE.getLocalName()).set(codeNode.asString());
            }
            ModelNode vaultAddress = new ModelNode();
            vaultAddress.add(CORE_SERVICE, VAULT);
            addAddNameAndAddress(vault, vaultAddress);

            ModelNode optionsNode = vaultNode.get(VAULT_OPTIONS);
            if (optionsNode.isDefined()) {
                vault.get(VAULT_OPTIONS).set(optionsNode);
            }
            updates.add(vault);
        }
    }

    private void addManagementSecurityRealms(List<ModelNode> updates) {
        if (hostModel.get(CORE_SERVICE, MANAGEMENT, SECURITY_REALM).isDefined()) {
            ModelNode securityRealms = hostModel.get(CORE_SERVICE, MANAGEMENT, SECURITY_REALM);
            Set<String> keys = securityRealms.keys();
            for (String current : keys) {
                ModelNode addOp = new ModelNode();
                ModelNode realmAddress = new ModelNode();
                realmAddress.add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, current);
                addAddNameAndAddress(addOp, realmAddress);
                updates.add(addOp);

                ModelNode currentRealm = securityRealms.get(current);
                if (currentRealm.hasDefined(SERVER_IDENTITY)) {
                    addManagementComponentComponent(currentRealm, realmAddress, SERVER_IDENTITY, updates);
                }
                if (currentRealm.hasDefined(AUTHENTICATION)) {
                    addManagementComponentComponent(currentRealm, realmAddress, AUTHENTICATION, updates);
                }
                if (currentRealm.hasDefined(AUTHORIZATION)) {
                    ModelNode authorization = currentRealm.require(AUTHORIZATION);
                    if (authorization.hasDefined(PROPERTIES)) {
                        addManagementComponentComponent(currentRealm, realmAddress, AUTHORIZATION, updates);
                    } else if (authorization.hasDefined(LDAP)) {
                        ModelNode ldap = authorization.require(LDAP);
                        // Add authorization=ldap
                        ModelNode addLdap = new ModelNode();
                        ModelNode ldapAddr = realmAddress.clone().add(AUTHORIZATION, LDAP);
                        addAddNameAndAddress(addLdap, ldapAddr);
                        addLdap.get(CONNECTION).set(ldap.get(CONNECTION));
                        updates.add(addLdap);

                        // Add sub-resources
                        if (ldap.hasDefined(USERNAME_TO_DN)) {
                            ModelNode usernameToDn = ldap.require(USERNAME_TO_DN);
                            if (usernameToDn.hasDefined(USERNAME_IS_DN)) {
                                addLdapChild(usernameToDn.require(USERNAME_IS_DN), ldapAddr, USERNAME_TO_DN, USERNAME_IS_DN, updates);
                            } else if (usernameToDn.hasDefined(USERNAME_FILTER)) {
                                addLdapChild(usernameToDn.require(USERNAME_FILTER), ldapAddr, USERNAME_TO_DN, USERNAME_FILTER, updates);
                            } else if (usernameToDn.hasDefined(ADVANCED_FILTER)) {
                                addLdapChild(usernameToDn.require(ADVANCED_FILTER), ldapAddr, USERNAME_TO_DN, ADVANCED_FILTER, updates);
                            }
                        }
                        if (ldap.hasDefined(GROUP_SEARCH)) {
                            ModelNode groupSearch = ldap.require(GROUP_SEARCH);
                            if (groupSearch.hasDefined(GROUP_TO_PRINCIPAL)) {
                                addLdapChild(groupSearch.require(GROUP_TO_PRINCIPAL), ldapAddr, GROUP_SEARCH, GROUP_TO_PRINCIPAL, updates);
                            } else if (groupSearch.hasDefined(PRINCIPAL_TO_GROUP)) {
                                addLdapChild(groupSearch.require(PRINCIPAL_TO_GROUP), ldapAddr, GROUP_SEARCH, PRINCIPAL_TO_GROUP, updates);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addLdapChild(ModelNode child, ModelNode parentAddress, String key, String value, List<ModelNode> updates) {
        ModelNode add = new ModelNode();
        convertAttributesToParams(child, add);
        addAddNameAndAddress(add, parentAddress.clone().add(key, value));
        updates.add(add);
    }

    private void addManagementComponentComponent(ModelNode parent, ModelNode parentAddress, String key, List<ModelNode> updates) {
        for (String currentComponent : parent.get(key).keys()) {
            ModelNode addComponent = new ModelNode();
            // First take the properties to pass over.
            addComponent.set(parent.get(key, currentComponent));

            // Now convert it to an operation by adding a name and address.
            ModelNode identityAddress = parentAddress.clone().add(key, currentComponent);
            addAddNameAndAddress(addComponent, identityAddress);

            updates.add(addComponent);
        }
    }

    private void addAuditLog(ModelNodeList updates) {
        final ModelNode auditLogModel = hostModel.get(CORE_SERVICE, MANAGEMENT, ACCESS, AUDIT);
        if (auditLogModel.isDefined()){
            final PathAddress auditLogAddr = PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), PathElement.pathElement(ACCESS, AUDIT));
            updates.add(Util.createAddOperation(auditLogAddr));
            if (auditLogModel.get(JSON_FORMATTER).isDefined()) {
                for (Property formatterProp : auditLogModel.get(JSON_FORMATTER).asPropertyList()) {
                    final PathAddress formatterAddress = auditLogAddr.append(PathElement.pathElement(JSON_FORMATTER, formatterProp.getName()));
                    updates.add(JsonAuditLogFormatterResourceDefinition.createServerAddOperation(formatterAddress, formatterProp.getValue()));
                }
            }
            if (auditLogModel.get(FILE_HANDLER).isDefined()){
                for (Property fileProp : auditLogModel.get(FILE_HANDLER).asPropertyList()){
                    final PathAddress fileHandlerAddress = auditLogAddr.append(PathElement.pathElement(FILE_HANDLER, fileProp.getName()));
                    updates.add(FileAuditLogHandlerResourceDefinition.createServerAddOperation(fileHandlerAddress, fileProp.getValue()));
                }
            }
            if (auditLogModel.get(SYSLOG_HANDLER).isDefined()){
                for (Property syslogProp : auditLogModel.get(SYSLOG_HANDLER).asPropertyList()){
                    final PathAddress syslogHandlerAddress = auditLogAddr.append(SYSLOG_HANDLER, syslogProp.getName());
                    SyslogAuditLogHandlerResourceDefinition.createServerAddOperations(updates, syslogHandlerAddress, syslogProp.getValue());
                }
            }
            if (auditLogModel.get(SERVER_LOGGER, AUDIT_LOG).isDefined()){
                //server-logger=audit-log becomes logger=audit-log on the server
                final PathAddress loggerAddress = auditLogAddr.append(LOGGER, AUDIT_LOG);
                AuditLogLoggerResourceDefinition.createServerAddOperations(updates, loggerAddress, auditLogModel.get(SERVER_LOGGER, AUDIT_LOG));
            }
        }
    }

    private void addManagementConnections(List<ModelNode> updates) {
        if (hostModel.get(CORE_SERVICE, MANAGEMENT, LDAP_CONNECTION).isDefined()) {
            ModelNode baseAddress = new ModelNode();
            baseAddress.add(CORE_SERVICE, MANAGEMENT);

            ModelNode connections = hostModel.get(CORE_SERVICE, MANAGEMENT, LDAP_CONNECTION);
            for (String connectionName : connections.keys()) {
                ModelNode addConnection = new ModelNode();
                // First take the properties to pass over.
                addConnection.set(connections.get(connectionName));

                // Now convert it to an operation by adding a name and address.
                ModelNode identityAddress = baseAddress.clone().add(LDAP_CONNECTION, connectionName);
                addAddNameAndAddress(addConnection, identityAddress);

                updates.add(addConnection);
            }
        }
    }

    private void addManagementAuthorization(ModelNodeList updates) {
        ModelNode domainConfig = domainModel.get(CORE_SERVICE, MANAGEMENT, ACCESS, AUTHORIZATION);
        if (domainConfig.isDefined()) {
            ModelNode baseAddress = new ModelNode();
            baseAddress.add(CORE_SERVICE, MANAGEMENT);
            baseAddress.add(ACCESS, AUTHORIZATION);

            if (domainConfig.hasDefined(PROVIDER)) {
                ModelNode providerOp = Util.getWriteAttributeOperation(baseAddress, PROVIDER, domainConfig.get(PROVIDER));
                updates.add(providerOp);
            }

            addRoleMappings(domainConfig, baseAddress, updates);
            convertSimpleResources(domainConfig, SERVER_GROUP_SCOPED_ROLE, baseAddress, updates);
            convertSimpleResources(domainConfig, HOST_SCOPED_ROLE, baseAddress, updates);
            if (domainConfig.hasDefined(CONSTRAINT)) {
                ModelNode constraints = domainConfig.get(CONSTRAINT);
                if (constraints.hasDefined(APPLICATION_CLASSIFICATION)) {
                    convertSimpleResources(constraints.get(APPLICATION_CLASSIFICATION), APPLICATION_CLASSIFICATION, baseAddress, updates);
                }
                if (constraints.hasDefined(SENSITIVITY_CLASSIFICATION)) {
                    convertSimpleResources(constraints.get(SENSITIVITY_CLASSIFICATION), SENSITIVITY_CLASSIFICATION,
                            baseAddress, updates);
                }
                if (constraints.hasDefined(VAULT_EXPRESSION)) {
                    ModelNode address = baseAddress.clone().add(CONSTRAINT, VAULT_EXPRESSION);
                    ModelNode ve = constraints.get(VAULT_EXPRESSION);
                    // No add for this one; need to write attributes
                    for (AttributeDefinition ad : SensitivityResourceDefinition.getWritableVaultAttributeDefinitions()) {
                        String attr = ad.getName();
                        if (ve.hasDefined(attr)) {
                            updates.add(Util.getWriteAttributeOperation(address, attr, ve.get(attr)));
                        }
                    }
                }
            }
        }
    }

    private void addRoleMappings(ModelNode accessControl, ModelNode baseAddress, ModelNodeList updates) {
        if (accessControl.hasDefined(ROLE_MAPPING)) {
            for (Property roleProp : accessControl.get(ROLE_MAPPING).asPropertyList()) {
                ModelNode roleAddress = baseAddress.clone().add(ROLE_MAPPING, roleProp.getName());
                updates.add(Util.getEmptyOperation(ADD, roleAddress));

                ModelNode roleMapping = roleProp.getValue();
                convertSimpleResources(roleMapping, INCLUDE, roleAddress, updates);
                convertSimpleResources(roleMapping, EXCLUDE, roleAddress, updates);
            }
        }
    }

    private void addInterfaces(List<ModelNode> updates) {
        final Map<String, ModelNode> interfaces = new LinkedHashMap<String, ModelNode>();
        addInterfaces(interfaces, domainModel.get(INTERFACE));
        addInterfaces(interfaces, hostModel.get(INTERFACE));
        addInterfaces(interfaces, hostModel.get(SERVER_CONFIG, serverName, INTERFACE));

        for (Map.Entry<String, ModelNode> entry : interfaces.entrySet()) {
            updates.add(getAddInterfaceOperation(PathAddress.pathAddress(PathElement.pathElement(INTERFACE, entry.getKey())), entry.getValue()));
        }
    }
    private static ModelNode getAddInterfaceOperation(PathAddress address, ModelNode criteria) {
           ModelNode op = Util.createAddOperation(address);
           for (final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
               if(criteria.hasDefined(def.getName())) {
                   op.get(def.getName()).set(criteria.get(def.getName()));
               }
           }
           return op;
       }

    private void addInterfaces(Map<String, ModelNode> map, ModelNode iface) {
        if (iface.isDefined()) {
            for (Property prop : iface.asPropertyList()) {
                map.put(prop.getName(), prop.getValue());
            }
        }
    }

    private void addSocketBindings(List<ModelNode> updates, int portOffSet, String bindingRef) {
        final Set<String> processed = new HashSet<String>();
        final Map<String, ModelNode> groups = new LinkedHashMap<String, ModelNode>();
        if (domainModel.hasDefined(SOCKET_BINDING_GROUP)) {
            for (Property prop : domainModel.get(SOCKET_BINDING_GROUP).asPropertyList()) {
                ModelNode node = prop.getValue().clone();
                if (portOffSet > 0) {
                    node.get(PORT_OFFSET).set(portOffSet);
                }
                groups.put(prop.getName(), node);
            }
        }
        final ModelNode group = groups.get(bindingRef);
        if (group == null) {
            throw MESSAGES.undefinedSocketBindingGroup(bindingRef);
        }
        final PathAddress groupAddress = PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, bindingRef));
        final ModelNode groupAdd = BindingGroupAddHandler.getOperation(groupAddress, group);
        groupAdd.get(PORT_OFFSET).set(portOffSet);
        updates.add(groupAdd);
        mergeBindingGroups(updates, groups, bindingRef, group, processed);
    }

    private void mergeBindingGroups(List<ModelNode> updates, Map<String, ModelNode> groups, final String groupName, ModelNode group, Set<String> processed) {
        addSocketBindings(updates, group, groupName, group.get(DEFAULT_INTERFACE));
        if (group.has(INCLUDES) && group.get(INCLUDES).isDefined()) {
            for (final ModelNode include : group.get(INCLUDES).asList()) {
                final String ref = include.asString();
                if (processed.add(ref)) {
                    final ModelNode includedGroup = groups.get(ref);
                    if (includedGroup == null) {
                        throw MESSAGES.undefinedSocketBindingGroup(ref);
                    }
                    mergeBindingGroups(updates, groups, groupName, includedGroup, processed);
                }
            }
        }
    }

    private void addSocketBindings(List<ModelNode> updates, ModelNode group, final String groupName, ModelNode defaultInterface) {
        if (group.hasDefined(SOCKET_BINDING)) {
            for (final Property socketBinding : group.get(SOCKET_BINDING).asPropertyList()) {
                final String name = socketBinding.getName();
                final ModelNode binding = socketBinding.getValue();
                if (!binding.isDefined()) {
                    continue;
                }
                if (!binding.get(DEFAULT_INTERFACE).isDefined()) {
                    binding.get(DEFAULT_INTERFACE).set(defaultInterface);
                }
                updates.add(SocketBindingAddHandler.getOperation(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, groupName),
                        PathElement.pathElement(SOCKET_BINDING, name)), binding));
            }
        }
        // outbound-socket-binding (for local destination)
        if (group.hasDefined(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING)) {
            for (final Property localDestinationOutboundSocketBindings : group.get(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING).asPropertyList()) {
                final String outboundSocketBindingName = localDestinationOutboundSocketBindings.getName();
                final ModelNode binding = localDestinationOutboundSocketBindings.getValue();
                if (!binding.isDefined()) {
                    continue;
                }
                // add the local destination outbound socket binding add operation
                updates.add(getLocalSocketBindingAddOperation(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, groupName),
                        PathElement.pathElement(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketBindingName)), binding));
            }
        }
        // outbound-socket-binding (for remote destination)
        if (group.hasDefined(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING)) {
            for (final Property remoteDestinationOutboundSocketBindings : group.get(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING).asPropertyList()) {
                final String outboundSocketBindingName = remoteDestinationOutboundSocketBindings.getName();
                final ModelNode binding = remoteDestinationOutboundSocketBindings.getValue();
                if (!binding.isDefined()) {
                    continue;
                }
                // add the local destination outbound socket binding add operation
                updates.add(getRemoteSocketBindingAddOperation(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, groupName),
                        PathElement.pathElement(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketBindingName)), binding));
            }
        }
    }

    public static ModelNode getLocalSocketBindingAddOperation(final PathAddress address, final ModelNode localDestinationOutboundSocketBinding) {
            final ModelNode addOperation = Util.createAddOperation(address);
            // socket binding reference
            for (SimpleAttributeDefinition ad : LocalDestinationOutboundSocketBindingResourceDefinition.ATTRIBUTES) {
                if (localDestinationOutboundSocketBinding.get(ad.getName()).isDefined()) {
                    addOperation.get(ad.getName()).set(localDestinationOutboundSocketBinding.get(ad.getName()));
                }
            }
            return addOperation;
        }

        public static ModelNode getRemoteSocketBindingAddOperation(final PathAddress address, final ModelNode remoteDestinationOutboundSocketBinding) {
            final ModelNode addOperation = Util.createAddOperation(address);
            for (SimpleAttributeDefinition ad : RemoteDestinationOutboundSocketBindingResourceDefinition.ATTRIBUTES) {
                if (remoteDestinationOutboundSocketBinding.get(ad.getName()).isDefined()) {
                    addOperation.get(ad.getName()).set(remoteDestinationOutboundSocketBinding.get(ad.getName()));
                }
            }
            return addOperation;
        }

    private void addSubsystems(List<ModelNode> updates) {
        ModelNode node = domainController.getProfileOperations(profileName);
        updates.addAll(node.asList());
    }

    private void addDeployments(List<ModelNode> updates) {
        if (serverGroup.hasDefined(DEPLOYMENT)) {

            HostFileRepository remoteRepository = null;
            if (!domainController.getLocalHostInfo().isMasterDomainController()) {
                remoteRepository = domainController.getRemoteFileRepository();
            }

            for (Property deployment : serverGroup.get(DEPLOYMENT).asPropertyList()) {
                String name = deployment.getName();
                ModelNode details = deployment.getValue();

                ModelNode domainDeployment = domainModel.require(DEPLOYMENT).require(name);
                ModelNode deploymentContent = domainDeployment.require(CONTENT).clone();

                if (remoteRepository != null) {
                    // Make sure we have a copy of the deployment in the local repo
                    for (ModelNode content : deploymentContent.asList()) {
                        if ((content.hasDefined(HASH))) {
                            byte[] hash = content.require(HASH).asBytes();
                            File[] files = domainController.getLocalFileRepository().getDeploymentFiles(hash);
                            if (files == null || files.length == 0) {
                                remoteRepository.getDeploymentFiles(hash);
                            }
                        }
                    }
                }

                PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, name));
                ModelNode addOp = Util.getEmptyOperation(ADD, addr.toModelNode());
                addOp.get(RUNTIME_NAME).set(details.get(RUNTIME_NAME));
                addOp.get(CONTENT).set(deploymentContent);
                if (!details.hasDefined(ENABLED)) {
                    addOp.get(ENABLED).set(true);  // TODO this seems wrong
                } else {
                    addOp.get(ENABLED).set(details.get(ENABLED));
                }

                updates.add(addOp);
            }
        }
    }

    public void addDeploymentOverlays(final List<ModelNode> updates) {
        if (domainModel.hasDefined(DEPLOYMENT_OVERLAY)) {

            HostFileRepository remoteRepository = null;
            if (!domainController.getLocalHostInfo().isMasterDomainController()) {
                remoteRepository = domainController.getRemoteFileRepository();
            }

            for (Property deploymentOverlay : domainModel.get(DEPLOYMENT_OVERLAY).asPropertyList()) {
                String name = deploymentOverlay.getName();
                ModelNode details = deploymentOverlay.getValue();


                PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, name));
                ModelNode addOp = Util.getEmptyOperation(ADD, addr.toModelNode());
                updates.add(addOp);

                if (details.hasDefined(CONTENT)) {

                    for (Property content : details.get(CONTENT).asPropertyList()) {
                        final String contentName = content.getName();
                        final ModelNode contentDetails = content.getValue();
                        byte[] hash = contentDetails.require(CONTENT).asBytes();
                        File[] files = domainController.getLocalFileRepository().getDeploymentFiles(hash);
                        if (files == null || files.length == 0) {
                            if (remoteRepository != null) {
                                remoteRepository.getDeploymentFiles(hash);
                            }
                        }
                        addr = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, name), PathElement.pathElement(CONTENT, contentName));
                        addOp = Util.getEmptyOperation(ADD, addr.toModelNode());
                        addOp.get(CONTENT).get(HASH).set(contentDetails.get(CONTENT));
                        updates.add(addOp);
                    }
                }
                if (serverGroup.hasDefined(DEPLOYMENT_OVERLAY)) {
                    final ModelNode groupOverlay = serverGroup.get(DEPLOYMENT_OVERLAY).asObject();
                    if (groupOverlay.has(name)) {
                        ModelNode deploymentsNode = groupOverlay.get(name);
                        if(deploymentsNode.has(DEPLOYMENT)) {
                        for (Property content : deploymentsNode.get(DEPLOYMENT).asPropertyList()) {
                            final String deploymentName = content.getName();
                            //final ModelNode deploymentDetails = content.getValue();
                            addr = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, name), PathElement.pathElement(DEPLOYMENT, deploymentName));
                            addOp = Util.getEmptyOperation(ADD, addr.toModelNode());
                            updates.add(addOp);
                        }
                        }
                    }
                }

            }
        }
    }

    private ModelNode addAddNameAndAddress(ModelNode op, PathAddress address){
        return addAddNameAndAddress(op, address.toModelNode());
    }

    private ModelNode addAddNameAndAddress(ModelNode op, ModelNode address){
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(address);
        return op;
    }

    private static void convertSimpleResources(ModelNode model, String type, ModelNode baseAddress, ModelNodeList updates) {
        if (model.hasDefined(type)) {
            for (Property prop : model.get(type).asPropertyList()) {
                ModelNode address = baseAddress.clone().add(type, prop.getName());
                ModelNode addOp = Util.getEmptyOperation(ADD, address);
                convertAttributesToParams(prop.getValue(), addOp);
                updates.add(addOp);
            }
        }
    }

    private static void convertAttributesToParams(ModelNode value, ModelNode addOp) {
        if (value.isDefined()) {
            for (Property prop : value.asPropertyList()) {
                ModelNode attrVal = prop.getValue();
                if (attrVal.isDefined()) {
                    addOp.get(prop.getName()).set(attrVal);
                }
            }
        }
    }

    private class ModelNodeList extends AbstractList<ModelNode> implements List<ModelNode> {


        private final ModelNode model = new ModelNode().setEmptyList();

        @Override
        public boolean add(ModelNode modelNode) {
            model.add(modelNode);
            return true;
        }

        @Override
        public ModelNode get(int index) {
            return model.get(index);
        }

        @Override
        public int size() {
            return model.asInt();
        }
    }

}
