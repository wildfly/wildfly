/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * HTTP integration test support.
 *
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2011
 */
public final class HttpTestSupport {

    // Hide ctor
    private HttpTestSupport() {
    }

    /**
     * Execute a HTTP request.
     */
    public static String getHttpResponse(String host, int port, String reqpath) throws IOException {
        URL url = new URL("http://" + host + ":" + port + reqpath);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = br.readLine();
        br.close();
        return line;
    }

    /**
     * Execute a HTTP request with a given timeout in milliseconds.
     */
    public static String getHttpResponse(String host, int port, String reqpath, int timeout) throws IOException {
        int fraction = 200;
        String line = null;
        IOException lastException = null;
        while (line == null && 0 < (timeout -= fraction)) {
            try {
                line = getHttpResponse(host, port, reqpath);
            } catch (IOException ex) {
                lastException = ex;
                try {
                    Thread.sleep(fraction);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
        if (line == null && lastException != null) {
            throw lastException;
        }
        return line;
    }
}