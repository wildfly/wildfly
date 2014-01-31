/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_SEARCH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_TO_DN;
import static org.jboss.as.domain.management.ModelDescriptionConstants.CONTAINS;
import static org.jboss.as.domain.management.ModelDescriptionConstants.FLUSH_CACHE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.as.domain.management.security.LdapSearcherCache.Predicate;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * {@link ResourceDefinition} for a LDAP caching.
 *
 *  @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapCacheResourceDefinition extends SimpleResourceDefinition {

    private static final CacheDefintionValidatingHandler VALIDATION_INSTANCE = new CacheDefintionValidatingHandler();

    /*
     * Configuration Attributes
     */

    public static final SimpleAttributeDefinition EVICTION_TIME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.EVICTION_TIME, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(900))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CACHE_FAILURES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CACHE_FAILURES, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition MAX_CACHE_SIZE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MAX_CACHE_SIZE, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(0))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    /*
     * Runtime Attributes
     */

    // Current Size - int
    public static final SimpleAttributeDefinition CACHE_SIZE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CACHE_SIZE, ModelType.INT)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    // (Other options are, max size, min size, max age, min age, average age.

    /*
     * Runtime Operations
     */

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinition(ModelDescriptionConstants.NAME, ModelType.STRING, true);
    public static final SimpleAttributeDefinition NAME_REQUIRED = new SimpleAttributeDefinition(ModelDescriptionConstants.NAME, ModelType.STRING, false);

    public static final SimpleAttributeDefinition DISTINGUISHED_NAME = new SimpleAttributeDefinition(ModelDescriptionConstants.DISTINGUISHED_NAME, ModelType.STRING, true);

    public static final SimpleOperationDefinition FLUSH_CACHE_NAME_ONLY = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.FLUSH_CACHE,
            ControllerResolver.getResolver("core.management.security-realm.ldap.cache"))
            .setEntryType(OperationEntry.EntryType.PUBLIC)
            .addParameter(NAME)
            .setRuntimeOnly()
            .build();

    public static final SimpleOperationDefinition FLUSH_CACHE_FULL = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.FLUSH_CACHE,
            ControllerResolver.getResolver("core.management.security-realm.ldap.cache"))
            .setEntryType(OperationEntry.EntryType.PUBLIC)
            .addParameter(NAME)
            .addParameter(DISTINGUISHED_NAME)
            .setRuntimeOnly()
            .build();

    public static final SimpleOperationDefinition CONTAINS_NAME_ONLY = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.CONTAINS,
            ControllerResolver.getResolver("core.management.security-realm.ldap.cache"))
            .setEntryType(OperationEntry.EntryType.PUBLIC)
            .addParameter(NAME_REQUIRED)
            .setRuntimeOnly()
            .setReplyValueType(ModelType.BOOLEAN)
            .build();

    public static final SimpleOperationDefinition CONTAINS_FULL = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.CONTAINS,
            ControllerResolver.getResolver("core.management.security-realm.ldap.cache"))
            .setEntryType(OperationEntry.EntryType.PUBLIC)
            .addParameter(NAME)
            .addParameter(DISTINGUISHED_NAME)
            .setRuntimeOnly()
            .setReplyValueType(ModelType.BOOLEAN)
            .build();

    private static final OperationStepHandler NAME_ONLY_HANDLER = new NameOnlyOpHandler();
    private static final OperationStepHandler FULL_HANDLER = new FullOpHandler();

    private final SimpleAttributeDefinition[] configurationAttributes;
    private final SimpleAttributeDefinition[] runtimeAttributes;
    private final SimpleOperationDefinition[] runtimeOperations;
    private final OperationStepHandler runtimeStepHandler;

    private LdapCacheResourceDefinition(final PathElement pathElement,
            final SimpleAttributeDefinition[] configurationAttributes, final SimpleAttributeDefinition[] runtimeAttributes,
            final SimpleOperationDefinition[] runtimeOperations, final OperationStepHandler runtimeStepHandler) {
        super(pathElement, ControllerResolver.getResolver("core.management.security-realm.ldap.cache"),
                new CacheChildAddHandler(configurationAttributes), new SecurityRealmChildRemoveHandler(
                        false), OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

        this.configurationAttributes = configurationAttributes;
        this.runtimeAttributes = runtimeAttributes;
        this.runtimeOperations = runtimeOperations;
        this.runtimeStepHandler = runtimeStepHandler;
    }

    private static ResourceDefinition create(final PathElement pathElement, final CacheFor cacheFor) {
        SimpleAttributeDefinition[] configurationAttributes = new SimpleAttributeDefinition[] { EVICTION_TIME, CACHE_FAILURES, MAX_CACHE_SIZE };
        SimpleAttributeDefinition[] runtimeAttributes = new SimpleAttributeDefinition[] { CACHE_SIZE };
        final SimpleOperationDefinition[] runtimeOperations;
        final OperationStepHandler runtimeHandler;
        switch (cacheFor) {
            case AuthUser:
                runtimeOperations = new SimpleOperationDefinition[] { FLUSH_CACHE_NAME_ONLY, CONTAINS_NAME_ONLY };
                runtimeHandler = NAME_ONLY_HANDLER;
                break;
            default:
                runtimeOperations = new SimpleOperationDefinition[] { FLUSH_CACHE_FULL, CONTAINS_FULL };
                runtimeHandler = FULL_HANDLER;
        }

        return new LdapCacheResourceDefinition(pathElement, configurationAttributes, runtimeAttributes, runtimeOperations,
                runtimeHandler);
    }

    public static ResourceDefinition createByAccessTime(final CacheFor cacheFor) {
        return create(PathElement.pathElement(ModelDescriptionConstants.CACHE, ModelDescriptionConstants.BY_ACCESS_TIME),
                cacheFor);
    }

    public static ResourceDefinition createBySearchTime(final CacheFor cacheFor) {
        return create(PathElement.pathElement(ModelDescriptionConstants.CACHE, ModelDescriptionConstants.BY_SEARCH_TIME),
                cacheFor);
    }

    /**
     * Creates an operations that targets the valiadating handler.
     *
     * @param operationToValidate the operation that this handler will validate
     * @return  the validation operation
     */
    private static ModelNode createOperation(final ModelNode operationToValidate) {
        PathAddress pa = PathAddress.pathAddress(operationToValidate.require(OP_ADDR));
        PathAddress validationAddress = pa.subAddress(0, pa.size() - 1);

        return Util.getEmptyOperation("validate-cache", validationAddress.toModelNode());
    }

    public enum CacheFor {
        AuthUser, AuthzUser, AuthzGroup
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        OperationStepHandler writeHandler = new SecurityRealmChildWriteAttributeHandler(configurationAttributes);
        for (SimpleAttributeDefinition attr : configurationAttributes) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
        for (SimpleAttributeDefinition attr : runtimeAttributes) {
            resourceRegistration.registerReadOnlyAttribute(attr, runtimeStepHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        for (SimpleOperationDefinition op : runtimeOperations) {
            resourceRegistration.registerOperationHandler(op, runtimeStepHandler);
        }
    }

    private abstract static class BaseRuntimeOpHandler<K> implements OperationStepHandler {

        private static final Set<String> VALID_OPS;

        static {
            Set<String> ops = new HashSet<String>(3);
            ops.add(READ_ATTRIBUTE_OPERATION);
            ops.add(FLUSH_CACHE);
            ops.add(CONTAINS);

            VALID_OPS = Collections.unmodifiableSet(ops);
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String operationName = operation.get(OP).asString();
            if (VALID_OPS.contains(operationName)) {
                context.addStep(new OperationStepHandler() {

                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
                            readAttribute(context, operation);
                        } else if (FLUSH_CACHE.equals(operationName)) {
                            flushCache(context, operation);
                        } else if (CONTAINS.equals(operationName)) {
                            contains(context, operation);
                        }

                        context.stepCompleted();
                    }
                }, Stage.RUNTIME);
            }

            context.stepCompleted();
        }

        public abstract void flushCache(OperationContext context, ModelNode operation) throws OperationFailedException;
        public abstract void contains(OperationContext context, ModelNode operation) throws OperationFailedException;

        public void readAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {


            String name = operation.get(ModelDescriptionConstants.NAME).asString();
            if (ModelDescriptionConstants.CACHE_SIZE.equals(name)) {
                LdapSearcherCache<?, K> ldapCacheService = lookupService(context, operation);

                context.getResult().set(ldapCacheService.getCurrentSize());
            }
        }

        protected LdapSearcherCache<?, K> lookupService(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            String realmName = null;
            boolean forAuthentication = false;
            boolean forUserSearch = false;

            PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            for (PathElement current : address) {
                String key = current.getKey();
                if (SECURITY_REALM.equals(key)) {
                    realmName = current.getValue();
                } else if (AUTHENTICATION.equals(key)) {
                    forAuthentication = true;
                    forUserSearch = true;
                } else if (AUTHORIZATION .equals(key)) {
                    forAuthentication = false;
                } else if (USERNAME_TO_DN.equals(key)) {
                    forUserSearch = true;
                } else if (GROUP_SEARCH.equals(key)) {
                    forUserSearch = false;
                }
            }
            ServiceName serviceName = LdapSearcherCache.ServiceUtil.createServiceName(forAuthentication, forUserSearch, realmName);

            ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController<LdapSearcherCache<?, K>> service = (ServiceController<LdapSearcherCache<?, K>>) registry.getRequiredService(serviceName);

            try {
                return service.awaitValue();
            } catch (InterruptedException e) {
                throw new OperationFailedException(e);
            }
        }

    }

    private static class NameOnlyOpHandler extends BaseRuntimeOpHandler<String> {

        @Override
        public void flushCache(OperationContext context, ModelNode operation) throws OperationFailedException {
            LdapSearcherCache<?, String> ldapCacheService = lookupService(context, operation);
            if (operation.hasDefined(ModelDescriptionConstants.NAME)) {
                String name = operation.require(ModelDescriptionConstants.NAME).asString();
                ldapCacheService.clear(name);
            } else {
                ldapCacheService.clearAll();
            }
        }

        @Override
        public void contains(OperationContext context, ModelNode operation) throws OperationFailedException {
            LdapSearcherCache<?, String> ldapCacheService = lookupService(context, operation);

            String name = operation.require(ModelDescriptionConstants.NAME).asString();
            context.getResult().set(ldapCacheService.contains(name));
        }

    }

    private static class FullOpHandler extends BaseRuntimeOpHandler<LdapEntry> {

        @Override
        public void flushCache(OperationContext context, ModelNode operation) throws OperationFailedException {
            LdapSearcherCache<?, LdapEntry> ldapCacheService = lookupService(context, operation);

            String name = null;
            String distinguishedName = null;

            if (operation.hasDefined(ModelDescriptionConstants.NAME)) {
                name = operation.require(ModelDescriptionConstants.NAME).asString();
            }
            if (operation.hasDefined(ModelDescriptionConstants.DISTINGUISHED_NAME)) {
                distinguishedName = operation.require(ModelDescriptionConstants.DISTINGUISHED_NAME).asString();
            }

            if (name == null && distinguishedName == null) {
                ldapCacheService.clearAll();
            } else if (name != null && distinguishedName != null) {
                ldapCacheService.clear(new LdapEntry(name, distinguishedName));
            } else {
                ldapCacheService.clear(new LdapEntryPredicate(name, distinguishedName));
            }
        }

        @Override
        public void contains(OperationContext context, ModelNode operation) throws OperationFailedException {
            LdapSearcherCache<?, LdapEntry> ldapCacheService = lookupService(context, operation);

            String name = null;
            String distinguishedName = null;

            if (operation.hasDefined(ModelDescriptionConstants.NAME)) {
                name = operation.require(ModelDescriptionConstants.NAME).asString();
            }
            if (operation.hasDefined(ModelDescriptionConstants.DISTINGUISHED_NAME)) {
                distinguishedName = operation.require(ModelDescriptionConstants.DISTINGUISHED_NAME).asString();
            }

            if (name == null && distinguishedName == null) {
                context.getResult().set(false); // TODO - Maybe report an error instead.
            } else if (name != null && distinguishedName != null) {
                context.getResult().set(ldapCacheService.contains(new LdapEntry(name, distinguishedName)));
            } else {
                boolean contains = ldapCacheService.count(new LdapEntryPredicate(name, distinguishedName)) > 0;
                context.getResult().set(contains);
            }
        }

    }

    private static class LdapEntryPredicate implements Predicate<LdapEntry> {

        private final String name;
        private final String distinguishedName;

        private LdapEntryPredicate(final String name, final String distinguishedName) {
            this.name = name;
            this.distinguishedName = distinguishedName;
        }

        @Override
        public boolean matches(LdapEntry key) {
            if (name != null) {
                if (name.equals(key.getSimpleName()) == false) {
                    return false;
                }
            }

            if (distinguishedName != null) {
                if (distinguishedName.equals(key.getDistinguishedName()) == false) {
                    return false;
                }
            }

            return true;
        }

    }

    static class CacheChildAddHandler extends SecurityRealmChildAddHandler {

        public CacheChildAddHandler(AttributeDefinition[] attributeDefinitions) {
            super(false, false, attributeDefinitions);
        }

        @Override
        protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.updateModel(context, operation);

            ModelNode validateOp = createOperation(operation);
            context.addStep(validateOp, VALIDATION_INSTANCE, Stage.MODEL);
        }
    }

    private static class CacheDefintionValidatingHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            Set<String> children = resource.getChildrenNames(ModelDescriptionConstants.CACHE);
            if (children.size() > 1) {
                String realmName = ManagementUtil.getSecurityRealmName(operation);
                throw DomainManagementMessages.MESSAGES.multipleCacheConfigurationsDefined(realmName);
            }

            context.stepCompleted();

        }
    }

}
