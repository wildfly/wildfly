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
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
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

    private static final SecuritySubsystemParser PARSER = SecuritySubsystemParser.getInstance();

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(SecuritySubsystemDescriptions.SUBSYSTEM);
        registration.registerOperationHandler(ADD, SecuritySubsystemAdd.INSTANCE, SecuritySubsystemDescriptions.SUBSYSTEM_ADD,
                false);
        registration.registerOperationHandler(DESCRIBE, SecurityDescribeHandler.INSTANCE,
                SecuritySubsystemDescriptions.SUBSYSTEM_DESCRIBE, false, OperationEntry.EntryType.PRIVATE);

        // security domains
        final ModelNodeRegistration securityDomain = registration.registerSubModel(PathElement.pathElement(SECURITY_DOMAIN),
                SecuritySubsystemDescriptions.SECURITY_DOMAIN);
        securityDomain.registerOperationHandler(SecurityDomainAdd.OPERATION_NAME, SecurityDomainAdd.INSTANCE,
                SecuritySubsystemDescriptions.SECURITY_DOMAIN_ADD, false);
        securityDomain.registerOperationHandler(SecurityDomainRemove.OPERATION_NAME, SecurityDomainRemove.INSTANCE,
                SecuritySubsystemDescriptions.SECURITY_DOMAIN_REMOVE, false);

        // add operations to the security domain
        securityDomain.registerOperationHandler(SecurityDomainOperations.LIST_CACHED_PRINCIPALS,
                SecurityDomainOperations.LIST_CACHED_PRINCIPALS_OP, SecuritySubsystemDescriptions.LIST_CACHED_PRINCIPALS);
        securityDomain.registerOperationHandler(SecurityDomainOperations.FLUSH_CACHE, SecurityDomainOperations.FLUSH_CACHE_OP,
                SecuritySubsystemDescriptions.FLUSH_CACHE);

        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    private static class SecurityDescribeHandler implements NewStepHandler {
        static final SecurityDescribeHandler INSTANCE = new SecurityDescribeHandler();

        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

            if (model.hasDefined(AUTHENTICATION_MANAGER_CLASS_NAME)) {
                subsystem.get(AUTHENTICATION_MANAGER_CLASS_NAME).set(model.get(AUTHENTICATION_MANAGER_CLASS_NAME));
            }
            if (model.hasDefined(DEEP_COPY_SUBJECT_MODE)) {
                subsystem.get(DEEP_COPY_SUBJECT_MODE).set(model.get(DEEP_COPY_SUBJECT_MODE));
            }
            if (model.hasDefined(DEFAULT_CALLBACK_HANDLER_CLASS_NAME)) {
                subsystem.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).set(model.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME));
            }
            if (model.hasDefined(SUBJECT_FACTORY_CLASS_NAME)) {
                subsystem.get(SUBJECT_FACTORY_CLASS_NAME).set(model.get(SUBJECT_FACTORY_CLASS_NAME));
            }
            if (model.hasDefined(AUTHORIZATION_MANAGER_CLASS_NAME)) {
                subsystem.get(AUTHORIZATION_MANAGER_CLASS_NAME).set(model.get(AUTHORIZATION_MANAGER_CLASS_NAME));
            }
            if (model.hasDefined(AUDIT_MANAGER_CLASS_NAME)) {
                subsystem.get(AUDIT_MANAGER_CLASS_NAME).set(model.get(AUDIT_MANAGER_CLASS_NAME));
            }
            if (model.hasDefined(IDENTITY_TRUST_MANAGER_CLASS_NAME)) {
                subsystem.get(IDENTITY_TRUST_MANAGER_CLASS_NAME).set(model.get(IDENTITY_TRUST_MANAGER_CLASS_NAME));
            }
            if (model.hasDefined(MAPPING_MANAGER_CLASS_NAME)) {
                subsystem.get(MAPPING_MANAGER_CLASS_NAME).set(model.get(MAPPING_MANAGER_CLASS_NAME));
            }
            if (model.hasDefined(SECURITY_PROPERTIES)) {
                subsystem.get(SECURITY_PROPERTIES).set(model.get(SECURITY_PROPERTIES));
            }

            ModelNode result = context.getResult();
            result.add(subsystem);

            if (model.hasDefined(SECURITY_DOMAIN)) {
                for (Property prop : model.get(SECURITY_DOMAIN).asPropertyList()) {
                    final ModelNode addr = subsystem.get(OP_ADDR).clone().add(SECURITY_DOMAIN, prop.getName());
                    result.add(SecurityDomainAdd.getRecreateOperation(addr, prop.getValue()));
                }
            }

            context.completeStep();
        }
    }

}
