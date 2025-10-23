/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs;

import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
enum JaxrsSubsystemSchema implements PersistentSubsystemSchema<JaxrsSubsystemSchema> {
    VERSION_1_0_0(1, 0, Stability.DEFAULT),
    @SuppressWarnings("deprecation")
    VERSION_2_0_0(2, 0, Stability.DEFAULT,
            JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING,
            JaxrsAttribute.RESTEASY_ADD_CHARSET,
            JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY,
            JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER,
            JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS,
            JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES,
            JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS,
            JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE,
            JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT,
            JaxrsAttribute.RESTEASY_JNDI_RESOURCES,
            JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS,
            JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS,
            JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING,
            JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB,
            JaxrsAttribute.RESTEASY_PROVIDERS,
            JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS,
            JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY,
            JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE,
            JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS,
            JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS,
            JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING),
    @SuppressWarnings("deprecation")
    VERSION_3_0_0(3, 0, Stability.DEFAULT,
            VERSION_2_0_0,
            JaxrsAttribute.TRACING_TYPE,
            JaxrsAttribute.TRACING_THRESHOLD),
    VERSION_4_0_0(4, 0, Stability.DEFAULT, VERSION_3_0_0, JaxrsAttribute.RESTEASY_PATCHFILTER_DISABLED),
    VERSION_5_0_0(5, 0, Stability.DEFAULT, VERSION_4_0_0, JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR),
    ;

    static final JaxrsSubsystemSchema CURRENT = VERSION_5_0_0;

    private final VersionedNamespace<IntVersion, JaxrsSubsystemSchema> namespace;
    private final AttributeDefinition[] attributes;

    JaxrsSubsystemSchema(final int major, final int minor, final Stability stability, final AttributeDefinition... attributes) {
        namespace = SubsystemSchema.createLegacySubsystemURN(JaxrsExtension.SUBSYSTEM_NAME, stability, new IntVersion(major, minor));
        this.attributes = attributes;
    }

    JaxrsSubsystemSchema(final int major, final int minor, final Stability stability, final JaxrsSubsystemSchema parent, final AttributeDefinition... attributes) {
        namespace = SubsystemSchema.createLegacySubsystemURN(JaxrsExtension.SUBSYSTEM_NAME, stability, new IntVersion(major, minor));
        this.attributes = Stream.concat(Stream.of(parent.attributes), Stream.of(attributes)).toArray(AttributeDefinition[]::new);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PersistentResourceXMLDescription getXMLDescription() {
        return PersistentResourceXMLDescription.builder(JaxrsExtension.SUBSYSTEM_PATH, namespace)
                .addAttributes(attributes)
                .setUseElementsForGroups(false)
                .build();
    }

    @Override
    public VersionedNamespace<IntVersion, JaxrsSubsystemSchema> getNamespace() {
        return namespace;
    }
}
