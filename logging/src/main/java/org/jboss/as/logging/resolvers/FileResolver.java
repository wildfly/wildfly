/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.resolvers;

import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.logging.LoggingMessages;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Used to resolve an absolute path for a file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FileResolver implements ModelNodeResolver<String> {

    public static final FileResolver INSTANCE = new FileResolver();

    private FileResolver() {
    }

    @Override
    public String resolveValue(final OperationContext context, final ModelNode value) throws OperationFailedException {
        final ModelNode pathNode = PATH.resolveModelAttribute(context, value);
        final ModelNode relativeToNode = RELATIVE_TO.resolveModelAttribute(context, value);
        String path = pathNode.asString();
        String result = path;
        if (relativeToNode.isDefined()) {
            result = resolve(context, relativeToNode.asString(), path);
        }
        if (result == null) {
            throw LoggingMessages.MESSAGES.pathManagerServiceNotStarted();
        }
        return result;
    }

    /**
     * Resolves the path based on the relative to and the path. May return {@code null} if the service is not up.
     *
     * @param context        the operation context.
     * @param relativeToPath the relative to path, may be {@code null}.
     * @param path           the path to append to the relative to path or the absolute path if the relative to path is
     *                       {@code null}.
     *
     * @return the full path or {@code null} if the services were not started.
     */
    public static String resolvePath(final OperationContext context, final String relativeToPath, final String path) {
        return INSTANCE.resolve(context, relativeToPath, path);
    }

    private String resolve(final OperationContext context, final String relativeToPath, final String path) {
        @SuppressWarnings("unchecked")
        final ServiceController<PathManager> controller = (ServiceController<PathManager>) context.getServiceRegistry(true).getService(PathManagerService.SERVICE_NAME);
        if (controller == null) {
            return null;
        }
        return controller.getValue().resolveRelativePathEntry(path, relativeToPath);
    }
}
