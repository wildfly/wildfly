/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ConfigValidator {

    private ConfigValidator(){
    }

    public static List<String> validateConfig(final OperationContext context, final ModelNode resourceModel) throws OperationFailedException {
        final List<String> warnings = new LinkedList<>();

        validateSocketBindings(context, resourceModel);

        final boolean supportSSL = IIOPRootDefinition.SUPPORT_SSL.resolveModelAttribute(context, resourceModel).asBoolean();
        final boolean serverRequiresSSL = IIOPRootDefinition.SERVER_REQUIRES_SSL.resolveModelAttribute(context, resourceModel).asBoolean();
        final boolean clientRequiresSSL = IIOPRootDefinition.CLIENT_REQUIRES_SSL.resolveModelAttribute(context, resourceModel).asBoolean();

        final boolean securityDomainConfigured = IIOPRootDefinition.SECURITY_DOMAIN.resolveModelAttribute(context, resourceModel).isDefined();

        final boolean serverSSLConfigured = IIOPRootDefinition.SERVER_SSL_CONTEXT.resolveModelAttribute(context, resourceModel).isDefined() || securityDomainConfigured;
        final boolean clientSSLConfigured = IIOPRootDefinition.CLIENT_SSL_CONTEXT.resolveModelAttribute(context, resourceModel).isDefined() || securityDomainConfigured;

        validateSSLConfig(supportSSL, serverSSLConfigured, clientSSLConfigured, serverRequiresSSL, clientRequiresSSL);
        validateSSLSocketBinding(context, resourceModel, serverSSLConfigured, clientSSLConfigured, warnings);
        validateIORTransportConfig(context, resourceModel, supportSSL, serverRequiresSSL, warnings);
        validateORBInitializerConfig(context, resourceModel);

        return warnings;
    }

    private static void validateSocketBindings(final OperationContext context, final ModelNode resourceModel) throws OperationFailedException {
        final ModelNode socketBinding = IIOPRootDefinition.SOCKET_BINDING.resolveModelAttribute(context, resourceModel);
        final ModelNode sslSocketBinding = IIOPRootDefinition.SSL_SOCKET_BINDING.resolveModelAttribute(context, resourceModel);

        if(!socketBinding.isDefined() && !sslSocketBinding.isDefined()){
            throw IIOPLogger.ROOT_LOGGER.noSocketBindingsConfigured();
        }
    }

    private static void validateSSLConfig(final boolean supportSSL, final boolean serverSSLConfigured, final boolean clientSSLConfigured,
                                          final boolean serverRequiresSSL, final boolean clientRequiresSSL) throws OperationFailedException {
        if (supportSSL) {
            if (!(clientSSLConfigured || serverSSLConfigured)) {
                throw IIOPLogger.ROOT_LOGGER.noSSLContextsSpecified();
            }
        }

        if (serverRequiresSSL && !serverSSLConfigured) {
            throw IIOPLogger.ROOT_LOGGER.serverSSLNotConfigured();
        }
        if (clientRequiresSSL && !clientSSLConfigured) {
            throw IIOPLogger.ROOT_LOGGER.clientSSLNotConfigured();
        }
    }

    private static void validateSSLSocketBinding(final OperationContext context, final ModelNode resourceModel, final boolean serverSSLConfigured, final boolean clientSSLConfigured, final List<String> warnings) throws OperationFailedException{
        ModelNode sslSocketBinding = IIOPRootDefinition.SSL_SOCKET_BINDING.resolveModelAttribute(context, resourceModel);
        if (sslSocketBinding.isDefined()) {
            if (!serverSSLConfigured) {
                warnings.add(IIOPLogger.ROOT_LOGGER.serverSSLPortWithoutSslConfiguration());
            }
            if (!clientSSLConfigured) {
                warnings.add(IIOPLogger.ROOT_LOGGER.clientSSLPortWithoutSslConfiguration());
            }
        }
    }

    private static void validateIORTransportConfig(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured,
                                                   final boolean serverRequiresSsl, final List<String> warnings) throws OperationFailedException {
        validateSSLAttribute(context, resourceModel, sslConfigured, serverRequiresSsl, IIOPRootDefinition.INTEGRITY, warnings);
        validateSSLAttribute(context, resourceModel, sslConfigured, serverRequiresSsl, IIOPRootDefinition.CONFIDENTIALITY, warnings);
        validateSSLAttribute(context, resourceModel, sslConfigured, serverRequiresSsl, IIOPRootDefinition.TRUST_IN_CLIENT, warnings);
        validateTrustInTarget(context, resourceModel, sslConfigured, warnings);
        validateSupportedAttribute(context, resourceModel, IIOPRootDefinition.DETECT_MISORDERING, warnings);
        validateSupportedAttribute(context, resourceModel, IIOPRootDefinition.DETECT_REPLAY, warnings);
    }

    private static void validateSSLAttribute(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured, final boolean serverRequiresSsl, final AttributeDefinition attributeDefinition, final List<String> warnings) throws OperationFailedException {
        final ModelNode attributeNode = attributeDefinition.resolveModelAttribute(context, resourceModel);
        if(attributeNode.isDefined()){
            final String attribute = attributeNode.asString();
            if(sslConfigured) {
                if(attribute.equals(Constants.IOR_NONE)){
                    final String warning = IIOPLogger.ROOT_LOGGER.inconsistentSupportedTransportConfig(attributeDefinition.getName(), serverRequiresSsl ? Constants.IOR_REQUIRED : Constants.IOR_SUPPORTED);
                    IIOPLogger.ROOT_LOGGER.warn(warning);
                    warnings.add(warning);
                }
                if (serverRequiresSsl && attribute.equals(Constants.IOR_SUPPORTED)) {
                    final String warning = IIOPLogger.ROOT_LOGGER.inconsistentRequiredTransportConfig(Constants.SECURITY_SERVER_REQUIRES_SSL, attributeDefinition.getName());
                    IIOPLogger.ROOT_LOGGER.warn(warning);
                    warnings.add(warning);
                }
            } else {
                if(!attribute.equals(Constants.IOR_NONE)){
                    final String warning = IIOPLogger.ROOT_LOGGER.inconsistentUnsupportedTransportConfig(attributeDefinition.getName());
                    IIOPLogger.ROOT_LOGGER.warn(warning);
                    warnings.add(warning);
                }
            }
        }
    }

    private static void validateTrustInTarget(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured, final List<String> warnings) throws OperationFailedException {
        final ModelNode establishTrustInTargetNode = IIOPRootDefinition.TRUST_IN_TARGET.resolveModelAttribute(context, resourceModel);
        if(establishTrustInTargetNode.isDefined()){
            final String establishTrustInTarget = establishTrustInTargetNode.asString();
            if(sslConfigured && establishTrustInTarget.equals(Constants.IOR_NONE)){
                final String warning = IIOPLogger.ROOT_LOGGER.inconsistentSupportedTransportConfig(Constants.IOR_TRANSPORT_TRUST_IN_TARGET, Constants.IOR_SUPPORTED);
                IIOPLogger.ROOT_LOGGER.warn(warning);
                warnings.add(warning);
            }
        }
    }

    private static void validateSupportedAttribute(final OperationContext context, final ModelNode resourceModel, final AttributeDefinition attributeDefinition, final List<String> warnings) throws OperationFailedException{
        final ModelNode attributeNode = attributeDefinition.resolveModelAttribute(context, resourceModel);
        if(attributeNode.isDefined() && !attributeNode.asString().equals(Constants.IOR_SUPPORTED)) {
            final String warning = IIOPLogger.ROOT_LOGGER.inconsistentSupportedTransportConfig(attributeDefinition.getName(), Constants.IOR_SUPPORTED);
            IIOPLogger.ROOT_LOGGER.warn(warning);
            warnings.add(warning);
        }
    }

    private static void validateORBInitializerConfig(final OperationContext context, final ModelNode resourceModel) throws OperationFailedException {
        // validate the elytron initializer configuration: it requires an authentication-context name.
        final ModelNode securityInitializerNode = IIOPRootDefinition.SECURITY.resolveModelAttribute(context, resourceModel);
        final ModelNode authContextNode = IIOPRootDefinition.AUTHENTICATION_CONTEXT.resolveModelAttribute(context, resourceModel);
        if ((!securityInitializerNode.isDefined()
                || !securityInitializerNode.asString().equalsIgnoreCase(Constants.ELYTRON))
                && authContextNode.isDefined()) {
            // authentication-context has been specified but is ineffective because the security initializer is not set to
            // 'elytron'.
            throw IIOPLogger.ROOT_LOGGER.ineffectiveAuthenticationContextConfiguration();
        }
    }
}
