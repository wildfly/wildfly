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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
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
 */public class SecurityExtension implements Extension {

    public static final ServiceName JBOSS_SECURITY = ServiceName.JBOSS.append("security");

    public static final String SUBSYSTEM_NAME = "security";
    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = SecurityExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 3, 0);

    private static final SecuritySubsystemParser PARSER = SecuritySubsystemParser.getInstance();
    static final PathElement ACL_PATH = PathElement.pathElement(Constants.ACL, Constants.CLASSIC);
    static final PathElement PATH_IDENTITY_TRUST_CLASSIC = PathElement.pathElement(Constants.IDENTITY_TRUST, Constants.CLASSIC);
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

    //deprecated in EAP 6.4
    static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(1,3,0);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, keyPrefix, RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, true);
    }

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, prefix.toString(), RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
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
        registerTransformers_1_1_0(subsystemRegistration);
        registerTransformers_1_2_0(subsystemRegistration);
    }

    private void registerTransformers_1_1_0(SubsystemRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE);
        final ResourceTransformationDescriptionBuilder securityDomain = builder.addChildResource(SECURITY_DOMAIN_PATH);
        securityDomain.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, SecurityDomainResourceDefinition.CACHE_TYPE).end();

        final ModulesToAttributeTransformer authClassicLoginModule = new ModulesToAttributeTransformer(Constants.LOGIN_MODULE, Constants.LOGIN_MODULES);
        registerModuleTransformer(securityDomain, PATH_CLASSIC_AUTHENTICATION, authClassicLoginModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.FLAG, Constants.MODULE_OPTIONS)
                .build());

        final ModulesToAttributeTransformer policyModule = new ModulesToAttributeTransformer(Constants.POLICY_MODULE, Constants.POLICY_MODULES);
        registerModuleTransformer(securityDomain, PATH_AUTHORIZATION_CLASSIC, policyModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.FLAG, Constants.MODULE_OPTIONS)
                .build());

        final ModulesToAttributeTransformer mappingModule = new ModulesToAttributeTransformer(Constants.MAPPING_MODULE, Constants.MAPPING_MODULES);
        registerModuleTransformer(securityDomain, PATH_MAPPING_CLASSIC, mappingModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.TYPE, Constants.MODULE_OPTIONS)
                .build());

        final ModulesToAttributeTransformer providerModule = new ModulesToAttributeTransformer(Constants.PROVIDER_MODULE, Constants.PROVIDER_MODULES);
        registerModuleTransformer(securityDomain, PATH_AUDIT_CLASSIC, providerModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.MODULE_OPTIONS)
                .build());

        final ModulesToAttributeTransformer identityTrustModule = new ModulesToAttributeTransformer(Constants.TRUST_MODULE, Constants.TRUST_MODULES);
        registerModuleTransformer(securityDomain, PATH_IDENTITY_TRUST_CLASSIC, identityTrustModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.FLAG, Constants.MODULE_OPTIONS)
                .build());

        final ModulesToAttributeTransformer aclModule = new ModulesToAttributeTransformer(Constants.ACL_MODULE, Constants.ACL_MODULES);
        registerModuleTransformer(securityDomain, ACL_PATH, aclModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.FLAG, Constants.MODULE_OPTIONS)
                .build());

        final ModulesToAttributeTransformer authModule = new JaspiModulesToAttributeTransformer(Constants.AUTH_MODULE, Constants.AUTH_MODULES);
        ResourceTransformationDescriptionBuilder jaspiReg = registerModuleTransformer(securityDomain, PATH_JASPI_AUTH, authModule,
                ChildResourceTransformersRegistrar.createBuilder()
                .addRejectExpressions(Constants.FLAG, Constants.MODULE_OPTIONS)
                .addRejectIfDefined(Constants.MODULE)
                .build());

        final ModulesToAttributeTransformer authLoginModule = new JaspiModulesToAttributeTransformer(Constants.LOGIN_MODULE, Constants.LOGIN_MODULES);
        registerModuleTransformer(jaspiReg, PATH_LOGIN_MODULE_STACK, authLoginModule, null);

        //reject expressions
        builder.addChildResource(VAULT_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, VaultResourceDefinition.OPTIONS)
                .end();
        securityDomain.addChildResource(JSSE_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, JSSEResourceDefinition.ADDITIONAL_PROPERTIES)
                .end();

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 1, 0));
    }

    private void registerTransformers_1_2_0(SubsystemRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        ResourceTransformationDescriptionBuilder securityDomain = builder.addChildResource(SECURITY_DOMAIN_PATH);

        // Transform any add op that includes the module list attribute into a compsosite of an add w/o that + write-attribute
        AttributeToModulesTransformer loginModule = new AttributeToModulesTransformer(Constants.LOGIN_MODULES);
        registerModuleTransformer(securityDomain, PATH_CLASSIC_AUTHENTICATION, loginModule);
        AttributeToModulesTransformer policyModule = new AttributeToModulesTransformer(Constants.POLICY_MODULES);
        registerModuleTransformer(securityDomain, PATH_AUTHORIZATION_CLASSIC, policyModule);
        AttributeToModulesTransformer mappingModule = new AttributeToModulesTransformer(Constants.MAPPING_MODULES);
        registerModuleTransformer(securityDomain, PATH_MAPPING_CLASSIC, mappingModule);
        AttributeToModulesTransformer providerModule = new AttributeToModulesTransformer(Constants.PROVIDER_MODULES);
        registerModuleTransformer(securityDomain, PATH_AUDIT_CLASSIC, providerModule);
        final AttributeToModulesTransformer identityTrustModule = new AttributeToModulesTransformer(Constants.TRUST_MODULES);
        registerModuleTransformer(securityDomain, PATH_IDENTITY_TRUST_CLASSIC, identityTrustModule);
        final AttributeToModulesTransformer aclModule = new AttributeToModulesTransformer(Constants.ACL_MODULES);
        ResourceTransformationDescriptionBuilder aclBuilder = registerModuleTransformer(securityDomain, ACL_PATH, aclModule);
        //https://issues.jboss.org/browse/WFLY-2474 acl-module was wrongly called login-module in 7.2.0
        aclBuilder.addChildRedirection(PathElement.pathElement(Constants.ACL_MODULE), PathElement.pathElement(Constants.LOGIN_MODULE));

        AttributeToModulesTransformer authModule = new AttributeToModulesTransformer(Constants.AUTH_MODULES);
        ResourceTransformationDescriptionBuilder jaspiReg = registerModuleTransformer(securityDomain, PATH_JASPI_AUTH, authModule);

        // the module attribute is not recognized in the 1.2.0 version of the subsystem.
        jaspiReg.addChildResource(PathElement.pathElement(Constants.AUTH_MODULE)).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MODULE)
                .end();

        registerModuleTransformer(jaspiReg, PATH_LOGIN_MODULE_STACK, loginModule);

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 2, 0));
    }

    private ResourceTransformationDescriptionBuilder registerModuleTransformer(final ResourceTransformationDescriptionBuilder parent, final PathElement childPath,
                                                                               final ModulesToAttributeTransformer transformer, final ChildResourceTransformersRegistrar childRegistrar) {
        final OperationTransformer addOrWriteTransformer = new OperationTransformer() {
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                    throws OperationFailedException {
                return transformer.transformParentOperation(context, address, operation);
            }
        };

        ResourceTransformationDescriptionBuilder child = parent.addChildResource(childPath)
                .setCustomResourceTransformer(transformer)
                .addOperationTransformationOverride(ADD).setCustomOperationTransformer(addOrWriteTransformer).inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(WRITE_ATTRIBUTE_OPERATION).setCustomOperationTransformer(addOrWriteTransformer).inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(UNDEFINE_ATTRIBUTE_OPERATION).setCustomOperationTransformer(addOrWriteTransformer).inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(REMOVE)
                    .setCustomOperationTransformer(new OperationTransformer() {
                        public TransformedOperation transformOperation(TransformationContext context, PathAddress address,
                                ModelNode operation) throws OperationFailedException {
                            //Record that we removed the resource and return the original operation
                            return new TransformedOperation(operation, TransformedOperation.ORIGINAL_RESULT);
                        }
                    }).end();
        ResourceTransformationDescriptionBuilder childRedirectionBuilder = child.addChildRedirection(
                    PathElement.pathElement(transformer.getResourceName()), CURRENT_PATH_TRANSFORMER)
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

        if (childRegistrar != null) {
            childRegistrar.registerTransformers(childRedirectionBuilder);
        }
        return child;
    }

    private ResourceTransformationDescriptionBuilder registerModuleTransformer(ResourceTransformationDescriptionBuilder parent, final PathElement childPath,
                                                                               AttributeToModulesTransformer transformer) {
        return parent.addChildResource(childPath)
            .addOperationTransformationOverride(ADD)
                .inheritResourceAttributeDefinitions()
                .setCustomOperationTransformer(transformer)
                .end();
    }


    private static class ModulesToAttributeTransformer implements CombinedTransformer {
        protected final String resourceName;
        protected final String attributeName;

        private ModulesToAttributeTransformer(String resourceName, String oldName) {
            this.resourceName = resourceName;
            this.attributeName = oldName;
        }

        String getResourceName() {
            return resourceName;
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            ModelNode model = new ModelNode();
            transformModulesToAttributes(address, resourceName, attributeName, context, model);
            resource.writeModel(model);
            final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            childContext.processChildren(resource);
        }

        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            ModelNode model = new ModelNode();
            transformModulesToAttributes(address, resourceName, attributeName, context, model);
            ModelNode modules = model.get(attributeName);
            int len = modules.asList().size();
            //Here we always remove, and if we have enough information, we overwrite the value
            if (len == 0) {
                return new TransformedOperation(Util.createRemoveOperation(address), OperationResultTransformer.ORIGINAL_RESULT);
            } else {
                ModelNode add = Util.createAddOperation(address);
                add.get(attributeName).set(modules);
                return new TransformedOperation(createComposite(address, add), OperationResultTransformer.ORIGINAL_RESULT);
            }
        }

        void transformModulesToAttributes(final PathAddress address, final String newName, final String oldName, final TransformationContext context, final ModelNode model) {
            ModelNode modules = model.get(oldName).setEmptyList();
            Set<Resource.ResourceEntry> children = context.readResourceFromRoot(address).getChildren(newName);
            if (children != null) {
                for (Resource.ResourceEntry entry : children) {
                    Resource moduleResource = context.readResourceFromRoot(address.append(entry.getPathElement()));
                    modules.add(moduleResource.getModel());
                }
            }
        }

        ModelNode createComposite(PathAddress address, ModelNode...steps) {
            ModelNode composite = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
            ModelNode stepsNode = composite.get(STEPS);
            stepsNode.add(Util.createRemoveOperation(address));
            for (ModelNode step : steps) {
                stepsNode.add(step);
            }
            return composite;
        }

        TransformedOperation transformParentOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            //If the add is the form which has the attribute set, let it through - otherwise just discard
            String op = operation.get(OP).asString();
            if (ADD.equals(op)) {
                if (operation.hasDefined(attributeName)) {
                    return new TransformedOperation(operation, TransformedOperation.ORIGINAL_RESULT);
                }
            } else if (op.equals(WRITE_ATTRIBUTE_OPERATION) || op.equals(UNDEFINE_ATTRIBUTE_OPERATION)) {
                return transformOperation(context, address, operation);
            }
            return OperationTransformer.DISCARD.transformOperation(context, address, operation);
        }
    }

    private static class JaspiModulesToAttributeTransformer extends ModulesToAttributeTransformer {
        final boolean loginModule;
        private JaspiModulesToAttributeTransformer(String resourceName, String oldName) {
            super(resourceName, oldName);
            this.loginModule = oldName.equals(Constants.LOGIN_MODULES);
        }

        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            //Within the authentication=jaspi resource the structure is:
            // authentication=jaspi
            //    auth-module=*  (should get pulled up into authentication=jaspi)
            //    login-module-stack=*
            //       login-module=*  (should get pulled up into login-module-stack
            //
            // If there are no auth-module children, and we are the login-module-stack, we cannot be added
            PathAddress jaspiAddress = getJaspiAuthenticationAddress(address);

            ModelNode authModulesModel = new ModelNode();
            transformModulesToAttributes(jaspiAddress, Constants.AUTH_MODULE, Constants.AUTH_MODULES, context, authModulesModel);

            ModelNode authModules = authModulesModel.get(Constants.AUTH_MODULES);
            if (authModules.asList().size() == 0) {
                return new TransformedOperation(Util.createRemoveOperation(jaspiAddress), OperationResultTransformer.ORIGINAL_RESULT);
            } else {
                List<ModelNode> list = new ArrayList<ModelNode>();

                ModelNode addJaspi = Util.createAddOperation(jaspiAddress);
                addJaspi.get(Constants.AUTH_MODULES).set(authModules);
                list.add(addJaspi);

                Resource jaspiResource = context.readResourceFromRoot(jaspiAddress);
                for (ResourceEntry entry : jaspiResource.getChildren(Constants.LOGIN_MODULE_STACK)) {
                    PathAddress stackAddress = jaspiAddress.append(PathElement.pathElement(Constants.LOGIN_MODULE_STACK, entry.getName()));

                    ModelNode loginModulesModel = new ModelNode();
                    transformModulesToAttributes(stackAddress, Constants.LOGIN_MODULE, Constants.LOGIN_MODULES, context, loginModulesModel);

                    ModelNode loginModules = loginModulesModel.get(Constants.LOGIN_MODULES);
                    if (loginModules.asList().size() > 0) {
                        ModelNode addStack = Util.createAddOperation(stackAddress);
                        addStack.get(Constants.LOGIN_MODULES).set(loginModules);
                        list.add(addStack);
                    }
                }

                return new TransformedOperation(createComposite(jaspiAddress, list.toArray(new ModelNode[list.size()])), OperationResultTransformer.ORIGINAL_RESULT);
            }
        }

        private PathAddress getJaspiAuthenticationAddress(PathAddress address) {
            PathAddress jaspi = PathAddress.EMPTY_ADDRESS;
            for (PathElement element : address) {
                jaspi = jaspi.append(element);
                if (element.equals(PATH_JASPI_AUTH)) {
                    break;
                }
            }
            return jaspi;
        }
    }

    private static class AttributeToModulesTransformer implements OperationTransformer {

        private final String attributeName;

        private AttributeToModulesTransformer(String attributeName) {
            this.attributeName = attributeName;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {

            final ModelNode transformedOp;
            if (!operation.has(attributeName)) {
                transformedOp = operation;
            } else if (!operation.hasDefined(attributeName)) {
                transformedOp = operation.clone();
                transformedOp.remove(attributeName);
            } else {
                // Convert to a composite of an add without the module list + a write-attribute for the module list
                PathAddress pa = PathAddress.pathAddress(operation.get(OP_ADDR));
                transformedOp = Util.createEmptyOperation(COMPOSITE, null);
                ModelNode steps = transformedOp.get(STEPS);
                ModelNode clone = operation.clone();
                clone.remove(attributeName);
                steps.add(clone);
                ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, pa);
                writeOp.get(NAME).set(attributeName);
                writeOp.get(VALUE).set(operation.get(attributeName));
                steps.add(writeOp);
            }

            return new TransformedOperation(transformedOp, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }


    private static class ChildResourceTransformersRegistrar {
        private String[] reject;
        private String[] defined;

        public ChildResourceTransformersRegistrar(String[] reject, String[] defined) {
            this.reject = reject;
            this.defined = defined;
        }

        void registerTransformers(ResourceTransformationDescriptionBuilder builder) {
            if (reject != null) {
                builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, reject);
            }
            if (defined != null) {
                builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, defined);
            }
        }

        static Builder createBuilder() {
            return new Builder();
        }

        static class Builder {
            private List<String> reject;
            private List<String> defined;

            Builder addRejectExpressions(String...attributes){
                reject = addToList(reject, attributes);
                return this;
            }

            Builder addRejectIfDefined(String...attributes){
                defined = addToList(defined, attributes);
                return this;
            }

            ChildResourceTransformersRegistrar build() {
                return new ChildResourceTransformersRegistrar(
                        reject == null ? null : reject.toArray(new String[reject.size()]),
                        defined == null ? null : defined.toArray(new String[defined.size()]));
            }

            private List<String> addToList(List<String> attrs, String...attributes){
                if (attrs == null) {
                    attrs = new ArrayList<String>();
                }
                for (String attr : attributes) {
                    attrs.add(attr);
                }
                return attrs;
            }
        }
    }

}
