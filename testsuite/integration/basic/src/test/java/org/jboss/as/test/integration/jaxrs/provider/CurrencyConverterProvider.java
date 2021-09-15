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
import javax.ws.rs.ext.ParamConverter;

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
