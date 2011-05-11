/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.services.path;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;

/**
 * {@link AbstractPathService} implementation for paths that are not relative
 * to other paths.
 *
 * @author Brian Stansberry
 */
public class AbsolutePathService extends AbstractPathService {

    private final String absolutePath;

    public static ServiceController<String> addService(final String name, final String abstractPath, final ServiceTarget serviceTarget) {
        return addService(pathNameOf(name), abstractPath, serviceTarget);
    }

    public static ServiceController<String> addService(final ServiceName sname, final String abstractPath, final ServiceTarget serviceTarget) {
        AbsolutePathService service = new AbsolutePathService(abstractPath);
        return serviceTarget.addService(sname, service).install();
    }

    public static void addService(final ServiceName name, final ModelNode element, final ServiceTarget serviceTarget) {
        final String path = element.require(PATH).asString();
        addService(name, path, serviceTarget);
    }

    public AbsolutePathService(final String abstractPath) {
        if (abstractPath == null) {
            throw new IllegalArgumentException("abstractPath is null");
        }
        if (abstractPath.length() == 0) {
            throw new IllegalArgumentException("abstractPath is empty");
        }
        // Use File.getAbsolutePath() to make relative paths absolute
        File f = new File(abstractPath);
        absolutePath = f.getAbsolutePath();
    }

    @Override
    protected String resolvePath() {
        return absolutePath;
    }

}
