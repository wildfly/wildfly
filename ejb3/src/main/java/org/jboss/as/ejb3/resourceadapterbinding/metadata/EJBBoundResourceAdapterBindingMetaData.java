/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.resourceadapterbinding.metadata;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Metadata for resource adapter binding of message driven beans
 *
 * @author <a href="mailto:robert.panzer@wincor-nixdorf.com">Robert Panzer</a>
 *
 */
public class EJBBoundResourceAdapterBindingMetaData extends AbstractEJBBoundMetaData {

    private static final long serialVersionUID = -5981839317495286524L;

    private String resourceAdapterName;

    public String getResourceAdapterName() {
        return resourceAdapterName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        this.resourceAdapterName = resourceAdapterName;
    }

}
