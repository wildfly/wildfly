/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor.Interception;

public class TestReadListener implements ReadListener {

    private final TestHttpUpgradeHandler handler;

    public TestReadListener(TestHttpUpgradeHandler handler) {
        this.handler = checkNotNullParam("handler", handler);
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
                String data = new String(b, 0, len, StandardCharsets.UTF_8);
                if (data.endsWith("#")) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(handler.getWebConnection().getOutputStream(), StandardCharsets.UTF_8));
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
