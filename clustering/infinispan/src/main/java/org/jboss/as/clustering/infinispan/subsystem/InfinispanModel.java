/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ModelVersion;

/**
 * Enumerates the supported model versions.
 * @author Paul Ferraro
 */
public enum InfinispanModel {

    VERSION_1_3_0(1, 3, 0),
    VERSION_1_4_0(1, 4, 0),
    VERSION_1_4_1(1, 4, 1),
    VERSION_2_0_0(2, 0, 0),
    VERSION_3_0_0(3, 0, 0),
    ;
    static final InfinispanModel CURRENT = VERSION_3_0_0;

    private final ModelVersion version;

    private InfinispanModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    public ModelVersion getVersion() {
        return this.version;
    }

    /**
     * Indicates whether this model is more recent than the specified version and thus requires transformation
     * @param version a model version
     * @return true this this model is more recent than the specified version, false otherwise
     */
    public boolean requiresTransformation(ModelVersion version) {
        return ModelVersion.compare(this.version, version) < 0;
    }
}
