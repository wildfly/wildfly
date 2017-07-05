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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions;
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

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 0, 0);

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

    //deprecated in EAP 6.4
    static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(1,3,0);

    @SuppressWarnings("deprecation")
    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, keyPrefix, RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, true);
    }

    @SuppressWarnings("deprecation")
    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
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
        // register the elytron integration resources.
        registration.registerSubModel(ElytronIntegrationResourceDefinitions.getElytronRealmResourceDefinition());
        registration.registerSubModel(ElytronIntegrationResourceDefinitions.getElytronKeyStoreResourceDefinition());
        registration.registerSubModel(ElytronIntegrationResourceDefinitions.getElytronTrustStoreResourceDefinition());
        registration.registerSubModel(ElytronIntegrationResourceDefinitions.getElytronKeyManagersResourceDefinition());
        registration.registerSubModel(ElytronIntegrationResourceDefinitions.getElytronTrustManagersResourceDefinition());
        // register the subsystem XML persister.
        subsystem.registerXMLElementWriter(SecuritySubsystemPersister.INSTANCE);

    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_0.getUriString(), SecuritySubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_1.getUriString(), SecuritySubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_1_2.getUriString(), SecuritySubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.SECURITY_2_0.getUriString(), SecuritySubsystemParser_2_0::new);
    }
}