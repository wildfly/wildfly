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

package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.ErrorHandler.ERROR_CONTEXT;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.com.sun.net.httpserver.HttpContext;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.modules.ModuleLoadException;

/**
 * An extension of the ResourceHandler to configure the handler to server up resources from the console module only.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConsoleHandler extends ResourceHandler {

    private static final String NOCACHE_JS = ".nocache.js";
    private static final String INDEX_HTML = "index.html";
    private static final String APP_HTML = "App.html";

    private static final String CONSOLE_MODULE = "org.jboss.as.console";
    private static final String CONTEXT = "/console";
    private static final String DEFAULT_RESOURCE = "/" + INDEX_HTML;

    public ConsoleHandler() throws ModuleLoadException {
        super(CONTEXT, DEFAULT_RESOURCE, getClassLoader(CONSOLE_MODULE));
    }

    @Override
    protected boolean skipCache(String resource) {
        return resource.endsWith(NOCACHE_JS) || resource.endsWith(APP_HTML) || resource.endsWith(INDEX_HTML);
    }

    /*
     * This method is override so we can ensure the RealmReadinessFilter is in place.
     *
     * (Later may change the return type to return the context so a sub-class can just continue after the parent class start)
     */

    @Override
    public void start(HttpServer httpServer, SecurityRealm securityRealm) {
        HttpContext httpContext = httpServer.createContext(CONTEXT, this);
        if (securityRealm != null) {
            DomainCallbackHandler domainCBH = securityRealm.getCallbackHandler();
            httpContext.getFilters().add(new RealmReadinessFilter(domainCBH, ERROR_CONTEXT));
        }
    }

}
