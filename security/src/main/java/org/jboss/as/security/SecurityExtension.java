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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import static org.jboss.as.security.Constants.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.Constants.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.Constants.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.SECURITY_PROPERTIES;
import static org.jboss.as.security.Constants.SUBJECT_FACTORY_CLASS_NAME;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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

    private static final String RESOURCE_NAME = SecurityExtension.class.getPackage().getName() + ".LocalDescriptions";


    private static final SecuritySubsystemParser PARSER = SecuritySubsystemParser.getInstance();

     static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, SecurityExtension.class.getClassLoader(), true, true);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SecuritySubsystemRootResourceDefinition.INSTANCE);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ManagementResourceRegistration securityDomain = registration.registerSubModel(SecurityDomainResourceDefinition.INSTANCE);
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
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.SECURITY_1_0.getUriString(), PARSER);
        context.setSubsystemXmlMapping(Namespace.SECURITY_1_1.getUriString(), PARSER);
    }
}
