/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.logging.Logger;

/**
 * Handler which keeps track of registered paths. If it is enabled, it will make HttpServerExchange return configured error
 * code. Otherwise it will trigger PathHandler.
 *
 * @author baranowb
 *
 */
public class HttpUnavailablePathHandler implements HttpHandler {

    protected static final Logger log = Logger.getLogger(HttpUnavailablePathHandler.class);
    protected static final boolean traceEnabled;
    static {
        traceEnabled = log.isTraceEnabled();
    }

    protected static Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
        public int compare(String str1, String str2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
            if (res == 0) {
                res = str1.compareTo(str2);
            }
            return res;
        }
    };

    protected static final char PATH_SEPARATOR = '/';

    protected final PathHandler pathHandler;
    protected CustomResponseCodeHandler responseCodeHandler;


    protected Set<String> exactPaths = new TreeSet<String>(ALPHABETICAL_ORDER);
    protected Set<String> prefixtPaths = new TreeSet<String>(ALPHABETICAL_ORDER);

    /**
     * lengths of all registered paths
     */
    private volatile int[] lengths = {};

    /**
     * @param pathHandler
     */
    public HttpUnavailablePathHandler(final PathHandler handler) {
        super();
        this.pathHandler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (this.responseCodeHandler!=null && !matchPath(this.normalizeSlashes(exchange.getRelativePath()))) {
            this.responseCodeHandler.handleRequest(exchange);
            return;
        }

        this.pathHandler.handleRequest(exchange);
    }

    public void setResponseCode(final int responseCode) {
        this.responseCodeHandler = new CustomResponseCodeHandler(responseCode);
    }

    protected boolean matchPath(final String path) {
        if (this.exactPaths.contains(path) || prefixtPaths.contains(path)) {
            return true;
        } else {

            int length = path.length();
            final int[] lengths = this.lengths;
            for (int i = 0; i < lengths.length; ++i) {
                int pathLength = lengths[i];
                if (pathLength < length) {
                    char c = path.charAt(pathLength);
                    if (c == '/') {
                        String part = path.substring(0, pathLength);
                        if (prefixtPaths.contains(part)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public synchronized HttpUnavailablePathHandler addPrefixPath(final String path, final HttpHandler handler) {
        this.pathHandler.addPrefixPath(path, handler);
        this.prefixtPaths.add(this.normalizeSlashes(path));
        this.buildLengths();
        return this;
    }

    public synchronized HttpUnavailablePathHandler addExactPath(final String path, final HttpHandler handler) {
        this.pathHandler.addExactPath(path, handler);
        this.exactPaths.add(this.normalizeSlashes(path));
        this.buildLengths();
        return this;
    }

    public synchronized HttpUnavailablePathHandler removePrefixPath(final String path) {
        this.pathHandler.removePrefixPath(path);
        this.prefixtPaths.remove(this.normalizeSlashes(path));
        this.buildLengths();
        return this;
    }

    public synchronized HttpUnavailablePathHandler removeExactPath(final String path) {
        this.pathHandler.removeExactPath(path);
        this.exactPaths.remove(this.normalizeSlashes(path));
        this.buildLengths();
        return this;
    }

    public synchronized HttpUnavailablePathHandler clearPaths() {
        this.pathHandler.clearPaths();
        this.prefixtPaths.clear();
        this.exactPaths.clear();
        this.lengths = new int[0];
        return this;
    }

    private void buildLengths() {
        final Set<Integer> lengths = new TreeSet<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return -o1.compareTo(o2);
            }
        });
        for (String p : prefixtPaths) {
            lengths.add(p.length());
        }

        int[] lengthArray = new int[lengths.size()];
        int pos = 0;
        for (int i : lengths) {
            lengthArray[pos++] = i;
        }
        this.lengths = lengthArray;
    }

    /**
     * Adds a '/' prefix to the beginning of a path if one isn't present and removes trailing slashes if any are present.
     *
     * @param path the path to normalize
     * @return a normalized (with respect to slashes) result
     */
    private String normalizeSlashes(final String path) {
        // prepare
        final StringBuilder builder = new StringBuilder(path);
        boolean modified = false;

        // remove all trailing '/'s except the first one
        while (builder.length() > 0 && builder.length() != 1 && PATH_SEPARATOR == builder.charAt(builder.length() - 1)) {
            builder.deleteCharAt(builder.length() - 1);
            modified = true;
        }

        // add a slash at the beginning if one isn't present
        if (builder.length() == 0 || PATH_SEPARATOR != builder.charAt(0)) {
            builder.insert(0, PATH_SEPARATOR);
            modified = true;
        }

        // only create string when it was modified
        if (modified) {
            return builder.toString();
        }

        return path;
    }

    /**
     * Simple handler to allow response code modification at runtime.
     *
     * @author baranowb
     *
     */
    private class CustomResponseCodeHandler implements HttpHandler {

        private int responseCode;

        public CustomResponseCodeHandler(int responseCode) {
            this.responseCode = responseCode;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.setResponseCode(this.responseCode);
            if (traceEnabled) {
                log.tracef("Setting response code %s for exchange %s", responseCode, exchange);
            }
        }
    }

}
