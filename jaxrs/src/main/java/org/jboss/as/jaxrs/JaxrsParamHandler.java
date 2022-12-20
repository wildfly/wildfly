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

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

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

        final JaxrsElement attribute = JaxrsElement.forName(attributeName);
        switch (attribute) {
            case JAXRS_2_0_REQUEST_MATCHING:
                config.setJaxrs20RequestMatching(value);
                break;

            case RESTEASY_ADD_CHARSET:
                config.setResteasyAddCharset(value);
                break;

            case RESTEASY_BUFFER_EXCEPTION_ENTITY:
                config.setResteasyBufferExceptionEntity(value);
                break;

            case RESTEASY_DISABLE_HTML_SANITIZER:
                config.setResteasyDisableHtmlSanitizer(value);
                break;

            case RESTEASY_DISABLE_PROVIDERS:
                config.setResteasyDisableProviders(value);
                break;

            case RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES:
                config.setResteasyDocumentExpandEntityReferences(value);
                break;

            case RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS:
                config.setResteasySecureDisableDTDs(value);
                break;

            case RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE:
                config.setResteasyDocumentSecureProcessingFeature(value);
                break;

            case RESTEASY_GZIP_MAX_INPUT:
                config.setResteasyGzipMaxInput(value);
                break;

            case RESTEASY_JNDI_RESOURCES:
                config.setResteasyJndiResources(value);
                break;

            case RESTEASY_LANGUAGE_MAPPINGS:
                config.setResteasyLanguageMappings(value);
                break;

            case RESTEASY_MATCH_CACHE_ENABLED:
                config.setResteasyMatchCacheEnabled(value);
                break;

            case RESTEASY_MATCH_CACHE_SIZE:
                config.setResteasyMatchCacheSize(value);
                break;

            case RESTEASY_MEDIA_TYPE_MAPPINGS:
                config.setResteasyMediaTypeMappings(value);
                break;

            case RESTEASY_MEDIA_TYPE_PARAM_MAPPING:
                config.setResteasyMediaTypeParamMapping(value);
                break;

            case RESTEASY_PATCH_FILTER_DISABLED:
                config.setResteasyPatchfilterDisabled(value);
                break;

            case RESTEASY_PATCH_FILTER_LEGACY:
                config.setResteasyPatchfilterLegacy(value);
                break;

            case RESTEASY_PREFER_JACKSON_OVER_JSONB:
                config.setResteasyPreferJacksonOverJsonB(value);
                break;

            case RESTEASY_PROVIDERS:
                config.setResteasyProviders(value);
                break;

            case RESTEASY_RFC7232_PRECONDITIONS:
                config.setResteasyRFC7232Preconditions(value);
                break;

            case RESTEASY_ROLE_BASED_SECURITY:
                config.setResteasyRoleBasedSecurity(value);
                break;

            case RESTEASY_SECURE_RANDOM_MAX_USE:
                config.setResteasySecureRandomMaxUse(value);
                break;

            case RESTEASY_USE_BUILTIN_PROVIDERS:
                config.setResteasyUseBuiltinProviders(value);
                break;

            case RESTEASY_USE_CONTAINER_FORM_PARAMS:
                config.setResteasyUseContainerFormParams(value);
                break;

            case RESTEASY_WIDER_REQUEST_MATCHING:
                config.setResteasyWiderRequestMatching(value);
                break;

            default:
                JAXRS_LOGGER.debug("Unknown parameter: " + attribute.getLocalName());
        }
    }
}
