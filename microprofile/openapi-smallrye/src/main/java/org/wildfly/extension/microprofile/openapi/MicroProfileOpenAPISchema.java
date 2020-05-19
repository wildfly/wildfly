/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.openapi;

import java.util.Locale;

import org.jboss.as.clustering.controller.Schema;

/**
 * Enumeration of MicroProfile OpenAPI subsystem schema versions.
 * @author Paul Ferraro
 */
public enum MicroProfileOpenAPISchema implements Schema<MicroProfileOpenAPISchema> {

    VERSION_1_0(1, 0), // WildFly 19
    ;
    static final MicroProfileOpenAPISchema CURRENT = VERSION_1_0;

    private final int major;
    private final int minor;

    MicroProfileOpenAPISchema(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    @Override
    public int major() {
        return this.major;
    }

    @Override
    public int minor() {
        return this.minor;
    }

    @Override
    public String getNamespaceUri() {
        return String.format(Locale.ROOT, "urn:wildfly:%s:%d.%d", MicroProfileOpenAPIExtension.SUBSYSTEM_NAME, this.major, this.minor);
    }
}
