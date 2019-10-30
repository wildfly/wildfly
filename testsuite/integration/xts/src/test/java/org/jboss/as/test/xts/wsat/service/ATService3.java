/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.wsat.service;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.annotation.WebServlet;


@WebService(serviceName = "ATService3", portName = "AT", name = "AT", targetNamespace = "http://www.jboss.com/jbossas/test/xts/wsat/at/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name = "ATService3", urlPatterns = {"/ATService3"})
public class ATService3 extends ATSuperService {
    public static final String LOG_NAME = "service3";

    @WebMethod
    public void invoke(ServiceCommand... serviceCommands) throws TestApplicationException {
        super.invokeWithCallName(LOG_NAME, serviceCommands);
    }
}
