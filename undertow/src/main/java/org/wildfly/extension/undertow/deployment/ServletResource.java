/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.deployment;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.RangeAwareResource;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 *
 * Resource implementation that wraps an underlying resource, and overrides the list() method to take overlays into account.
 *
 * @author Stuart Douglas
 */
public class ServletResource implements Resource, RangeAwareResource {

    private final ServletResourceManager resourceManager;
    private final Resource underlying;

    public ServletResource(ServletResourceManager resourceManager, Resource underlying) {
        this.resourceManager = resourceManager;
        this.underlying = underlying;
    }

    @Override
    public String getPath() {
        return underlying.getPath();
    }

    @Override
    public Date getLastModified() {
        return underlying.getLastModified();
    }

    @Override
    public String getLastModifiedString() {
        return underlying.getLastModifiedString();
    }

    @Override
    public ETag getETag() {
        return underlying.getETag();
    }

    @Override
    public String getName() {
        return underlying.getName();
    }

    @Override
    public boolean isDirectory() {
        return underlying.isDirectory();
    }

    @Override
    public List<Resource> list() {
        return resourceManager.list(getPath());
    }

    @Override
    public String getContentType(MimeMappings mimeMappings) {
        return underlying.getContentType(mimeMappings);
    }

    @Override
    public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
        underlying.serve(sender, exchange, completionCallback);
    }

    @Override
    public Long getContentLength() {
        return underlying.getContentLength();
    }

    @Override
    public String getCacheKey() {
        return underlying.getCacheKey();
    }

    @Override
    public File getFile() {
        return underlying.getFile();
    }

    @Override
    public File getResourceManagerRoot() {
        return underlying.getResourceManagerRoot();
    }

    @Override
    public URL getUrl() {
        return underlying.getUrl();
    }


    public Path getResourceManagerRootPath() {
        return getResourceManagerRoot().toPath();
    }

    public Path getFilePath() {
        if(getFile() == null) {
            return null;
        }
        return getFile().toPath();
    }

    @Override
    public void serveRange(Sender sender, HttpServerExchange exchange, long start, long end, IoCallback completionCallback) {
        ((RangeAwareResource) underlying).serveRange(sender, exchange, start, end, completionCallback);
    }

    @Override
    public boolean isRangeSupported() {
        if(underlying instanceof RangeAwareResource) {
            return ((RangeAwareResource) underlying).isRangeSupported();
        }
        return false;
    }
}
