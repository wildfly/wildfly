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

package org.jboss.as.webservices.util;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.servlet.Servlet;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;

import org.jboss.jandex.DotName;
import org.jboss.ws.api.annotation.WebContext;

/**
 * Centralized DotNames relevant for WS integration.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DotNames {

    private DotNames() {
        // forbidden instantiation
    }

    public static final DotName HANDLER_CHAIN_ANNOTATION = DotName.createSimple(HandlerChain.class.getName());
    public static final DotName JAXWS_SERVICE_CLASS = DotName.createSimple(Service.class.getName());
    public static final DotName OBJECT_CLASS = DotName.createSimple(Object.class.getName());
    public static final DotName ROLES_ALLOWED_ANNOTATION = DotName.createSimple(RolesAllowed.class.getName());
    public static final DotName DECLARE_ROLES_ANNOTATION = DotName.createSimple(DeclareRoles.class.getName());
    public static final DotName PERMIT_ALL_ANNOTATION = DotName.createSimple(PermitAll.class.getName());
    public static final DotName SERVLET_CLASS = DotName.createSimple(Servlet.class.getName());
    public static final DotName SINGLETON_ANNOTATION = DotName.createSimple(Singleton.class.getName());
    public static final DotName STATELESS_ANNOTATION = DotName.createSimple(Stateless.class.getName());
    public static final DotName WEB_CONTEXT_ANNOTATION = DotName.createSimple(WebContext.class.getName());
    public static final DotName WEB_SERVICE_ANNOTATION = DotName.createSimple(WebService.class.getName());
    public static final DotName WEB_SERVICE_PROVIDER_ANNOTATION = DotName.createSimple(WebServiceProvider.class.getName());
    public static final DotName WEB_SERVICE_REF_ANNOTATION = DotName.createSimple(WebServiceRef.class.getName());
    public static final DotName WEB_SERVICE_REFS_ANNOTATION = DotName.createSimple(WebServiceRefs.class.getName());

}
