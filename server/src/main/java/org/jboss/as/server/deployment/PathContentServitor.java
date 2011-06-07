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
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import java.io.File;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class PathContentServitor extends AbstractService<VirtualFile> {
    private final String unresolvedPath;
    private final InjectedValue<String> relativePathValue = new InjectedValue<String>();

    static ServiceController<VirtualFile> addService(final ServiceTarget serviceTarget, final ServiceName serviceName, final String path, final ServiceName relativeToServiceName, final ServiceVerificationHandler verificationHandler) {
        final PathContentServitor service = new PathContentServitor(path);
        ServiceBuilder<VirtualFile> builder = serviceTarget.addService(serviceName, service);
        if (relativeToServiceName != null) {
             builder.addDependency(relativeToServiceName, String.class, service.relativePathValue);
        }
        builder.addListener(verificationHandler);
        return builder.install();
    }

    private PathContentServitor(final String relativePath) {
        this.unresolvedPath = relativePath;
    }

    @Override
    public VirtualFile getValue() throws IllegalStateException, IllegalArgumentException {
        return VFS.getChild(resolvePath());
    }

    private String resolvePath() {
        String base = relativePathValue.getOptionalValue();
        if (base != null) {
            base = base.endsWith(File.separator) ? base.substring(0, base.length() -1) : base;
            return base + File.separatorChar + unresolvedPath;
        } else {
            return unresolvedPath;
        }

    }
}
