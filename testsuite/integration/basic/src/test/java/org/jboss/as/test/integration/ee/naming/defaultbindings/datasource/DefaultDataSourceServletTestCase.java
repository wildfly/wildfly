/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *  Test for EE's default data source on a Servlet
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DefaultDataSourceServletTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DefaultDataSourceServletTestCase.class.getSimpleName()+".war");
        war.addClasses(HttpRequest.class, DefaultDataSourceTestServlet.class);
        return war;
    }

    @Test
    public void testServlet() throws Exception {
        HttpRequest.get(url.toExternalForm() + "simple", 10, SECONDS);
    }

}
