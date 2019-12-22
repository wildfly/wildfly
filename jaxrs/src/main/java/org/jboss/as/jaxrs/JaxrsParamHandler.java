/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jaxrs;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * An AbstractWriteAttributeHandler extension for updating basic WS server config attributes
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
final class JaxrsParamHandler extends AbstractWriteAttributeHandler<Void> {

    public JaxrsParamHandler(final AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder)
                    throws OperationFailedException {

        //If the required value is the current one, we do not need to do anything.
        if (!isSameValue(context, resolvedValue, currentValue, attributeName)) {
            final String value = resolvedValue.isDefined() ? resolvedValue.asString() : null;
            updateServerConfig(context, attributeName, value, false);
        }
        return false;
    }

    private boolean isSameValue(OperationContext context, ModelNode resolvedValue, ModelNode currentValue, String attributeName)
            throws OperationFailedException {
        if (resolvedValue.equals(getAttributeDefinition(attributeName).resolveValue(context, currentValue))) {
            return true;
        }
        if (!currentValue.isDefined()) {
            return resolvedValue.equals(getAttributeDefinition(attributeName).getDefaultValue());
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final String value = valueToRestore.isDefined() ? valueToRestore.asString() : null;
        updateServerConfig(null, attributeName, value, true);
    }

    /**
     * Returns true if the update operation succeeds in modifying the runtime, false otherwise.
     * @param context TODO
     * @param attributeName
     * @param value
     */
    private void updateServerConfig(OperationContext context, String attributeName, String value, boolean isRevert) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceName name = JaxrsServerConfigService.CONFIG_SERVICE;
        @SuppressWarnings("deprecation")
        JaxrsServerConfig config = (JaxrsServerConfig) registry.getRequiredService(name).getValue();

        if (JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING.equals(attributeName)) {
            config.setJaxrs20RequestMatching(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_ADD_CHARSET.equals(attributeName)) {
            config.setResteasyAddCharset(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY.equals(attributeName)) {
            config.setResteasyBufferExceptionEntity(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER.equals(attributeName)) {
            config.setResteasyDisableHtmlSanitizer(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_DISABLE_PROVIDERS.equals(attributeName)) {
            config.setResteasyDisableProviders(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES.equals(attributeName)) {
            config.setResteasyDocumentExpandEntityReferences(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS.equals(attributeName)) {
            config.setResteasySecureDisableDTDs(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE.equals(attributeName)) {
            config.setResteasyDocumentSecureProcessingFeature(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_GZIP_MAX_INPUT.equals(attributeName)) {
            config.setResteasyGzipMaxInput(new ModelNode(Integer.parseInt(value)));
        } else if (JaxrsConstants.RESTEASY_JNDI_RESOURCES.equals(attributeName)) {
            config.setResteasyJndiResources(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS.equals(attributeName)) {
            config.setResteasyLanguageMappings(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS.equals(attributeName)) {
            config.setResteasyMediaTypeMappings(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING.equals(attributeName)) {
            config.setResteasyMediaTypeParamMapping(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_PROVIDERS.equals(attributeName)) {
            config.setResteasyProviders(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_RESOURCES.equals(attributeName)) {
            config.setResteasyResources(new ModelNode(value));
        } else if (JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS.equals(attributeName)) {
            config.setResteasyRFC7232Preconditions(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY.equals(attributeName)) {
            config.setResteasyRoleBasedSecurity(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE.equals(attributeName)) {
            config.setResteasySecureRandomMaxUse(new ModelNode(Integer.parseInt(value)));
        } else if (JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS.equals(attributeName)) {
            config.setResteasyUseBuiltinProviders(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS.equals(attributeName)) {
            config.setResteasyUseContainerFormParams(new ModelNode(Boolean.parseBoolean(value)));
        } else if (JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING.equals(attributeName)) {
            config.setResteasyWiderRequestMatching(new ModelNode(Boolean.parseBoolean(value)));
        }
    }
}
