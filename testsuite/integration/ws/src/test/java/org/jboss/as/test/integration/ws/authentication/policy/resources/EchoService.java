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

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.xml.ws.WebServiceContext;

/**
 * Session Bean implementation class EchoService
 */
@Stateless
@WebService(
        targetNamespace = "http://ws.picketlink.sts.jboss.org/")
@SecurityDomain("sp")
@RolesAllowed("testRole")
@HandlerChain(file = "dummmy-ws-handler.xml")
public class EchoService implements EchoServiceRemote {

    @Resource
    private WebServiceContext wsCtx;

    @Resource
    private SessionContext ejbCtx;

    @Override
    @WebMethod
    public void echo(final String echo) {
        try {
            Subject subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");
            SecurityContext current = SecurityContextAssociation.getSecurityContext();
        } catch (PolicyContextException e) {
            throw new IllegalStateException(e);
        }
    }

}
