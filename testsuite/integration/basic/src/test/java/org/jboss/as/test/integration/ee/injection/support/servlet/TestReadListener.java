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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor.Interception;

public class TestReadListener implements ReadListener {

    private final TestHttpUpgradeHandler handler;

    public TestReadListener(TestHttpUpgradeHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("No upgrade handler set");
        }
        this.handler = handler;
    }

    @Override
    public void onDataAvailable() throws IOException {

        ServletInputStream input = handler.getWebConnection().getInputStream();

        int len = -1;
        byte[] b = new byte[1024];

        if (input.isReady()) {
            // Expected data is "dummy request#"
            len = input.read(b);
            if (len > 0) {
                String data = new String(b, 0, len);
                if (data.endsWith("#")) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(handler.getWebConnection().getOutputStream()));
                    writeLine(writer, "isPostConstructCallbackInvoked: " + handler.isPostConstructCallbackInvoked());
                    writeLine(writer, "isInjectionOk: " + handler.isInjectionOk());
                    writeLine(writer, "isInterceptorInvoked: " + isInterceptorInvoked());
                    writeLine(writer, "END");
                    writer.flush();
                }
            }
        }
    }

    private boolean isInterceptorInvoked() {
        List<Interception> interceptions = ComponentInterceptor.getInterceptions();
        return interceptions != null && (interceptions.size() == 1) && interceptions.get(0).getMethodName().equals("init");
    }

    private void writeLine(BufferedWriter writer, String text) throws IOException {
        writer.write(text);
        writer.newLine();
    }

    @Override
    public void onAllDataRead() throws IOException {
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }


}