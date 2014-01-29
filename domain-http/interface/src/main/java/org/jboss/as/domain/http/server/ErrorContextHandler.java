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

import java.util.Locale;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

import static io.undertow.predicate.Predicates.not;
import static io.undertow.predicate.Predicates.path;

/**
 * ResourceHandler for the error context.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ErrorContextHandler {

    private static final String INDEX_HTML = "index.html";
    private static final String INDEX_WIN_HTML = "index_win.html";
    private static final String ERROR_MODULE = "org.jboss.as.domain-http-error-context";

    static final String ERROR_CONTEXT = "/error";

    private static final String DEFAULT_RESOURCE;

    static {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase(Locale.ENGLISH).contains("win")) {
            DEFAULT_RESOURCE = "/" + INDEX_WIN_HTML;
        } else {
            DEFAULT_RESOURCE = "/" + INDEX_HTML;
        }
    }

    private ErrorContextHandler() {

    }

    public static HttpHandler createErrorContext(final String slot) throws ModuleLoadException {
        final ClassPathResourceManager cpresource = new ClassPathResourceManager(getClassLoader(Module.getCallerModuleLoader(), ERROR_MODULE, slot), "");
        final io.undertow.server.handlers.resource.ResourceHandler handler = new io.undertow.server.handlers.resource.ResourceHandler()
                .setAllowed(not(path("META-INF")))
                .setResourceManager(cpresource)
                .setDirectoryListingEnabled(false)
                .setCachable(Predicates.<HttpServerExchange>falsePredicate());

        //we also need to setup the default resource redirect
        PredicateHandler predicateHandler = new PredicateHandler(path("/"), new RedirectHandler(ERROR_CONTEXT + DEFAULT_RESOURCE), handler);
        return predicateHandler;
    }

    private static ClassLoader getClassLoader(final ModuleLoader moduleLoader, final String module, final String slot) throws ModuleLoadException {
        ModuleIdentifier id = ModuleIdentifier.create(module, slot);
        ClassLoader cl = moduleLoader.loadModule(id).getClassLoader();

        return cl;
    }
}
