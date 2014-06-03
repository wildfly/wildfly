/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.resourcename;

import javax.jws.WebService;
import org.apache.cxf.annotations.SchemaValidation;
import org.jboss.as.test.integration.ws.resourcename.generated.ObjectFactory;
import org.jboss.as.test.integration.ws.resourcename.generated.RequestType;
import org.jboss.as.test.integration.ws.resourcename.generated.ResponseType;
import org.jboss.as.test.integration.ws.resourcename.generated.ServiceV1Port;
import org.jboss.logging.Logger;


@WebService(
  serviceName = "service_v1", 
  portName = "service_v1_port", 
  endpointInterface = "org.jboss.as.test.integration.ws.resourcename.generated.ServiceV1Port", 
  targetNamespace = "http://www.test.nl/xsd/test/", 
  wsdlLocation = "WEB-INF/wsdl/service.wsdl"
)
@SchemaValidation
public class ServiceServlet implements ServiceV1Port {
private static final Logger log = Logger.getLogger(ServiceServlet.class);
    @Override
    public ResponseType testRequest(RequestType request) {
        log.info("test request received");
        ObjectFactory factory = new ObjectFactory();
        ResponseType response = factory.createResponseType();
        response.setText("request correctly received");
        return response;
    }
}
