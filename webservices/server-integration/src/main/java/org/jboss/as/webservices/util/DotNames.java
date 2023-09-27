/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.util;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateless;
import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.servlet.Servlet;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.WebServiceRef;
import jakarta.xml.ws.WebServiceRefs;

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
