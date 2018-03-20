/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ws.authentication.policy.resources;

import org.jboss.logging.Logger;
import org.picketlink.identity.federation.core.wstrust.PicketLinkSTS;

import javax.annotation.Resource;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.WebServiceContext;

@WebServiceProvider(
        serviceName = "PicketLinkSTS",
        portName = "PicketLinkSTSPort",
        targetNamespace = "urn:picketlink:identity-federation:sts",
        wsdlLocation = "WEB-INF/wsdl/PicketLinkSTS.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
public class PicketLinkSTSService extends PicketLinkSTS implements Provider<SOAPMessage> {

    private static Logger log = Logger.getLogger(PicketLinkSTSService.class.getName());

    @Resource
    public void setWSC(WebServiceContext wctx) {
        log.debug("Setting WebServiceContext = " + wctx);
        this.context = wctx;
    }
}