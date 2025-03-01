/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

/**
 * Encapsulates an XML particle with content.
 * @author Paul Ferraro
 * @param <RC> the reader context
 * @param <WC> the writer content
 */
public interface XMLContainer<RC, WC> extends XMLParticle<RC, WC> {

    /**
     * Builder of an XML container.
     * @param <RC> the reader context
     * @param <WC> the writer content
     */
    interface Builder<RC, WC, T extends XMLContainer<RC, WC>, B extends Builder<RC, WC, T, B>> extends XMLParticle.Builder<RC, WC, T, B> {
        /**
         * Applies the specified content to this container
         * @param content XML content
         * @return a reference to this builder
         */
        default B withContent(XMLSequence<RC, WC> sequence) {
            return this.withContent(XMLContent.of(sequence));
        }

        /**
         * Applies the specified content to this container
         * @param content XML content
         * @return a reference to this builder
         */
        default B withContent(XMLAll<RC, WC> all) {
            return this.withContent(XMLContent.of(all));
        }

        /**
         * Applies the specified content to this container
         * @param content XML content
         * @return a reference to this builder
         */
        default B withContent(XMLChoice<RC, WC> choice) {
            return this.withContent(XMLContent.of(choice));
        }

        /**
         * Applies the specified content to this container
         * @param content XML content
         * @return a reference to this builder
         */
        B withContent(XMLContent<RC, WC> content);
    }

    abstract class AbstractBuilder<RC, WC, T extends XMLContainer<RC, WC>, B extends Builder<RC, WC, T, B>> extends XMLParticle.AbstractBuilder<RC, WC, T, B> implements Builder<RC, WC, T, B> {
        private volatile XMLContent<RC, WC> content = XMLContent.empty();

        protected AbstractBuilder() {
        }

        @Override
        public B withContent(XMLContent<RC, WC> content) {
            this.content = content;
            return this.builder();
        }

        protected XMLContent<RC, WC> getContent() {
            return this.content;
        }
    }
}
