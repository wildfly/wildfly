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
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
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

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, true);
    }
    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
           StringBuilder prefix = new StringBuilder();
           for (String kp : keyPrefix) {
               if (prefix.length()>0){
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
        final ManagementResourceRegistration jaspi = securityDomain.registerSubModel(JASPIAuthenticationResourceDefinition.INSTANCE);
        jaspi.registerSubModel(LoginModuleStackResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(ClassicAuthenticationResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(AuthorizationResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(MappingResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(ACLResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(AuditResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(IdentityTrustResourceDefinition.INSTANCE);
        securityDomain.registerSubModel(JSSEResourceDefinition.INSTANCE);
        registration.registerSubModel(VaultResourceDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(PARSER);
        registerTransformers(subsystem);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_0.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_1.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_2.getUriString(), PARSER);
    }

    private void registerTransformers(SubsystemRegistration subsystemRegistration) {
        TransformersSubRegistration subsystemTransformer = subsystemRegistration.registerModelTransformers(ModelVersion.create(1, 1), null);
        TransformersSubRegistration securityDomain = subsystemTransformer.registerSubResource(SECURITY_DOMAIN_PATH);
        securityDomain.registerSubResource(PATH_CLASSIC_AUTHENTICATION, new ModulesToAttributeTransformer(Constants.LOGIN_MODULE, Constants.LOGIN_MODULES))
                .registerSubResource(PathElement.pathElement(Constants.LOGIN_MODULE), true);
        securityDomain.registerSubResource(PATH_AUTHORIZATION_CLASSIC, new ModulesToAttributeTransformer(Constants.POLICY_MODULE, Constants.POLICY_MODULES))
                .registerSubResource(PathElement.pathElement(Constants.POLICY_MODULE), true);
        securityDomain.registerSubResource(PATH_MAPPING_CLASSIC, new ModulesToAttributeTransformer(Constants.MAPPING_MODULE, Constants.MAPPING_MODULES))
                .registerSubResource(PathElement.pathElement(Constants.MAPPING_MODULE), true);
        securityDomain.registerSubResource(PATH_AUDIT_CLASSIC, new ModulesToAttributeTransformer(Constants.PROVIDER_MODULE, Constants.PROVIDER_MODULES))
                .registerSubResource(PathElement.pathElement(Constants.PROVIDER_MODULE), true);
        TransformersSubRegistration jaspiReg = securityDomain.registerSubResource(PATH_JASPI_AUTH, new ModulesToAttributeTransformer(Constants.AUTH_MODULE, Constants.AUTH_MODULES));
        jaspiReg.registerSubResource(PathElement.pathElement(Constants.AUTH_MODULE), true);
        jaspiReg.registerSubResource(PATH_LOGIN_MODULE_STACK, new ModulesToAttributeTransformer(Constants.LOGIN_MODULE, Constants.LOGIN_MODULES))
                .registerSubResource(PathElement.pathElement(Constants.LOGIN_MODULE), true);
    }


    private static void transformModulesToAttributes(final PathAddress address, final String newName, final String oldName, final TransformationContext context, final ModelNode model) {
        ModelNode modules = model.get(oldName);
        for (Resource.ResourceEntry entry : context.readResource(address).getChildren(newName)) {
            Resource moduleResource = context.readResource(address.append(entry.getPathElement()));
            modules.add(moduleResource.getModel());
        }
    }

    private static class ModulesToAttributeTransformer implements OperationTransformer {
        private final String resourceName;
        private final String oldName;

        ModulesToAttributeTransformer(String resourceName, String oldName) {
            this.resourceName = resourceName;
            this.oldName = oldName;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            transformModulesToAttributes(address, resourceName, oldName, context, operation);
            TransformedOperation op = new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
            return op;
        }

    }
}
