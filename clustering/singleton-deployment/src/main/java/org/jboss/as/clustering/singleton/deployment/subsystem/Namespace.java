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

package org.jboss.as.clustering.singleton.deployment.subsystem;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * @author Paul Ferraro
 */
public enum Namespace {

    VERSION_1_0(1, 0, new SingletonDeploymentSubsystemXMLReader_1_0())
    ;

    private static final String URN_PATTERN = "urn:jboss:domain:%s:%d.%d";

    public static final Namespace CURRENT = VERSION_1_0;

    private final int major;
    private final int minor;
    private final XMLElementReader<List<ModelNode>> reader;

    Namespace(int major, int minor, XMLElementReader<List<ModelNode>> reader) {
        this.major = major;
        this.minor = minor;
        this.reader = reader;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUri() {
        return String.format(URN_PATTERN, SingletonDeploymentExtension.SUBSYSTEM_NAME, this.major, this.minor);
    }

    public XMLElementReader<List<ModelNode>> getXMLReader() {
        return this.reader;
    }
}
