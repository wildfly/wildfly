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
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

/**
 * The security extension
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
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return SecuritySubsystemDescriptions.getSubsystemRoot(locale);
            }
        });
        registration.registerOperationHandler(ADD, SecuritySubsystemAdd.INSTANCE, SecuritySubsystemAdd.INSTANCE,
                false);

        final ModelNodeRegistration jaas = registration.registerSubModel(PathElement.pathElement(CommonAttributes.JAAS_APPLICATION_POLICY), new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return SecuritySubsystemDescriptions.getJaasApplicationPolicy(locale);
            }
        });
        jaas.registerOperationHandler(JaasApplicationPolicyAdd.OPERATION_NAME, JaasApplicationPolicyAdd.INSTANCE, JaasApplicationPolicyAdd.INSTANCE, false);
        jaas.registerOperationHandler(JaasApplicationPolicyRemove.OPERATION_NAME, JaasApplicationPolicyRemove.INSTANCE, JaasApplicationPolicyRemove.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, SecurityDescribeHandler.INSTANCE, SecurityDescribeHandler.INSTANCE, false);
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    private static class SecurityDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final SecurityDescribeHandler INSTANCE = new SecurityDescribeHandler();
        @Override
        public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            final ModelNode model = context.getSubModel();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

            if (model.hasDefined(AUTHENTICATION_MANAGER_CLASS_NAME)) {
                subsystem.get(AUTHENTICATION_MANAGER_CLASS_NAME).set(model.get(AUTHENTICATION_MANAGER_CLASS_NAME));
            }
            if (subsystem.hasDefined(DEEP_COPY_SUBJECT_MODE)) {
                subsystem.get(DEEP_COPY_SUBJECT_MODE).set(model.get(DEEP_COPY_SUBJECT_MODE));

            }
            if (subsystem.hasDefined(DEFAULT_CALLBACK_HANDLER_CLASS_NAME)) {
                subsystem.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).set(model.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME));
            }

            ModelNode result = new ModelNode();
            result.add(subsystem);

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete(null);
            return Cancellable.NULL;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }

    }

}
