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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

/**
 * A {@link URLStreamHandlerService} for 'webbundle://' locations.
 *
 * @author thomas.diesler@jboss.com
 * @since 30-Nov-2012
 */
public final class WebBundleURLStreamHandler extends AbstractURLStreamHandlerService {

    public static ServiceRegistration registerService(BundleContext context) {
        WebBundleURLStreamHandler handler = new WebBundleURLStreamHandler();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(URLConstants.URL_HANDLER_PROTOCOL, WebExtension.WEBBUNDLE_PROTOCOL);
        return context.registerService(URLStreamHandlerService.class.getName(), handler, props);
    }

    // Hide ctor
    private WebBundleURLStreamHandler() {
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        String path = url.getPath();
        URL nested = new URL(path);
        return nested.openConnection();
    }
}
