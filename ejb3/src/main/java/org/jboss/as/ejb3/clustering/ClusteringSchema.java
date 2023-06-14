/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.clustering;

import java.util.List;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * @author Paul Ferraro
 */
public enum ClusteringSchema implements IntVersionSchema<ClusteringSchema> {

    VERSION_1_0(1, 0),
    VERSION_1_1(1, 1),
    ;
    static final ClusteringSchema CURRENT = VERSION_1_1;

    private final VersionedNamespace<IntVersion, ClusteringSchema> namespace;

    ClusteringSchema(int major, int minor) {
        this.namespace = IntVersionSchema.createURN(List.of(this.getLocalName()), new IntVersion(major, minor));
    }

    @Override
    public String getLocalName() {
        return "clustering";
    }

    @Override
    public VersionedNamespace<IntVersion, ClusteringSchema> getNamespace() {
        return this.namespace;
    }
}
