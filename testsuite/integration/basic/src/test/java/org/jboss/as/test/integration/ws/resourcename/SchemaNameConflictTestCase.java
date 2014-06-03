/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.integration.ws.resourcename;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ws.resourcename.generated.ObjectFactory;
import org.jboss.as.test.integration.ws.resourcename.generated.RequestType;
import org.jboss.as.test.integration.ws.resourcename.generated.ResponseType;
import org.jboss.as.test.integration.ws.resourcename.generated.ServiceV1;
import org.jboss.as.test.integration.ws.resourcename.generated.ServiceV1Port;
import org.jboss.as.test.integration.ws.resourcename.generated.SomeFeatureType;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014
 * Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SchemaNameConflictTestCase {

    private static final Logger log = Logger.getLogger(SchemaNameConflictTestCase.class);
    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static Archive mainDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "resource-name-issue.war").
                addPackage(ServiceV1.class.getPackage()).
                addClass(ServiceServlet.class).
                addAsWebInfResource(SchemaNameConflictTestCase.class.getPackage(), "service.wsdl", "wsdl/service.wsdl").
                addAsWebInfResource(SchemaNameConflictTestCase.class.getPackage(), "d1/test.xsd", "wsdl/d1/test.xsd").
                addAsWebInfResource(SchemaNameConflictTestCase.class.getPackage(), "d1/d1/test.xsd", "wsdl/d1/d1/test.xsd").
                addAsWebInfResource(SchemaNameConflictTestCase.class.getPackage(), "web.xml", "web.xml");
        log.debug(war.toString(true));
        return war;
    }

    @Test
    public void testService() throws Exception {
        QName serviceName = new QName("http://www.test.nl/xsd/test/", "service_v1");
        URL wsdlURL = new URL(baseUrl, "serviceV1?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        ServiceV1Port proxy = (ServiceV1Port) service.getPort(ServiceV1Port.class);
        ObjectFactory factory = new ObjectFactory();
        SomeFeatureType feature = factory.createSomeFeatureType();
        feature.setSomeMeasure(18);
        RequestType request = factory.createRequestType();
        request.setRequest(feature);               
        ResponseType response = proxy.testRequest(request);
    }
    
    @Test
    public void testXsdResolution() throws Exception {
        URL xsdURL = new URL(baseUrl, "serviceV1?xsd=d1/test.xsd");
        URL includedXsdURL = new URL(baseUrl, "serviceV1?xsd=d1/d1/test.xsd");
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(xsdURL.toURI());
        HttpResponse response =  client.execute(httpGet);
        String content = new String(IOUtil.asByteArray(response.getEntity().getContent()), "UTF-8");
        assertThat(content, content.contains("<include schemaLocation=\"http://localhost:8080/resource-name-issue/serviceV1?xsd=d1/d1/test.xsd\"/>"), is(true));
        httpGet = new HttpGet(includedXsdURL.toURI());
        response =  client.execute(httpGet);
        assertThat(response.getStatusLine().getStatusCode(), is(200));
        content = new String(IOUtil.asByteArray(response.getEntity().getContent()), "UTF-8");
        assertThat(content, content.contains("<complexType name=\"SomeFeatureType\">"), is(true));
    }
}
