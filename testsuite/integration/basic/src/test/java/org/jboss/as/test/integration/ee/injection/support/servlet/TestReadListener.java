/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.injection.support.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ReadListener;

import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor.Interception;

public class TestReadListener implements ReadListener {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final TestHttpUpgradeHandler handler;

    public TestReadListener(TestHttpUpgradeHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("No upgrade handler set");
        }
        this.handler = handler;
    }

    @Override
    public void onDataAvailable() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(handler.getWebConnection().getInputStream()));
        while ((reader.readLine()) != null) {
            // read the input stream
        }
        OutputStream out = handler.getWebConnection().getOutputStream();
        write(out, "isPostConstructCallbackInvoked: " + handler.isPostConstructCallbackInvoked() + LINE_SEPARATOR);
        write(out, "isInjectionOk: " + handler.isInjectionOk() + LINE_SEPARATOR);
        write(out, "isInterceptorInvoked: " + isInterceptorInvoked() + LINE_SEPARATOR);
        write(out, "END" + LINE_SEPARATOR);
        out.flush();
    }

    private boolean isInterceptorInvoked() {
        List<Interception> interceptions = ComponentInterceptor.getInterceptions();
        return interceptions != null && (interceptions.size() == 1) && interceptions.get(0).getMethodName().equals("init");
    }

    private void write(OutputStream out, String text) throws IOException {
        out.write(text.getBytes());
    }

    @Override
    public void onAllDataRead() throws IOException {
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

}