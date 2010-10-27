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

package org.jboss.as.services.path;

import java.io.File;

import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * {@link AbstractPathService} implementation for paths that are not relative
 * to other paths.
 *
 * @author Brian Stansberry
 */
public class AbsolutePathService extends AbstractPathService {

    private final String absolutePath;

    public static BatchServiceBuilder<String> addService(final String name, final String abstractPath, final BatchBuilder batchBuilder) {
        return addService(pathNameOf(name), abstractPath, batchBuilder);
    }

    public static BatchServiceBuilder<String> addService(final ServiceName sname, final String abstractPath, final BatchBuilder batchBuilder) {
        AbsolutePathService service = new AbsolutePathService(abstractPath);
        return batchBuilder.addService(sname, service);
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
