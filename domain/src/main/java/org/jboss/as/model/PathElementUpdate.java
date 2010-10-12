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

package org.jboss.as.model;

/**
 * {@code PathElement} update.
 *
 * @author Emanuel Muckenhuber
 */
public class PathElementUpdate extends AbstractModelElementUpdate<PathElement> {

    private static final long serialVersionUID = 7203735088855065479L;
    private final String name;
    private final String path;
    private final String relativeTo;

    public PathElementUpdate(final String pathName, final String path, final String relativeTo) {
        if(pathName == null) {
            throw new IllegalArgumentException("null path name");
        }
        this.name = pathName;
        this.path = path;
        this.relativeTo = relativeTo;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    boolean isAbsolutePath() {
        return relativeTo == null;
    }

    /** {@inheritDoc} */
    public Class<PathElement> getModelElementType() {
        return PathElement.class;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(PathElement element) throws UpdateFailedException {
        element.setPath(path);
        element.setRelativeTo(relativeTo);
    }

    /** {@inheritDoc} */
    public AbstractModelElementUpdate<PathElement> getCompensatingUpdate(PathElement original) {
        return new PathElementUpdate(name, original.getPath(), original.getRelativeTo());
    }

}
