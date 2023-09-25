/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.provider;

import java.util.Currency;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ext.ParamConverter;

import org.jboss.logging.Logger;

/**
 * A Jakarta RESTful Web Services {@link ParamConverter} test implementation for Currency.
 *
 * @author Josef Cacek
 */
@Path("/")
public class CurrencyConverterProvider {

    private static final Logger LOGGER = Logger.getLogger(CurrencyConverterProvider.class);

    public static final String PARAM_CURRENCY = "currency";
    public static final String PATH_CONVERTER = "/converter/{" + PARAM_CURRENCY + "}";

    /**
     * Test method for currency converter.
     *
     * @param currency
     *
     * @return
     */
    @GET
    @Path(PATH_CONVERTER)
    public String testCurrencyConverter(@PathParam(PARAM_CURRENCY) Currency currency) {
        LOGGER.trace("Returning currency symbol");
        return currency.getSymbol();
    }
}
