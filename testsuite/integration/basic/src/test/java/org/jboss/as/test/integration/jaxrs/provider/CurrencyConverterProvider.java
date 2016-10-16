/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jaxrs.provider;

import java.util.Currency;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.StringConverter;

/**
 * A RestEasy {@link StringConverter} test implementation for Currency.
 *
 * @author Josef Cacek
 */
@Provider
@Path("/")
public class CurrencyConverterProvider implements StringConverter<Currency> {

    private static Logger LOGGER = Logger.getLogger(CurrencyConverterProvider.class);

    public static final String PARAM_CURRENCY = "currency";
    public static final String PATH_CONVERTER = "/converter/{" + PARAM_CURRENCY + "}";

    // Public methods --------------------------------------------------------

    /**
     * Converts from a provided String currency code to a Currency instance.
     *
     * @param str
     * @return
     * @see org.jboss.resteasy.spi.StringConverter#fromString(java.lang.String)
     */
    public Currency fromString(String str) {
        LOGGER.trace("Converting to currency: " + str);
        return Currency.getInstance(str);
    }

    /**
     * Returns Currency code.
     *
     * @param currency
     * @return
     * @see org.jboss.resteasy.spi.StringConverter#toString(java.lang.Object)
     */
    public String toString(Currency currency) {
        return currency.getCurrencyCode();
    }

    /**
     * Test method for currency converter.
     *
     * @param currency
     * @return
     */
    @GET
    @Path(PATH_CONVERTER)
    public String testCurrencyConverter(@PathParam(PARAM_CURRENCY) Currency currency) {
        LOGGER.trace("Returning currency symbol");
        return currency.getSymbol();
    }

}
