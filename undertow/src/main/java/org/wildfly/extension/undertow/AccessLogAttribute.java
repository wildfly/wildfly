/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.undertow;

import java.util.Objects;
import java.util.function.Function;

import io.undertow.attribute.ExchangeAttribute;
import io.undertow.server.HttpServerExchange;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class AccessLogAttribute implements Comparable<AccessLogAttribute> {
    private final String key;
    private final ExchangeAttribute exchangeAttribute;
    private final Function<String, Object> valueConverter;

    private AccessLogAttribute(final String key, final ExchangeAttribute exchangeAttribute, final Function<String, Object> valueConverter) {
        this.key = key;
        this.exchangeAttribute = exchangeAttribute;
        this.valueConverter = valueConverter;
    }

    /**
     * Creates a new attribute.
     *
     * @param key               the key for the attribute
     * @param exchangeAttribute the exchange attribute which resolves the value
     *
     * @return the new attribute
     */
    static AccessLogAttribute of(final String key, final ExchangeAttribute exchangeAttribute) {
        return new AccessLogAttribute(key, exchangeAttribute, null);
    }


    /**
     * Creates a new attribute.
     *
     * @param key               the key for the attribute
     * @param exchangeAttribute the exchange attribute which resolves the value
     * @param valueConverter    the converter used to convert the
     *                          {@linkplain ExchangeAttribute#readAttribute(HttpServerExchange) string} into a different
     *                          type
     *
     * @return the new attribute
     */
    static AccessLogAttribute of(final String key, final ExchangeAttribute exchangeAttribute, final Function<String, Object> valueConverter) {
        return new AccessLogAttribute(key, exchangeAttribute, valueConverter);
    }

    /**
     * Returns the key used for the structured log output.
     *
     * @return the key
     */
    String getKey() {
        return key;
    }

    /**
     * Resolves the value for the attribute.
     *
     * @param exchange the exchange to resolve the value from
     *
     * @return the value of the attribute
     */
    Object resolveAttribute(final HttpServerExchange exchange) {
        if (valueConverter == null) {
            return exchangeAttribute.readAttribute(exchange);
        }
        return valueConverter.apply(exchangeAttribute.readAttribute(exchange));
    }

    @Override
    public int compareTo(final AccessLogAttribute o) {
        return key.compareTo(o.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AccessLogAttribute)) {
            return false;
        }
        final AccessLogAttribute other = (AccessLogAttribute) obj;
        return key.equals(other.key);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{key=" + key + ", exchangeAttribute=" + exchangeAttribute +
                ", valueConverter=" + valueConverter + "}";
    }
}
