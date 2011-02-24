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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.CommonAttributes.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.SECURITY_DOMAIN;
import static org.jboss.as.security.CommonAttributes.SUBJECT_FACTORY_CLASS_NAME;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

/**
 * The security extension.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    public static final ServiceName JBOSS_SECURITY = ServiceName.JBOSS.append("security");

    public static final String SUBSYSTEM_NAME = "security";

    private static final SecuritySubsystemParser PARSER = new SecuritySubsystemParser();

    @Override
    public void initialize(ExtensionContext context) {
        log.debug("Initializing Security Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(SecuritySubsystemDescriptions.SUBSYSTEM);
        registration.registerOperationHandler(ADD, SecuritySubsystemAdd.INSTANCE, SecuritySubsystemDescriptions.SUBSYSTEM_ADD,
                false);
        registration.registerOperationHandler(DESCRIBE, SecurityDescribeHandler.INSTANCE,
                SecuritySubsystemDescriptions.SUBSYSTEM_DESCRIBE, false);

        // security domains
        final ModelNodeRegistration securityDomain = registration.registerSubModel(PathElement
                .pathElement(CommonAttributes.SECURITY_DOMAIN), SecuritySubsystemDescriptions.SECURITY_DOMAIN);
        securityDomain.registerOperationHandler(SecurityDomainAdd.OPERATION_NAME, SecurityDomainAdd.INSTANCE,
                SecuritySubsystemDescriptions.SECURITY_DOMAIN_ADD, false);
        securityDomain.registerOperationHandler(SecurityDomainRemove.OPERATION_NAME, SecurityDomainRemove.INSTANCE,
                SecuritySubsystemDescriptions.SECURITY_DOMAIN_REMOVE, false);

        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    private static class SecurityDescribeHandler implements ModelQueryOperationHandler {
        static final SecurityDescribeHandler INSTANCE = new SecurityDescribeHandler();

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            final ModelNode model = context.getSubModel();

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

            ModelNode result = new ModelNode();
            result.add(subsystem);

            if (model.hasDefined(SECURITY_DOMAIN)) {
                for (Property prop : model.get(SECURITY_DOMAIN).asPropertyList()) {
                    final ModelNode addr = subsystem.get(OP_ADDR).clone().add(SECURITY_DOMAIN, prop.getName());
                    result.add(SecurityDomainAdd.getRecreateOperation(addr, prop.getValue()));
                }
            }

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

    }

}
