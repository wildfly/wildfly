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

package org.jboss.as.test.integration.ws;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for setting statistics-enabled=true attribute in webservice subsystem
 *
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebServiceEndpointMetricsTestCase extends ContainerResourceMgmtTestBase { 
	private static final ModelNode webserviceAddress;
	private static final ModelNode wsEndpointAddress;
	@ArquillianResource
    URL baseUrl;
	static {
        webserviceAddress = new ModelNode();
        webserviceAddress.add("subsystem", "webservices");

        wsEndpointAddress = new ModelNode();
        wsEndpointAddress.add("deployment", "ws-endpoint-metrics.war");
        wsEndpointAddress.add("subsystem", "webservices");
        try {
			wsEndpointAddress.add("endpoint", URLEncoder.encode("ws-endpoint-metrics:TestService", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
		}
        wsEndpointAddress.protect();        
    }
    private static final Logger log = Logger.getLogger(WebServiceEndpointMetricsTestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-endpoint-metrics.war");
        war.addPackage(SimpleWebserviceEndpointImpl.class.getPackage());
        war.addClass(SimpleWebserviceEndpointImpl.class);
        war.addAsWebInfResource(SimpleWebserviceEndpointTestCase.class.getPackage(),"web.xml","web.xml");
        log.info(war.toString(true));
        return war;
    }

    @Test
    public void testStatisticsEnabled() throws Exception {    	   	
        final QName serviceName = new QName("org.jboss.as.test.integration.ws", "SimpleService");
        final URL wsdlURL = new URL(baseUrl, "/ws-endpoint-metrics/SimpleService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final SimpleWebserviceEndpointIface port = service.getPort(SimpleWebserviceEndpointIface.class);
           
        setStatisticsEnabled(true);
        final String result = port.echo("hello");
        Assert.assertEquals("hello", result);
        ModelNode resource = readRuntimeResource();
        int requestCount = resource.get("request-count").asInt();
        assertEquals("request-count value is not expceted", 1, requestCount);
        
        
        setStatisticsEnabled(false);
        port.echo("hello");
        resource = readRuntimeResource();
        String requestCountString = resource.get("request-count").asString();
        assertTrue("No metrics available response is expected", requestCountString.contains("No metrics available"));        
    }
    
    
    private void setStatisticsEnabled(boolean enabled) throws Exception {
        final ModelNode updateStatistics = new ModelNode();
        updateStatistics.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        updateStatistics.get(ModelDescriptionConstants.OP_ADDR).set(webserviceAddress);
        updateStatistics.get(ModelDescriptionConstants.NAME).set("statistics-enabled");
        updateStatistics.get(ModelDescriptionConstants.VALUE).set(enabled);
        executeOperation(updateStatistics);
    }
    
    private ModelNode readRuntimeResource() throws Exception {
    	final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");    
        operation.get(OP_ADDR).set(wsEndpointAddress);
        operation.get(INCLUDE_RUNTIME).set(true);
        return executeOperation(operation);  
    }

}

