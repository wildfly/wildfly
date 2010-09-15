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

package org.jboss.as.logging;

import javax.xml.namespace.QName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractFileHandlerElement<E extends AbstractFileHandlerElement<E>> extends AbstractHandlerElement<E> {

    private static final long serialVersionUID = -9214638141059862173L;

    private String fileName;
    private boolean append = true;

    protected AbstractFileHandlerElement(final String name, final QName elementName) {
        super(name, elementName);
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isAppend() {
        return append;
    }

    void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setAppend(final boolean append) {
        this.append = append;
    }
}
