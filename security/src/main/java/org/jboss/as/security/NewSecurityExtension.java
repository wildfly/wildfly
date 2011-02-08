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

import java.util.Locale;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * The security extension
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NewSecurityExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "security";

    private static final NewSecuritySubsystemParser PARSER = new NewSecuritySubsystemParser();

    @Override
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return SecuritySubsystemDescriptions.getSubsystemRoot(locale);
            }
        });
        registration.registerOperationHandler(ADD, NewSecuritySubsystemAdd.INSTANCE, NewSecuritySubsystemAdd.INSTANCE,
                false);

        final ModelNodeRegistration jaas = registration.registerSubModel(PathElement.pathElement(CommonAttributes.JAAS_APPLICATION_POLICY), new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return SecuritySubsystemDescriptions.getJaasApplicationPolicy(locale);
            }
        });
        jaas.registerOperationHandler(NewJaasApplicationPolicyAdd.OPERATION_NAME, NewJaasApplicationPolicyAdd.INSTANCE, NewJaasApplicationPolicyAdd.INSTANCE, false);
        jaas.registerOperationHandler(NewJaasApplicationPolicyRemove.OPERATION_NAME, NewJaasApplicationPolicyRemove.INSTANCE, NewJaasApplicationPolicyRemove.INSTANCE, false);
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

}
