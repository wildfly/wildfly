/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust.picketlink;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.interceptor.InInterceptors;
import org.picketlink.identity.federation.core.wstrust.PicketLinkSTS;

import javax.annotation.Resource;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;

/**
 * A Security Token Service implementation extending default PicketLink one
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
@WebServiceProvider(serviceName = "PicketLinkSTS", portName = "PicketLinkSTSPort", targetNamespace = "urn:picketlink:identity-federation:sts", wsdlLocation = "WEB-INF/wsdl/PicketLinkSTS.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
//be sure to have dependency on org.apache.cxf module when on AS7, otherwise Apache CXF annotations are ignored
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.username", value = "mystskey"),
        @EndpointProperty(key = "ws-security.signature.properties", value = "stsKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.jboss.as.test.integration.ws.wsse.trust.sts.STSCallbackHandler"),
        @EndpointProperty(key = "ws-security.validate.token", value = "false") //to let the JAAS integration deal with validation through the interceptor below
})
@InInterceptors(interceptors = {"org.jboss.wsf.stack.cxf.security.authentication.SubjectCreatingPolicyInterceptor"})
public class PicketLinkSTService extends PicketLinkSTS {
    @Resource
    public void setWSC(WebServiceContext wctx) {
        this.context = wctx;
    }

}
