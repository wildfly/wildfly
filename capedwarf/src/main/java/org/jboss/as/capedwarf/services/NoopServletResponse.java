/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.services;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Execute servlet from an async invocation.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class NoopServletResponse implements HttpServletResponse {

    private static ServletOutputStream NOOP_SOS = new NoopServletOutputStream();
    private static PrintWriter NOOP_PW = new PrintWriter(new NoopOutputStream());

    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public String getContentType() {
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return NOOP_SOS;
    }

    public PrintWriter getWriter() throws IOException {
        return NOOP_PW;
    }

    public void setCharacterEncoding(String charset) {
    }

    public void setContentLength(int len) {
    }

    public void setContentType(String type) {
    }

    public void setBufferSize(int size) {
    }

    public int getBufferSize() {
        return 0;
    }

    public void flushBuffer() throws IOException {
    }

    public void resetBuffer() {
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
    }

    public void setLocale(Locale loc) {
    }

    public Locale getLocale() {
        return Locale.getDefault();
    }

    private static class NoopOutputStream extends OutputStream {
        public void write(int b) throws IOException {
        }
    }

    private static class NoopServletOutputStream extends ServletOutputStream {
        public void write(int b) throws IOException {
        }
    }

    public void addCookie(Cookie cookie) {
    }

    public boolean containsHeader(String name) {
        return false;
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeUrl(String url) {
        return url;
    }

    public String encodeRedirectUrl(String url) {
        return url;
    }

    public void sendError(int sc, String msg) throws IOException {
    }

    public void sendError(int sc) throws IOException {
    }

    public void sendRedirect(String location) throws IOException {
    }

    public void setDateHeader(String name, long date) {
    }

    public void addDateHeader(String name, long date) {
    }

    public void setHeader(String name, String value) {
    }

    public void addHeader(String name, String value) {
    }

    public void setIntHeader(String name, int value) {
    }

    public void addIntHeader(String name, int value) {
    }

    public void setStatus(int sc) {
    }

    public void setStatus(int sc, String sm) {
    }

    public int getStatus() {
        return 0;
    }

    public String getHeader(String name) {
        return null;
    }

    public Collection<String> getHeaders(String name) {
        return null;
    }

    public Collection<String> getHeaderNames() {
        return Collections.emptySet();
    }
}

