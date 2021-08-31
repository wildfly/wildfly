/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import org.jboss.as.clustering.controller.Model;
import org.jboss.as.controller.ModelVersion;

/**
 * Enumerates the model versions for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public enum DistributableWebModel implements Model {

    /*
    List of unsupported versions commented out for reference purposes:

    VERSION_1_0_0(1, 0, 0), // WildFly 17
     */
    VERSION_2_0_0(2, 0, 0), // WildFly 18+, EAP 7.4
    ;
    public static final DistributableWebModel CURRENT = VERSION_2_0_0;

    private final ModelVersion version;

    DistributableWebModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
