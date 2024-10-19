/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.function.Predicate;

import org.jboss.as.controller.Feature;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Encapsulates an XML particle.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLParticle<RC, WC> extends XMLContentWriter.Provider<WC>, Feature {

    XMLCardinality getCardinality();

    XMLElementReader<RC> getReader();

    interface Builder<RC, WC, T extends XMLParticle<RC, WC>, B extends Builder<RC, WC, T, B>> {
        B withCardinality(XMLCardinality cardinality);

        T build();
    }

    abstract class AbstractBuilder<RC, WC, T extends XMLParticle<RC, WC>, B extends Builder<RC, WC, T, B>> implements Builder<RC, WC, T, B> {
        private static final Predicate<XMLCardinality> DEFAULT_VALIDATOR = new Predicate<>() {
            @Override
            public boolean test(XMLCardinality cardinality) {
                return (cardinality.getMinOccurs() >= 0) && (cardinality.getMinOccurs() <= cardinality.getMaxOccurs().orElse(Integer.MAX_VALUE));
            }
        };
        private volatile XMLCardinality cardinality = XMLCardinality.Single.REQUIRED;

        protected final Predicate<XMLCardinality> validator;

        protected AbstractBuilder() {
            this.validator = DEFAULT_VALIDATOR;
        }

        protected AbstractBuilder(Predicate<XMLCardinality> validator) {
            this.validator = DEFAULT_VALIDATOR.and(validator);
        }

        @Override
        public B withCardinality(XMLCardinality cardinality) {
            if (!this.validator.test(cardinality)) {
                throw new IllegalArgumentException(cardinality.toString());
            }
            this.cardinality = cardinality;
            return this.builder();
        }

        protected XMLCardinality getCardinality() {
            return this.cardinality;
        }

        protected abstract B builder();
    }

    class DefaultXMLParticle<RC, WC> implements XMLParticle<RC, WC> {
        private final XMLCardinality cardinality;
        private final XMLElementReader<RC> reader;
        private final XMLContentWriter<WC> writer;
        private final Stability stability;

        protected DefaultXMLParticle(XMLCardinality cardinality, XMLElementReader<RC> reader, XMLContentWriter<WC> writer, Stability stability) {
            this.cardinality = cardinality;
            this.reader = reader;
            this.writer = writer;
            this.stability = stability;
        }

        @Override
        public XMLCardinality getCardinality() {
            return this.cardinality;
        }

        @Override
        public XMLElementReader<RC> getReader() {
            return this.reader;
        }

        @Override
        public XMLContentWriter<WC> getWriter() {
            return this.writer;
        }

        @Override
        public Stability getStability() {
            return this.stability;
        }
    }
}
