/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.webservices;

import javax.ejb.Stateless;
import javax.xml.ws.WebServiceRef;

/**
 * A simple stateless session bean.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@Stateless
public class SimpleStatelessSessionBean implements SimpleStatelessSessionLocal {

    @WebServiceRef(wsdlLocation="http://localhost:8080/ws-example?wsdl", value=EndpointService.class)
    private Endpoint endpoint1;

    @WebServiceRef(value=EndpointService.class)
    private Endpoint endpoint2;

    @WebServiceRef
    private EndpointService endpoint3;

    private EndpointService endpoint4;

    @WebServiceRef(wsdlLocation="http://localhost:8080/ws-example?wsdl", value=EndpointService.class)
    public void setEndpoint4(EndpointService endpoint) {
        endpoint4 = endpoint;
    }

    public String echo(String msg) {
        return "Echo " + msg + " -- Endpoint:" + endpoint1 + " " + endpoint2 + " " + endpoint3 + " " + endpoint4 + ")";
    }
}
