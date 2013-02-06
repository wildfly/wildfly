/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * The security extension.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityExtension implements Extension {

    public static final ServiceName JBOSS_SECURITY = ServiceName.JBOSS.append("security");

    public static final String SUBSYSTEM_NAME = "security";
    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = SecurityExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    private static final SecuritySubsystemParser PARSER = SecuritySubsystemParser.getInstance();
    static final PathElement ACL_PATH = PathElement.pathElement(Constants.ACL, Constants.CLASSIC);
    static final PathElement PATH_JASPI_AUTH = PathElement.pathElement(Constants.AUTHENTICATION, Constants.JASPI);
    static final PathElement PATH_CLASSIC_AUTHENTICATION = PathElement.pathElement(Constants.AUTHENTICATION, Constants.CLASSIC);
    static final PathElement SECURITY_DOMAIN_PATH = PathElement.pathElement(Constants.SECURITY_DOMAIN);
    static final PathElement PATH_AUTHORIZATION_CLASSIC = PathElement.pathElement(Constants.AUTHORIZATION, Constants.CLASSIC);
    static final PathElement PATH_MAPPING_CLASSIC = PathElement.pathElement(Constants.MAPPING, Constants.CLASSIC);
    static final PathElement PATH_AUDIT_CLASSIC = PathElement.pathElement(Constants.AUDIT, Constants.CLASSIC);
    static final PathElement PATH_LOGIN_MODULE_STACK = PathElement.pathElement(Constants.LOGIN_MODULE_STACK);
    static final PathElement VAULT_PATH = PathElement.pathElement(Constants.VAULT, Constants.CLASSIC);
    static final PathElement JSSE_PATH = PathElement.pathElement(Constants.JSSE, Constants.CLASSIC);
    private static final PathAddressTransformer CURRENT_PATH_TRANSFORMER = new PathAddressTransformer() {
        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            return builder.getCurrent();
        }
    };

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, true);
    }

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SecuritySubsystemRootResourceDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        final ManagementResourceRegistration securityDomain = registration.registerSubModel(new SecurityDomainResourceDefinition(registerRuntimeOnly));
        securityDomain.registerSubModel(JASPIAuthenticationResourceDefinition.INSTANCE);

        securityDomain.registerSubModel(ClassicAuthenticationResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(AuthorizationResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(MappingResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(ACLResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(AuditResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(IdentityTrustResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(JSSEResourceDefinition.INSTANCE);
        registration.registerSubModel(VaultResourceDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(PARSER);

        if (context.isRegisterTransformers()) {
            registerTransformers(subsystem);
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_0.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_1.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_2.getUriString(), PARSER);
    }

    private void registerTransformers(SubsystemRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE);
        ResourceTransformationDescriptionBuilder securityDomain = builder.addChildResource(SECURITY_DOMAIN_PATH);
        securityDomain.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, SecurityDomainResourceDefinition.CACHE_TYPE).end();


        ModulesToAttributeTransformer loginModule = new ModulesToAttributeTransformer(Constants.LOGIN_MODULE, Constants.LOGIN_MODULES);

        registerModuleTransformer(securityDomain, PATH_CLASSIC_AUTHENTICATION, loginModule);
        final ModulesToAttributeTransformer policyModule = new ModulesToAttributeTransformer(Constants.POLICY_MODULE, Constants.POLICY_MODULES);
        registerModuleTransformer(securityDomain, PATH_AUTHORIZATION_CLASSIC, policyModule);
        final ModulesToAttributeTransformer mappingModule = new ModulesToAttributeTransformer(Constants.MAPPING_MODULE, Constants.MAPPING_MODULES);
        registerModuleTransformer(securityDomain, PATH_MAPPING_CLASSIC, mappingModule);
        final ModulesToAttributeTransformer providerModule = new ModulesToAttributeTransformer(Constants.PROVIDER_MODULE, Constants.PROVIDER_MODULES);
        registerModuleTransformer(securityDomain, PATH_AUDIT_CLASSIC, providerModule);


        final ModulesToAttributeTransformer authModule = new ModulesToAttributeTransformer(Constants.AUTH_MODULE, Constants.AUTH_MODULES);

        ResourceTransformationDescriptionBuilder jaspiReg = registerModuleTransformer(securityDomain, PATH_JASPI_AUTH, authModule);
        //todo check if there are cases when child is created before authentication=jaspi
        registerModuleTransformer(jaspiReg, PATH_LOGIN_MODULE_STACK, loginModule);

        //reject expressions
        builder.addChildResource(VAULT_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, VaultResourceDefinition.CODE, VaultResourceDefinition.OPTIONS)
                .end();
        builder.addChildResource(JSSE_PATH).getAttributeBuilder()
                .addRejectCheck(new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(
                        Collections.singletonMap(JSSEResourceDefinition.ADDITIONAL_PROPERTIES.getName(), RejectAttributeChecker.SIMPLE_EXPRESSIONS))
                        , JSSEResourceDefinition.ADDITIONAL_PROPERTIES
                ).end();

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 1, 0));
    }

    private ResourceTransformationDescriptionBuilder registerModuleTransformer(ResourceTransformationDescriptionBuilder parent, final PathElement childPath,
                                                                               ModulesToAttributeTransformer transformer) {
        ResourceTransformationDescriptionBuilder child = parent.addChildResource(childPath)
                .setCustomResourceTransformer(transformer)
                .discardOperations(ADD);
        //child.discardChildResource(PathElement.pathElement(transformer.getResourceName()));

        child.addChildRedirection(PathElement.pathElement(transformer.getResourceName()), CURRENT_PATH_TRANSFORMER)
                .setCustomResourceTransformer(ResourceTransformer.DISCARD)
                .addOperationTransformationOverride(ADD)
                .setCustomOperationTransformer(transformer)
                .inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(WRITE_ATTRIBUTE_OPERATION)
                .setCustomOperationTransformer(transformer)
                .inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(REMOVE)
                .setCustomOperationTransformer(transformer)
                .inheritResourceAttributeDefinitions().end();
        return child;
    }


    private static class ModulesToAttributeTransformer implements CombinedTransformer {
        protected final String resourceName;
        protected final String oldName;
        private final Map<PathAddress, Boolean> addOps = new HashMap<PathAddress, Boolean>();

        private ModulesToAttributeTransformer(String resourceName, String oldName) {
            this.resourceName = resourceName;
            this.oldName = oldName;
        }

        String getResourceName() {
            return resourceName;
        }

        private boolean isAddOpAdded(PathAddress address) {
            if (addOps.containsKey(address)) {
                return true;
            }
            addOps.put(address, true);
            return false;
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            ModelNode model = new ModelNode();
            transformModulesToAttributes(address, resourceName, oldName, context, model);
            resource.writeModel(model);
            final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            childContext.processChildren(resource);
        }

        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            ModelNode model = new ModelNode();
            transformModulesToAttributes(address, resourceName, oldName, context, model);
            ModelNode modules = model.get(oldName);
            int len = modules.asList().size();
            boolean addOpAdded = isAddOpAdded(address);
            if (len == 0) { //remove
                operation = new ModelNode();
                operation.get(OP).set(REMOVE);
            } else if (!addOpAdded) {
                operation = model.clone();
                operation.get(OP).set(ADD);
            } else {
                operation.clear();
                operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                operation.get(ModelDescriptionConstants.NAME).set(oldName);
                operation.get(ModelDescriptionConstants.VALUE).set(modules);
            }
            operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }

        private void transformModulesToAttributes(final PathAddress address, final String newName, final String oldName, final TransformationContext context, final ModelNode model) {
            ModelNode modules = model.get(oldName).setEmptyList();
            for (Resource.ResourceEntry entry : context.readResourceFromRoot(address).getChildren(newName)) {
                Resource moduleResource = context.readResourceFromRoot(address.append(entry.getPathElement()));
                modules.add(moduleResource.getModel());
            }
        }
    }


}
