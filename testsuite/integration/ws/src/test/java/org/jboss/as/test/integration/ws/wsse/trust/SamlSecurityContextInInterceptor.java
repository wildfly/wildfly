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
package org.jboss.as.test.integration.ws.wsse.trust;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.security.SecurityDomainContext;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;

public class SamlSecurityContextInInterceptor extends WSS4JInInterceptor {

   public SamlSecurityContextInInterceptor() {
      super();
      getAfter().add(PolicyBasedWSS4JInInterceptor.class.getName());
   }

   @Override
   public void handleMessage(SoapMessage message) throws Fault {
      final SecurityContext securityContext = message.get(SecurityContext.class);
      final Principal principal = securityContext.getUserPrincipal();
      final String name = principal.getName();
      final Endpoint endpoint = message.getExchange().get(Endpoint.class);
      final SecurityDomainContext securityDomainContext = endpoint.getSecurityDomainContext();
      Principal simplePrincipal = new SimplePrincipal(name);
      Subject subject = new Subject(false, Collections.singleton(simplePrincipal), Collections.emptySet(), Collections.emptySet());
      securityDomainContext.pushSubjectContext(subject, simplePrincipal, null);
      message.put(SecurityContext.class, new DefaultSecurityContext(simplePrincipal, subject));
   }

}
