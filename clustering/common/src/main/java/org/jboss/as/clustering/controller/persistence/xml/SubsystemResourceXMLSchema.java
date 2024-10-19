/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Feature;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * A {@link org.jboss.as.controller.SubsystemSchema} described by a {@link SubsystemResourceRegistrationXMLElement}.
 * @author Paul Ferraro
 */
public interface SubsystemResourceXMLSchema<S extends SubsystemResourceXMLSchema<S>> extends SubsystemSchema<S> {

    SubsystemResourceRegistrationXMLElement getSubsystemXMLElement();

    @Override
    default void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        new SubsystemResourceXMLElementReader(this.getSubsystemXMLElement()).readElement(reader, operations);
    }

    /**
     * Creates the subsystem persistence configuration for the current version of the schema.
     * Ported from {@link SubsystemPersistence} from WFCORE-6779.
     * @param <S> the schema type
     * @param currentSchema the current schema version
     * @return a subsystem persistence configuration
     */
    static <S extends Enum<S> & SubsystemResourceXMLSchema<S>> SubsystemPersistence<S> persistence(S currentSchema) {
        return persistence(EnumSet.of(currentSchema));
    }

    /**
     * Creates the subsystem persistence configuration for the current versions of the schema.
     * Ported from {@link SubsystemPersistence} from WFCORE-6779.
     * @param <S> the schema type
     * @param currentSchemas the current schema versions
     * @return a subsystem persistence configuration
     */
    static <S extends Enum<S> & SubsystemResourceXMLSchema<S>> SubsystemPersistence<S> persistence(Set<S> currentSchemas) {
        Assert.assertFalse(currentSchemas.isEmpty());
        Class<S> schemaClass = currentSchemas.iterator().next().getDeclaringClass();
        // Build SubsystemResourceRegistrationXMLElement for current schemas to share between reader and writer.
        Map<S, SubsystemResourceRegistrationXMLElement> elements = new EnumMap<>(schemaClass);
        for (S currentSchema : currentSchemas) {
            elements.put(currentSchema, currentSchema.getSubsystemXMLElement());
        }
        Map<Stability, S> currentSchemaPerStability = Feature.map(currentSchemas);
        return new SubsystemPersistence<>() {
            @Override
            public Set<S> getSchemas() {
                return EnumSet.allOf(schemaClass);
            }

            @Override
            public XMLElementReader<List<ModelNode>> getReader(S schema) {
                return Optional.ofNullable(elements.get(schema)).<XMLElementReader<List<ModelNode>>>map(SubsystemResourceXMLElementReader::new).orElse(schema);
            }

            @Override
            public XMLElementWriter<SubsystemMarshallingContext> getWriter(Stability stability) {
                S currentSchema = currentSchemaPerStability.get(stability);
                return new SubsystemResourceXMLElementWriter(elements.get(currentSchema));
            }
        };
    }
}
