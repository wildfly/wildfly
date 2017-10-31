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

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.Currency;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A JUnit 4 test for testing deployment multiple JAX-RS providers from different modules. It's mainly a regression test for
 * JBPAPP-9963 issue.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomProvidersTestCase {

    private static Logger LOGGER = Logger.getLogger(CustomProvidersTestCase.class);

    private static final String WEBAPP_TEST_EXCEPTION_MAPPER = "test-exception-mapper";
    private static final String WEBAPP_TEST_CONVERTER = "test-converter";

    // Public methods --------------------------------------------------------

    /**
     * Creates {@value #WEBAPP_TEST_EXCEPTION_MAPPER} web application.
     *
     * @return
     */
    @Deployment(name = WEBAPP_TEST_EXCEPTION_MAPPER)
    public static WebArchive deployMapperApp() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_TEST_EXCEPTION_MAPPER + ".war");
        war.addClasses(MyApplication.class, ExceptionMapperProvider.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * Creates {@value #WEBAPP_TEST_CONVERTER} web application.
     *
     * @return
     */
    @Deployment(name = WEBAPP_TEST_CONVERTER)
    public static WebArchive deployConverterApp() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_TEST_CONVERTER + ".war");
        war.addClasses(MyApplication.class, CurrencyConverterProvider.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * Test JAX-RS providers deployment, when 2 web applications are used.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_TEST_EXCEPTION_MAPPER)
    public void testProvidersInTwoWars(@ArquillianResource URL webAppURL) throws Exception {
        final String path = webAppURL.toExternalForm() + ExceptionMapperProvider.PATH_EXCEPTION.substring(1);
        LOGGER.trace("Requested path: " + path);
        assertEquals(ExceptionMapperProvider.ERROR_MESSAGE, HttpRequest.get(path, 10, TimeUnit.SECONDS));

        final String converterPath = webAppURL.toExternalForm().replace(WEBAPP_TEST_EXCEPTION_MAPPER, WEBAPP_TEST_CONVERTER)
                + CurrencyConverterProvider.PATH_CONVERTER.substring(1).replace(
                "{" + CurrencyConverterProvider.PARAM_CURRENCY + "}", "USD");
        LOGGER.trace("Requested path: " + converterPath);
        assertEquals(Currency.getInstance("USD").getSymbol(), HttpRequest.get(converterPath, 10, TimeUnit.SECONDS));
    }

}
