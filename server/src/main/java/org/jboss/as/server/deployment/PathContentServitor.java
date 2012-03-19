/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.server.deployment;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class PathContentServitor extends AbstractService<VirtualFile> {
    private final String unresolvedPath;
    private final String relativeTo;
    private final InjectedValue<PathManager> pathManagerValue = new InjectedValue<PathManager>();
    private volatile PathManager.Callback.Handle callbackHandle;

    static ServiceController<VirtualFile> addService(final ServiceTarget serviceTarget, final ServiceName serviceName, final String path, final String relativeTo, final ServiceVerificationHandler verificationHandler) {
        final PathContentServitor service = new PathContentServitor(path, relativeTo);
        ServiceBuilder<VirtualFile> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.pathManagerValue);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    private PathContentServitor(final String relativePath, final String relativeTo) {
        this.unresolvedPath = relativePath;
        this.relativeTo = relativeTo;
    }

    @Override
    public VirtualFile getValue() throws IllegalStateException, IllegalArgumentException {
        return VFS.getChild(resolvePath());
    }

    private String resolvePath() {
        return pathManagerValue.getValue().resolveRelativePathEntry(unresolvedPath, relativeTo);
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        if (relativeTo != null) {
            callbackHandle = pathManagerValue.getValue().registerCallback(relativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        }
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
    }


}
