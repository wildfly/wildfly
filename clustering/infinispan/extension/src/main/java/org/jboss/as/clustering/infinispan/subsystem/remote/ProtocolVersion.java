/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Locale;

/**
 * Temporary copy of {@link org.infinispan.client.hotrod.ProtocolVersion} until Infinispan 9.x upgrade.
 * TODO remove the class completely with 9.x upgrade
 *
 * @author Radoslav Husar
 */
public enum ProtocolVersion {

    PROTOCOL_VERSION_10(1, 0),
    PROTOCOL_VERSION_11(1, 1),
    PROTOCOL_VERSION_12(1, 2),
    PROTOCOL_VERSION_13(1, 3),
    PROTOCOL_VERSION_20(2, 0),
    PROTOCOL_VERSION_21(2, 1),
    PROTOCOL_VERSION_22(2, 2),
    PROTOCOL_VERSION_23(2, 3),
    PROTOCOL_VERSION_24(2, 4),
    PROTOCOL_VERSION_25(2, 5),
    ;

    public static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = PROTOCOL_VERSION_25;

    private final String version;

    ProtocolVersion(int major, int minor) {
        version = String.format(Locale.ROOT, "%d.%d", major, minor);
    }

    @Override
    public String toString() {
        return version;
    }
}
