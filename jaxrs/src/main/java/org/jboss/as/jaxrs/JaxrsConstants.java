/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;

public class JaxrsConstants {

    public static final String JAXRS_2_0_REQUEST_MATCHING = "jaxrs-2-0-request-matching";
    public static final String RESTEASY_ADD_CHARSET = "resteasy-add-charset";
    public static final String RESTEASY_BUFFER_EXCEPTION_ENTITY = "resteasy-buffer-exception-entity";
    public static final String RESTEASY_DISABLE_HTML_SANITIZER = "resteasy-disable-html-sanitizer";
    public static final String RESTEASY_DISABLE_PROVIDERS = "resteasy-disable-providers";
    public static final String RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES = "resteasy-document-expand-entity-references";
    public static final String RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS = "resteasy-document-secure-disableDTDs";
    public static final String RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE = "resteasy-document-secure-processing-feature";
    public static final String RESTEASY_GZIP_MAX_INPUT = "resteasy-gzip-max-input";
    public static final String RESTEASY_JNDI_RESOURCES = "resteasy-jndi-resources";
    public static final String RESTEASY_LANGUAGE_MAPPINGS = "resteasy-language-mappings";
    public static final String RESTEASY_MEDIA_TYPE_MAPPINGS = "resteasy-media-type-mappings";
    public static final String RESTEASY_MEDIA_TYPE_PARAM_MAPPING = "resteasy-media-type-param-mapping";
    public static final String RESTEASY_PREFER_JACKSON_OVER_JSONB = "resteasy-prefer-jackson-over-jsonb";
    public static final String RESTEASY_PROVIDERS = "resteasy-providers";
    public static final String RESTEASY_RFC7232_PRECONDITIONS = "resteasy-rfc7232preconditions";
    public static final String RESTEASY_ROLE_BASED_SECURITY = "resteasy-role-based-security";
    public static final String RESTEASY_SECURE_RANDOM_MAX_USE = "resteasy-secure-random-max-use";
    public static final String RESTEASY_USE_BUILTIN_PROVIDERS = "resteasy-use-builtin-providers";
    public static final String RESTEASY_USE_CONTAINER_FORM_PARAMS = "resteasy-use-container-form-params";
    public static final String RESTEASY_WIDER_REQUEST_MATCHING = "resteasy-wider-request-matching";

    public static final Map<String, AttributeDefinition> nameToAttributeMap = new HashMap<String,AttributeDefinition> ();
    static {
        nameToAttributeMap.put(JAXRS_2_0_REQUEST_MATCHING, JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING);
        nameToAttributeMap.put(RESTEASY_ADD_CHARSET, JaxrsAttribute.RESTEASY_ADD_CHARSET);
        nameToAttributeMap.put(RESTEASY_BUFFER_EXCEPTION_ENTITY, JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY);
        nameToAttributeMap.put(RESTEASY_DISABLE_HTML_SANITIZER, JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER);
        nameToAttributeMap.put(RESTEASY_DISABLE_PROVIDERS, JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS);
        nameToAttributeMap.put(RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES);
        nameToAttributeMap.put(RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS);
        nameToAttributeMap.put(RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE);
        nameToAttributeMap.put(RESTEASY_GZIP_MAX_INPUT, JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT);
        nameToAttributeMap.put(RESTEASY_JNDI_RESOURCES, JaxrsAttribute.RESTEASY_JNDI_RESOURCES);
        nameToAttributeMap.put(RESTEASY_LANGUAGE_MAPPINGS, JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS);
        nameToAttributeMap.put(RESTEASY_MEDIA_TYPE_MAPPINGS, JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS);
        nameToAttributeMap.put(RESTEASY_MEDIA_TYPE_PARAM_MAPPING, JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING);
        nameToAttributeMap.put(RESTEASY_PREFER_JACKSON_OVER_JSONB, JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB);
        nameToAttributeMap.put(RESTEASY_PROVIDERS, JaxrsAttribute.RESTEASY_PROVIDERS);
        nameToAttributeMap.put(RESTEASY_RFC7232_PRECONDITIONS, JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS);
        nameToAttributeMap.put(RESTEASY_ROLE_BASED_SECURITY, JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY);
        nameToAttributeMap.put(RESTEASY_SECURE_RANDOM_MAX_USE, JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE);
        nameToAttributeMap.put(RESTEASY_USE_BUILTIN_PROVIDERS, JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS);
        nameToAttributeMap.put(RESTEASY_USE_CONTAINER_FORM_PARAMS, JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS);
        nameToAttributeMap.put(RESTEASY_WIDER_REQUEST_MATCHING, JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING);
    }
}
