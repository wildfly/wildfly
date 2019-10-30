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

import io.undertow.UndertowLogger;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.util.DateUtils;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;
import org.jboss.vfs.VirtualFile;
import org.xnio.FileAccess;
import org.xnio.IoUtils;
import org.xnio.Pooled;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class VirtualFileResource implements Resource {

    private final File resourceManagerRoot;
    private final VirtualFile file;
    private final String path;

    public VirtualFileResource(File resourceManagerRoot, final VirtualFile file, String path) {
        this.resourceManagerRoot = resourceManagerRoot;
        this.file = file;
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Date getLastModified() {
        return new Date(file.getLastModified());
    }

    @Override
    public String getLastModifiedString() {
        final Date lastModified = getLastModified();
        if (lastModified == null) {
            return null;
        }
        return DateUtils.toDateString(lastModified);
    }

    @Override
    public ETag getETag() {
        return null;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public List<Resource> list() {
        final List<Resource> resources = new ArrayList<Resource>();
        for (VirtualFile child : file.getChildren()) {
            resources.add(new VirtualFileResource(resourceManagerRoot, child, path));
        }
        return resources;
    }

    @Override
    public String getContentType(final MimeMappings mimeMappings) {
        final String fileName = file.getName();
        int index = fileName.lastIndexOf('.');
        if (index != -1 && index != fileName.length() - 1) {
            return mimeMappings.getMimeType(fileName.substring(index + 1));
        }
        return null;
    }

    @Override
    public void serve(final Sender sender, final HttpServerExchange exchange, final IoCallback callback) {
        abstract class BaseFileTask implements Runnable {
            protected volatile FileChannel fileChannel;

            protected boolean openFile() {
                try {
                    fileChannel = exchange.getConnection().getWorker().getXnio().openFile(file.getPhysicalFile(), FileAccess.READ_ONLY);
                } catch (FileNotFoundException e) {
                    exchange.setResponseCode(404);
                    callback.onException(exchange, sender, e);
                    return false;
                } catch (IOException e) {
                    exchange.setResponseCode(500);
                    callback.onException(exchange, sender, e);
                    return false;
                }
                return true;
            }
        }

        class ServerTask extends BaseFileTask implements IoCallback {

            private Pooled<ByteBuffer> pooled;

            @Override
            public void run() {
                if (fileChannel == null) {
                    if (!openFile()) {
                        return;
                    }
                    pooled = exchange.getConnection().getBufferPool().allocate();
                }
                if (pooled != null) {
                    ByteBuffer buffer = pooled.getResource();
                    try {
                        buffer.clear();
                        int res = fileChannel.read(buffer);
                        if (res == -1) {
                            //we are done
                            pooled.free();
                            IoUtils.safeClose(fileChannel);
                            callback.onComplete(exchange, sender);
                            return;
                        }
                        buffer.flip();
                        sender.send(buffer, this);
                    } catch (IOException e) {
                        onException(exchange, sender, e);
                    }
                }

            }

            @Override
            public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                } else {
                    run();
                }
            }

            @Override
            public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
                UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
                if (pooled != null) {
                    pooled.free();
                    pooled = null;
                }
                IoUtils.safeClose(fileChannel);
                if (!exchange.isResponseStarted()) {
                    exchange.setResponseCode(500);
                }
                callback.onException(exchange, sender, exception);
            }
        }

        class TransferTask extends BaseFileTask {
            @Override
            public void run() {
                if (!openFile()) {
                    return;
                }

                sender.transferFrom(fileChannel, new IoCallback() {
                    @Override
                    public void onComplete(HttpServerExchange exchange, Sender sender) {
                        try {
                            IoUtils.safeClose(fileChannel);
                        } finally {
                            callback.onComplete(exchange, sender);
                        }
                    }

                    @Override
                    public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
                        try {
                            IoUtils.safeClose(fileChannel);
                        } finally {
                            callback.onException(exchange, sender, exception);
                        }
                    }
                });
            }
        }

        BaseFileTask task = new TransferTask();
        if (exchange.isInIoThread()) {
            exchange.dispatch(task);
        } else {
            task.run();
        }
    }

    @Override
    public Long getContentLength() {
        return file.getSize();
    }

    @Override
    public String getCacheKey() {
        return file.toString();
    }

    @Override
    public File getFile() {
        try {
            return file.getPhysicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getResourceManagerRoot() {
        return resourceManagerRoot;
    }

    @Override
    public URL getUrl() {
        try {
            return file.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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

}
