/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.HttpContext;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.modules.ModuleLoadException;

/**
 * Different modes for showing the admin console
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public enum ConsoleMode {

    /**
     * Show the console normally
     */
    CONSOLE {
        @Override
        ResourceHandler createConsoleHandler(String skin) throws ModuleLoadException {
            return new ConsoleHandler(skin);
        }
        @Override
        public boolean hasConsole() {
            return true;
        }
    },
    /**
     * If an attempt is made to go to the console show an error saying the host is a slave
     */
    SLAVE_HC {
        @Override
        ResourceHandler createConsoleHandler(String skin) throws ModuleLoadException {
            return DisabledConsoleHandler.createNoConsoleForSlave(skin);
        }
        @Override
        public boolean hasConsole() {
            return false;
        }
    },
    /**
     * If an attempt is made to go to the console show an error saying the server/host is in admin-only mode
     */
    ADMIN_ONLY{
        @Override
        ResourceHandler createConsoleHandler(String skin) throws ModuleLoadException {
            return DisabledConsoleHandler.createNoConsoleForAdminMode(skin);
        }
        @Override
        public boolean hasConsole() {
            return false;
        }
    },
    /**
     * If an attempt is made to go to the console a 404 is shown
     */
    NO_CONSOLE{
        @Override
        ResourceHandler createConsoleHandler(String skin) throws ModuleLoadException {
            return null;
        }
        @Override
        public boolean hasConsole() {
            return false;
        }
    };

    /**
     * Returns a console handler for the mode
     *
     * @param skin the console look and feel to use
     *
     * @return the console handler, may be {@code null}
     */
    ResourceHandler createConsoleHandler(String skin) throws ModuleLoadException {
        throw new IllegalStateException("Not overridden for " + this);
    }

    /**
     * Returns whether the console is displayed or not
     */
    public boolean hasConsole() {
        throw new IllegalStateException("Not overridden for " + this);
    }

    /**
     * An extension of the ResourceHandler to configure the handler to server up resources from the console module only.
     *
     * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
     */
    private static class ConsoleHandler extends ResourceHandler {

        private static final String NOCACHE_JS = ".nocache.js";
        private static final String INDEX_HTML = "index.html";
        private static final String APP_HTML = "App.html";

        private static final String CONSOLE_MODULE = "org.jboss.as.console";
        private static final String CONTEXT = "/console";
        private static final String DEFAULT_RESOURCE = "/" + INDEX_HTML;

        ConsoleHandler(String skin) throws ModuleLoadException {
            super(CONTEXT, DEFAULT_RESOURCE, findConsoleClassLoader(skin));
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
            HttpContext httpContext = httpServer.createContext(getContext(), this);
            if (securityRealm != null
                    && securityRealm.getSupportedAuthenticationMechanisms().contains(AuthenticationMechanism.CLIENT_CERT) == false) {
                httpContext.getFilters().add(new RedirectReadinessFilter(securityRealm, ErrorHandler.getRealmRedirect()));
            }
        }

        private static ClassLoader findConsoleClassLoader(String consoleSkin) throws ModuleLoadException {
            final String moduleName = CONSOLE_MODULE + "." + (consoleSkin == null ? "main" : consoleSkin);

            // Find all console versions on the filesystem, sorted by version
            SortedSet<ConsoleVersion> consoleVersions = findConsoleVersions(moduleName);
            for (ConsoleVersion consoleVersion : consoleVersions) {
                try {
                    return getClassLoader(moduleName, consoleVersion.getName());
                } catch (ModuleLoadException mle) {
                    // ignore
                }
            }

            // No joy. Fall back to the AS 7.1 approach where the module id is org.jboss.as.console:<sking>
            try {
                return getClassLoader(CONSOLE_MODULE, consoleSkin);
            } catch (ModuleLoadException mle) {
                // ignore
            }

            throw HttpServerMessages.MESSAGES.consoleModuleNotFound(moduleName);
        }
    }

    /**
     * An extension of the ResourceHandler to configure the handler to show an error page when the console has been turned off.
     */
    static class DisabledConsoleHandler extends ResourceHandler {

        private static final String ERROR_MODULE = "org.jboss.as.domain-http-error-context";
        private static final String CONTEXT = "/consoleerror";
        private static final String NO_CONSOLE_FOR_SLAVE = "/noConsoleForSlaveDcError.html";
        private static final String NO_CONSOLE_FOR_ADMIN_MODE = "/noConsoleForAdminModeError.html";

        private DisabledConsoleHandler(String slot, String resource) throws ModuleLoadException {
            super(CONTEXT, resource, getClassLoader(ERROR_MODULE, slot));
        }

        static DisabledConsoleHandler createNoConsoleForSlave(String slot) throws ModuleLoadException {
            return new DisabledConsoleHandler(slot, NO_CONSOLE_FOR_SLAVE);
        }

        static DisabledConsoleHandler createNoConsoleForAdminMode(String slot) throws ModuleLoadException {
            return new DisabledConsoleHandler(slot, NO_CONSOLE_FOR_ADMIN_MODE);
        }

        @Override
        protected boolean skipCache(String resource) {
            /*
             * This context is not expected to be used a lot, however if the pages can
             * be cached this can cause problems with new installations that may
             * have different content.
             */
            return true;
        }
    }

    /**
     * Scan filesystem looking for the slot versions of all modules with the given name.
     * Package protected to allow unit testing.
     *
     * @param moduleName the name portion of the target module's {@code ModuleIdentifier}
     *
     * @return set of console versions, sorted from highest version to lowest
     */
    static SortedSet<ConsoleVersion> findConsoleVersions(String moduleName) {
        String path = moduleName.replace('.', '/') ;

        final String modulePath = SecurityActions.getProperty("module.path");
        File[] moduleRoots = getFiles(modulePath, 0, 0);
        SortedSet<ConsoleVersion> consoleVersions = new TreeSet<ConsoleVersion>();
        for (File root : moduleRoots) {
            findConsoleModules(root, path, consoleVersions);
            File layers = new File(root, "system" + File.separator + "layers");
            File[] children = layers.listFiles();
            if (children != null) {
                for (File child : children) {
                    findConsoleModules(child, path, consoleVersions);
                }
            }
            File addOns = new File(root, "system" + File.separator + "add-ons");
            children = addOns.listFiles();
            if (children != null) {
                for (File child : children) {
                    findConsoleModules(child, path, consoleVersions);
                }
            }
        }
        return consoleVersions;
    }

    private static void findConsoleModules(File root, String path, Set<ConsoleVersion> consoleVersions) {
        File module = new File(root, path);
        File[] children = module.listFiles();
        if (children != null) {
            for (File child : children) {
                consoleVersions.add(new ConsoleVersion(child.getName()));
            }
        }
    }

    private static File[] getFiles(final String modulePath, final int stringIdx, final int arrayIdx) {
        final int i = modulePath.indexOf(File.pathSeparatorChar, stringIdx);
        final File[] files;
        if (i == -1) {
            files = new File[arrayIdx + 1];
            files[arrayIdx] = new File(modulePath.substring(stringIdx)).getAbsoluteFile();
        } else {
            files = getFiles(modulePath, i + 1, arrayIdx + 1);
            files[arrayIdx] = new File(modulePath.substring(stringIdx, i)).getAbsoluteFile();
        }
        return files;
    }
}
