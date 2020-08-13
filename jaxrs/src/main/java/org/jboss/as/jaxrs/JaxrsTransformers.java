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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;

/**
 * Jaxrs transformers.
 *
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class JaxrsTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return JaxrsExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_2_0_0(subsystemRegistration);
    }

    private static void registerTransformers_2_0_0(SubsystemTransformerRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = ResourceTransformationDescriptionBuilder.Factory.createSubsystemInstance();
        AttributeTransformationDescriptionBuilder attributeBuilder = builder.getAttributeBuilder();
        checkAttribute(attributeBuilder, JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_ADD_CHARSET);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_JNDI_RESOURCES);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_PROVIDERS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS);
        checkAttribute(attributeBuilder, JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING);
        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, JaxrsExtension.MODEL_VERSION_1_0_0);
    }

    private static void checkAttribute(AttributeTransformationDescriptionBuilder builder, AttributeDefinition attribute) {
        builder.setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, attribute)
               .addRejectCheck(RejectAttributeChecker.DEFINED, attribute);
    }
}
