/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * A JUnit 4 test for testing deployment multiple Jakarta RESTful Web Services providers from different modules. It's mainly a regression test for
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
        war.addClasses(MyApplication.class, CurrencyConverterProvider.class, CurrencyParamConverter.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * Test Jakarta RESTful Web Services providers deployment, when 2 web applications are used.
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
