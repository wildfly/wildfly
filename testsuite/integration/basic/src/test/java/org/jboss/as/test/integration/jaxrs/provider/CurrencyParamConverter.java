/*
 * Copyright 2021 Red Hat, Inc.
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

package org.jboss.as.test.integration.jaxrs.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Currency;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

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
