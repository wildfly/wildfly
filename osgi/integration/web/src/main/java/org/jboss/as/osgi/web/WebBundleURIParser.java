/*
 * #%L
 * JBossOSGi Framework
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.as.osgi.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Manifest;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Parser for 'webbundle://' locations.
 *
 * @author thomas.diesler@jboss.com
 * @since 30-Nov-2012
 */
public final class WebBundleURIParser {

    // Hide ctor
    private WebBundleURIParser() {
    }

    /**
     * Parse a bundle location as a webbundle URI and generate a
     * Manifest from it
     *
     * @param The bundle location
     * @return A valid OSGi Manifest or null
     */
    public static Manifest parse(String location) {

        if (!location.startsWith(WebExtension.WEBBUNDLE_PREFIX))
            return null;

        URI uri;
        try {
            uri = new URI(location);
        } catch (URISyntaxException ex) {
            return null;
        }

        // If the scheme specific part is an URI we use that
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        try {
            uri = new URI(schemeSpecificPart);
        } catch (URISyntaxException ex) {
            // ignore
        }

        String symbolicName = null;
        String contextPath = null;

        String query = uri.getQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                int valueIndex = part.indexOf("=") + 1;
                if (part.startsWith(Constants.BUNDLE_SYMBOLICNAME)) {
                    symbolicName = part.substring(valueIndex);
                } else if (part.startsWith(WebExtension.WEB_CONTEXTPATH)) {
                    contextPath = part.substring(valueIndex);
                }
            }
        }

        // Derive the context path from the URI
        if (contextPath == null) {
            contextPath = uri.getHost();
            if (contextPath.endsWith(".war")) {
                contextPath = contextPath.substring(0, contextPath.length() - 4);
            } else {
                int index = contextPath.indexOf(".jar");
                if (index > 0) {
                    contextPath = contextPath.substring(0, index);
                }
            }
        }

        OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
        builder.addBundleManifestVersion(2);
        builder.addBundleSymbolicName(symbolicName);
        builder.addManifestHeader(WebExtension.WEB_CONTEXTPATH, contextPath);
        builder.addImportPackages(WebServlet.class, Servlet.class, HttpServlet.class);
        builder.addImportPackages(Bundle.class);
        builder.addBundleClasspath("WEB-INF/classes");
        Manifest manifest = builder.getManifest();

        return manifest;
    }
}
