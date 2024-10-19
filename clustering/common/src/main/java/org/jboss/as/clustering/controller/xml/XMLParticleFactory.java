/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import javax.xml.namespace.QName;

import org.jboss.as.controller.FeatureFilter;
import org.jboss.as.version.Stability;

/**
 * A factory for creating builders of XML particles.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer context
 */
public interface XMLParticleFactory<RC, WC> {
    /**
     * Creates a new factory instance for creating XML particles.
     * @param <RC> the reader context
     * @param <WC> the writer context
     * @param filter a feature filter
     * @return a new factory instance for creating XML particles.
     */
    static <RC, WC> XMLParticleFactory<RC, WC> newInstance(FeatureFilter filter) {
        return new DefaultXMLParticleFactory<>(filter);
    }

    /**
     * Returns a builder of an XML element, using the specified name.
     * @param name the element name
     * @return a builder of an XML element.
     */
    XMLElement.Builder<RC, WC> element(QName name);

    /**
     * Returns a builder of an XML choice.
     * @return a builder of an XML choice.
     */
    XMLChoice.Builder<RC, WC> choice();

    /**
     * Returns a builder of XML content.
     * @return a builder of XML content.
     */
    XMLAll.Builder<RC, WC> all();

    /**
     * Returns a builder of XML content.
     * @return a builder of XML content.
     */
    XMLSequence.Builder<RC, WC> sequence();

    class DefaultXMLParticleFactory<RC, WC> implements XMLParticleFactory<RC, WC> {
        private final FeatureFilter filter;

        DefaultXMLParticleFactory(FeatureFilter filter) {
            this.filter = filter;
        }

        @Override
        public XMLElement.Builder<RC, WC> element(QName name) {
            return new XMLElement.DefaultBuilder<>(name, Stability.DEFAULT);
        }

        @Override
        public XMLChoice.Builder<RC, WC> choice() {
            return new XMLChoice.DefaultBuilder<>(this.filter);
        }

        @Override
        public XMLAll.Builder<RC, WC> all() {
            return new XMLAll.DefaultBuilder<>(this.filter);
        }

        @Override
        public XMLSequence.Builder<RC, WC> sequence() {
            return new XMLSequence.DefaultBuilder<>(this.filter);
        }
    }
}
