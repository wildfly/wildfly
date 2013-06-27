/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.installation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.runner.PatchUtils;

/**
 * Immutable layer info.
 *
 * @author Emanuel Muckenhuber
 */
public class LayerInfo implements Layer, AddOn {

    private final String name;
    private final TargetInfo targetInfo;
    private final DirectoryStructure structure;

    public LayerInfo(String name, TargetInfo targetInfo, DirectoryStructure structure) {
        this.name = name;
        this.targetInfo = targetInfo;
        this.structure = structure;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DirectoryStructure getDirectoryStructure() {
        return structure;
    }

    @Override
    public TargetInfo loadTargetInfo() {
        return targetInfo;
    }

    static class TargetInfoImpl implements TargetInfo {

        private final Properties properties;
        private final String cumulativeID;
        private final List<String> patches;
        private final DirectoryStructure structure;

        TargetInfoImpl(Properties properties, String cumulativeID, List<String> patches, DirectoryStructure structure) {
            this.properties = properties;
            this.cumulativeID = cumulativeID;
            this.patches = Collections.unmodifiableList(patches);
            this.structure = structure;
        }

        @Override
        public String getCumulativePatchID() {
            return cumulativeID;
        }

        @Override
        public List<String> getPatchIDs() {
            return patches;
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public DirectoryStructure getDirectoryStructure() {
            return structure;
        }
    }

    static TargetInfo loadTargetInfoFromDisk(final DirectoryStructure structure) throws IOException {
        final Properties properties = PatchUtils.loadProperties(structure.getInstallationInfo());
        return loadTargetInfo(properties, structure);
    }

    public static TargetInfo loadTargetInfo(final Properties properties, final DirectoryStructure structure) {
        final String ref = PatchUtils.readRef(properties, Constants.CUMULATIVE);
        final List<String> patches = PatchUtils.readRefs(properties);
        return new TargetInfoImpl(properties, ref, patches, structure);
    }

}
