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
 * An AbstractWriteAttributeHandler extension for updating basic RESTEasy config attributes
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
            updateServerConfig(context, attributeName, resolvedValue, false);
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
        updateServerConfig(null, attributeName, valueToRestore, true);
    }

    /**
     * Returns true if the update operation succeeds in modifying the runtime, false otherwise.
     * @param context TODO
     * @param attributeName
     * @param value
     */
    private void updateServerConfig(OperationContext context, String attributeName, ModelNode value, boolean isRevert) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceName name = JaxrsServerConfigService.CONFIG_SERVICE;
        @SuppressWarnings("deprecation")
        JaxrsServerConfig config = (JaxrsServerConfig) registry.getRequiredService(name).getValue();

        if (JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING.equals(attributeName)) {
            config.setJaxrs20RequestMatching(value);
        } else if (JaxrsConstants.RESTEASY_ADD_CHARSET.equals(attributeName)) {
            config.setResteasyAddCharset(value);
        } else if (JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY.equals(attributeName)) {
            config.setResteasyBufferExceptionEntity(value);
        } else if (JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER.equals(attributeName)) {
            config.setResteasyDisableHtmlSanitizer(value);
        } else if (JaxrsConstants.RESTEASY_DISABLE_PROVIDERS.equals(attributeName)) {
            config.setResteasyDisableProviders(value);
        } else if (JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES.equals(attributeName)) {
            config.setResteasyDocumentExpandEntityReferences(value);
        } else if (JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS.equals(attributeName)) {
            config.setResteasySecureDisableDTDs(value);
        } else if (JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE.equals(attributeName)) {
            config.setResteasyDocumentSecureProcessingFeature(value);
        } else if (JaxrsConstants.RESTEASY_GZIP_MAX_INPUT.equals(attributeName)) {
            config.setResteasyGzipMaxInput(value);
        } else if (JaxrsConstants.RESTEASY_JNDI_RESOURCES.equals(attributeName)) {
            config.setResteasyJndiResources(value);
        } else if (JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS.equals(attributeName)) {
            config.setResteasyLanguageMappings(value);
        } else if (JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS.equals(attributeName)) {
            config.setResteasyMediaTypeMappings(value);
        } else if (JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING.equals(attributeName)) {
            config.setResteasyMediaTypeParamMapping(value);
        } else if (JaxrsConstants.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR.equals(attributeName)) {
           config.setResteasyOriginalWebApplicationExceptionBehavior(value);
        } else if (JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB.equals(attributeName)) {
            config.setResteasyPreferJacksonOverJsonB(value);
        } else if (JaxrsConstants.RESTEASY_PROVIDERS.equals(attributeName)) {
            config.setResteasyProviders(value);
        } else if (JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS.equals(attributeName)) {
            config.setResteasyRFC7232Preconditions(value);
        } else if (JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY.equals(attributeName)) {
            config.setResteasyRoleBasedSecurity(value);
        } else if (JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE.equals(attributeName)) {
            config.setResteasySecureRandomMaxUse(value);
        } else if (JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS.equals(attributeName)) {
            config.setResteasyUseBuiltinProviders(value);
        } else if (JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS.equals(attributeName)) {
            config.setResteasyUseContainerFormParams(value);
        } else if (JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING.equals(attributeName)) {
            config.setResteasyWiderRequestMatching(value);
        }
    }
}
