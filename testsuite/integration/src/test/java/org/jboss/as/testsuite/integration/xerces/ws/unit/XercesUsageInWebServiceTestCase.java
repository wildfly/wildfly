/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.xerces.ws.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.xerces.ws.XercesUsageWSEndpoint;
import org.jboss.as.testsuite.integration.xerces.ws.XercesUsageWebService;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

/**
 * Tests that packaging a xerces jar within a web application containing a webservice implementation, doesn't break the
 * functioning of the webservice.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XercesUsageInWebServiceTestCase {

    private static final String WEBSERVICE_WEB_APP_CONTEXT = "xerces-webservice-webapp";

    private static final String BASE_URL = "http://localhost:8080/";

    private static final Logger logger = Logger.getLogger(XercesUsageInWebServiceTestCase.class);

    /**
     * Creates a .war file, containing a webservice implementation and also packages the xerces jar within the
     * web application's .war/WEB-INF/lib
     *
     * @return
     */
    @Deployment(name = "webservice-app-with-xerces", testable = false)
    public static WebArchive createWebServiceDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBSERVICE_WEB_APP_CONTEXT + ".war");
        war.addClasses(XercesUsageWebService.class, XercesUsageWSEndpoint.class);
        // add a web.xml containing the webservice mapping as a servlet
        war.addAsWebInfResource("xerces/xerces-webservice-web.xml", "web.xml");
        // add a dummy xml to parse
        war.addAsResource("xerces/dummy.xml", "dummy.xml");

        // add the xerces jar in the .war/WEB-INF/lib
        war.addAsLibrary("xerces/xercesImpl.jar", "xercesImpl.jar");

        logger.info(war.toString(true));
        return war;
    }

    /**
     * Test that the webservice invocation works fine
     *
     * @throws Exception
     */
    @OperateOnDeployment("webservice-app-with-xerces")
    @Test
    public void testXercesUsageInWebService() throws Exception {

        final QName serviceName = new QName("org.jboss.as.testsuite.integration.xerces.ws", "XercesUsageWebService");
        final URL wsdlURL = new URL(BASE_URL + WEBSERVICE_WEB_APP_CONTEXT + "/XercesUsageWebService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final XercesUsageWSEndpoint port = service.getPort(XercesUsageWSEndpoint.class);
        final String xml = "dummy.xml";
        final String result = port.parseUsingXerces(xml);
        Assert.assertEquals("Unexpected return message from webservice", XercesUsageWebService.SUCCESS_MESSAGE, result);
    }


}
