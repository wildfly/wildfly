/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Currency;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Provider
public class CurrencyParamConverter implements ParamConverter<Currency>, ParamConverterProvider {

    private static final Logger LOGGER = Logger.getLogger(CurrencyParamConverter.class);

    // Public methods --------------------------------------------------------

    /**
     * Converts from a provided String currency code to a Currency instance.
     *
     * @param str
     *
     * @return
     */
    @Override
    public Currency fromString(String str) {
        LOGGER.trace("Converting to currency: " + str);
        return Currency.getInstance(str);
    }

    /**
     * Returns Currency code.
     *
     * @param currency
     *
     * @return
     */
    @Override
    public String toString(Currency currency) {
        return currency.getCurrencyCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (Currency.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) this;
        }
        return null;
    }
}
