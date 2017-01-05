/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.wildfly.iiop.openjdk;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ConfigValidator {

    private ConfigValidator(){
    }

    public static void validateConfig(final OperationContext context, final ModelNode resourceModel) throws OperationFailedException {
        final boolean supportSSL = IIOPRootDefinition.SUPPORT_SSL.resolveModelAttribute(context, resourceModel).asBoolean();
        final boolean serverRequiresSsl = IIOPRootDefinition.SERVER_REQUIRES_SSL.resolveModelAttribute(context, resourceModel).asBoolean();
        final boolean clientRequiresSsl = IIOPRootDefinition.CLIENT_REQUIRES_SSL.resolveModelAttribute(context, resourceModel).asBoolean();

        validateSSLConfig(context, resourceModel, supportSSL, serverRequiresSsl, clientRequiresSsl);
        validateIORTransportConfig(context, resourceModel, supportSSL, serverRequiresSsl);
        validateORBInitializerConfig(context, resourceModel);
    }

    private static void validateSSLConfig(final OperationContext context, final ModelNode model, final boolean supportSSL,
                                          final boolean serverRequiresSsl, final boolean clientRequiresSsl) throws OperationFailedException {

        if (supportSSL) {
            // if SSL is to be used, then either a JSSE domain or a pair of client/server SSL contexts must be defined.
            final ModelNode securityDomainNode = IIOPRootDefinition.SECURITY_DOMAIN.resolveModelAttribute(context, model);
            final ModelNode serverSSLContextNode = IIOPRootDefinition.SERVER_SSL_CONTEXT.resolveModelAttribute(context, model);
            final ModelNode clientSSLContextNode = IIOPRootDefinition.CLIENT_SSL_CONTEXT.resolveModelAttribute(context, model);
            if (!securityDomainNode.isDefined() && (!serverSSLContextNode.isDefined() || !clientSSLContextNode.isDefined())) {
                throw IIOPLogger.ROOT_LOGGER.noSecurityDomainOrSSLContextsSpecified();
            }
        } else if(serverRequiresSsl || clientRequiresSsl) {
            // if either the server or the client requires SSL, then SSL support must have been enabled.
            throw IIOPLogger.ROOT_LOGGER.sslNotConfigured();
        }
    }

    private static void validateIORTransportConfig(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured,
                                                   final boolean serverRequiresSsl) throws OperationFailedException {
        validateSSLAttribute(context, resourceModel, sslConfigured, serverRequiresSsl, IIOPRootDefinition.INTEGRITY);
        validateSSLAttribute(context, resourceModel, sslConfigured, serverRequiresSsl, IIOPRootDefinition.CONFIDENTIALITY);
        validateSSLAttribute(context, resourceModel, sslConfigured, serverRequiresSsl, IIOPRootDefinition.TRUST_IN_CLIENT);
        validateTrustInTarget(context, resourceModel, sslConfigured);
        validateSupportedAttribute(context, resourceModel, IIOPRootDefinition.DETECT_MISORDERING);
        validateSupportedAttribute(context, resourceModel, IIOPRootDefinition.DETECT_REPLAY);
    }

    private static void validateSSLAttribute(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured, final boolean serverRequiresSsl, final AttributeDefinition attributeDefinition) throws OperationFailedException {
        final ModelNode attributeNode = attributeDefinition.resolveModelAttribute(context, resourceModel);
        if(attributeNode.isDefined()){
            final String attribute = attributeNode.asString();
            if(sslConfigured) {
                if(attribute.equals(Constants.IOR_NONE)){
                    throw IIOPLogger.ROOT_LOGGER.inconsistentSupportedTransportConfig(attributeDefinition.getName());
                }
                if (serverRequiresSsl && serverRequiresSsl && attribute.equals(Constants.IOR_SUPPORTED)) {
                    throw IIOPLogger.ROOT_LOGGER.inconsistentRequiredTransportConfig(Constants.SECURITY_SERVER_REQUIRES_SSL, attributeDefinition.getName());
                }
            } else {
                if(!attribute.equals(Constants.IOR_NONE)){
                    throw IIOPLogger.ROOT_LOGGER.inconsistentUnsupportedTransportConfig(attributeDefinition.getName());
                }
            }
        }
    }

    private static void validateTrustInTarget(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured) throws OperationFailedException {
        final ModelNode establishTrustInTargetNode = IIOPRootDefinition.TRUST_IN_TARGET.resolveModelAttribute(context, resourceModel);
        if(establishTrustInTargetNode.isDefined()){
            final String establishTrustInTarget = establishTrustInTargetNode.asString();
            if(sslConfigured && establishTrustInTarget.equals(Constants.IOR_NONE)){
                throw IIOPLogger.ROOT_LOGGER.inconsistentSupportedTransportConfig(Constants.IOR_TRANSPORT_TRUST_IN_TARGET);
            }
        }
    }

    private static void validateSupportedAttribute(final OperationContext context, final ModelNode resourceModel, final AttributeDefinition attributeDefinition) throws OperationFailedException{
        final ModelNode attributeNode = attributeDefinition.resolveModelAttribute(context, resourceModel);
        if(attributeNode.isDefined() && !attributeNode.asString().equals(Constants.IOR_SUPPORTED)) {
            throw IIOPLogger.ROOT_LOGGER.inconsistentSupportedTransportConfig(attributeDefinition.getName());
        }
    }

    private static void validateORBInitializerConfig(final OperationContext context, final ModelNode resourceModel) throws OperationFailedException {
        // validate the elytron initializer configuration: it requires an authentication-context name.
        final ModelNode securityInitializerNode = IIOPRootDefinition.SECURITY.resolveModelAttribute(context, resourceModel);
        final ModelNode authContextNode = IIOPRootDefinition.AUTHENTICATION_CONTEXT.resolveModelAttribute(context, resourceModel);
        if (securityInitializerNode.isDefined() && securityInitializerNode.asString().equalsIgnoreCase(Constants.ELYTRON)) {
            if (!authContextNode.isDefined()) {
                throw IIOPLogger.ROOT_LOGGER.elytronInitializerMissingAuthContext();
            }
        } else if (authContextNode.isDefined()) {
            // authentication-context has been specified but is ineffective because the security initializer is not set to 'elytron'.
            throw IIOPLogger.ROOT_LOGGER.ineffectiveAuthenticationContextConfiguration();
        }
    }
}
