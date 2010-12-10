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
 * An update which applies to a jvm options list.
 *
 * @author <a href="mailto:kkhan@redhat.com">Kabir Khan</a>
 */
public abstract class AbstractJvmOptionsUpdate extends AbstractModelElementUpdate<JvmOptionsElement> {

    private static final long serialVersionUID = -988127260524571932L;

    /**
     * Construct a new instance.
     */
    protected AbstractJvmOptionsUpdate() {
    }

    /** {@inheritDoc} */
    @Override
    public final Class<JvmOptionsElement> getModelElementType() {
        return JvmOptionsElement.class;
    }

    /** {@inheritDoc} */
    @Override
    protected abstract void applyUpdate(JvmOptionsElement element) throws UpdateFailedException;

    /** {@inheritDoc} */
    @Override
    public abstract AbstractJvmOptionsUpdate getCompensatingUpdate(JvmOptionsElement original);
}
