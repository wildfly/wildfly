/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Encapsulates a group of XML particles using xs:choice (i.e. one of) semantics.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLChoice<RC, WC> extends XMLParticleGroup<RC, WC> {

    interface Builder<RC, WC> extends XMLParticleGroup.Builder<RC, WC, XMLElement<RC, WC>, XMLChoice<RC, WC>, Builder<RC, WC>> {
    }

    class DefaultBuilder<RC, WC> extends XMLParticleGroup.AbstractBuilder<RC, WC, XMLElement<RC, WC>, XMLChoice<RC, WC>, Builder<RC, WC>> implements Builder<RC, WC> {
        DefaultBuilder(FeatureRegistry registry) {
            super(registry);
        }

        @Override
        public XMLChoice<RC, WC> build() {
            return new DefaultXMLChoice<>(this.getGroups(), this.getCardinality());
        }

        @Override
        protected Builder<RC, WC> builder() {
            return this;
        }
    }

    class DefaultXMLChoice<RC, WC> extends DefaultXMLParticleGroup<RC, WC> implements XMLChoice<RC, WC> {
        private static <RC, WC> Map<QName, XMLParticleGroup<RC, WC>> collect(Collection<? extends XMLParticleGroup<RC, WC>> groups) {
            Map<QName, XMLParticleGroup<RC, WC>> result = new TreeMap<>(QNameResolver.COMPARATOR);
            for (XMLParticleGroup<RC, WC> group : groups) {
                for (QName name : group.getReaderNames()) {
                    if (result.put(name, group) != null) {
                        throw ClusteringLogger.ROOT_LOGGER.duplicateElements(name);
                    }
                }
            }
            return Collections.unmodifiableMap(result);
        }

        DefaultXMLChoice(XMLParticleGroup<RC, WC> group) {
            // Singleton choice
            this(group.getReaderNames().stream().collect(Collectors.toMap(Function.identity(), name -> group)), List.of(group), group.getCardinality());
        }

        protected DefaultXMLChoice(Collection<? extends XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            this(collect(groups), Collections.unmodifiableCollection(groups), cardinality);
        }

        private DefaultXMLChoice(Map<QName, XMLParticleGroup<RC, WC>> choices, Collection<XMLParticleGroup<RC, WC>> groups, XMLCardinality cardinality) {
            super(choices.keySet(), groups, cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    Assert.assertTrue(reader.isStartElement());
                    QName name = reader.getName();
                    XMLParticleGroup<RC, WC> choice = choices.get(name);
                    if (choice != null) {
                        if (!cardinality.isEnabled()) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        int occurrences = 0;
                        int maxOccurs = choice.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
                        do {
                            occurrences += 1;
                            choice.getReader().readElement(reader, context);
                            // Read any additional occurrences of this choice
                        } while ((reader.getEventType() != XMLStreamConstants.END_ELEMENT) && (occurrences < maxOccurs) && reader.getName().equals(name));
                        // Validate minOccurs
                        if (occurrences < choice.getCardinality().getMinOccurs()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, choice.getNames(), choice.getCardinality());
                        }
                    } else if (cardinality.isRequired()) {
                        throw ParseUtils.unexpectedElement(reader, choices.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    } else {
                        this.handleAbsentElement(context);
                    }
                }

                @Override
                public void handleAbsentElement(RC context) {
                    for (XMLParticleGroup<RC, WC> choice : groups) {
                        choice.getReader().handleAbsentElement(context);
                    }
                }
            });
        }
    }

    class DefaultXMLElementChoice<RC, WC> extends DefaultXMLParticleGroup<RC, WC> implements XMLChoice<RC, WC> {
        private static <RC, WC> Map<QName, XMLElement<RC, WC>> collect(Collection<? extends XMLElement<RC, WC>> elements) {
            Map<QName, XMLElement<RC, WC>> result = new TreeMap<>(QNameResolver.COMPARATOR);
            for (XMLElement<RC, WC> element : elements) {
                if (result.put(element.getName(), element) != null) {
                    throw ClusteringLogger.ROOT_LOGGER.duplicateElements(element.getName());
                }
            }
            return Collections.unmodifiableMap(result);
        }

        // Singleton choice
        DefaultXMLElementChoice(XMLElement<RC, WC> element) {
            this(Map.of(element.getName(), element), element.getCardinality(), element.getReader()::handleAbsentElement, List.of(element), element.getStability());
        }

        protected DefaultXMLElementChoice(Collection<? extends XMLElement<RC, WC>> elements, XMLCardinality cardinality, Consumer<RC> absenteeHandler, Stability stability) {
            this(collect(elements), cardinality, absenteeHandler, Collections.unmodifiableCollection(elements), stability);
        }

        private DefaultXMLElementChoice(Map<QName, XMLElement<RC, WC>> choices, XMLCardinality cardinality, Consumer<RC> absenteeHandler, Collection<XMLElement<RC, WC>> elements, Stability stability) {
            this(choices.keySet(), cardinality, new ExceptionFunction<>() {
                @Override
                public XMLElement<RC, WC> apply(XMLExtendedStreamReader reader) throws XMLStreamException {
                    return choices.get(reader.getName());
                }
            }, absenteeHandler, XMLContentWriter.composite(elements), stability);
        }

        protected DefaultXMLElementChoice(Set<QName> names, XMLCardinality cardinality, ExceptionFunction<XMLExtendedStreamReader, XMLElement<RC, WC>, XMLStreamException> elementReader, Consumer<RC> absenteeHandler, XMLContentWriter<WC> writer, Stability stability) {
            super(names, cardinality, new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, RC context) throws XMLStreamException {
                    Assert.assertTrue(reader.isStartElement());
                    XMLElement<RC, WC> element = elementReader.apply(reader);
                    if (element != null) {
                        if (!cardinality.isEnabled()) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        int occurrences = 0;
                        int maxOccurs = element.getCardinality().getMaxOccurs().orElse(Integer.MAX_VALUE);
                        do {
                            occurrences += 1;
                            element.getReader().readElement(reader, context);
                            // Read any additional occurrences of this element
                        } while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT) && (occurrences < maxOccurs) && (elementReader.apply(reader) == element));
                        // Validate minOccurs
                        if (occurrences < element.getCardinality().getMinOccurs()) {
                            throw ClusteringLogger.ROOT_LOGGER.minOccursNotReached(reader, Set.of(element.getName()), element.getCardinality());
                        }
                    } else if (cardinality.isRequired()) {
                        throw ParseUtils.unexpectedElement(reader, names.stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                    } else {
                        this.handleAbsentElement(context);
                    }
                }

                @Override
                public void handleAbsentElement(RC context) {
                    absenteeHandler.accept(context);
                }
            }, writer, stability);
        }
    }
}
