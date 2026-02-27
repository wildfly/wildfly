/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class OtlpHttpServlet extends HttpServlet {
    @Override
    protected abstract void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException;

    protected byte[] readAllBytes(InputStream in) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    protected void setSuccessResponse(HttpServletResponse resp, byte[] responseBytes) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/x-protobuf");
        resp.setContentLength(responseBytes.length);
        resp.getOutputStream().write(responseBytes);
    }
}
