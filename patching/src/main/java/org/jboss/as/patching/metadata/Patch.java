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

package org.jboss.as.patching.metadata;

import java.util.List;

/**
 * The patch metadata.
 *
 * @author Emanuel Muckenhuber
 */
public interface Patch {

    public enum PatchType {
        CUMULATIVE("cumulative"),
        ONE_OFF("one-off"),
        ;

        private final String name;

        private PatchType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Get the unique patch ID.
     *
     * @return the patch id
     */
    String getPatchId();

    /**
     * Get the patch description.
     *
     * @return the patch description
     */
    String getDescription();

    /**
     * Get the patch type.
     *
     * @return the type of the patch
     */
    PatchType getPatchType();

    /**
     * Returns the identity which produced this patch.
     *
     * @return  identity which produced this patch
     */
    Identity getIdentity();

    /**
     * Get the resulting version of a CP or {@code null} for a one-off patch
     *
     * @return the resulting version
     */
    String getResultingVersion();

    /**
     * Get the versions this patch applies to.
     *
     * @return the versions the patch can be applied
     */
    List<String> getAppliesTo();

    /**
     * List of patch elements.
     *
     * @return  list of patch elements
     */
    List<PatchElement> getElements();

    /**
     * Get the content modifications.
     *
     * @return the modifications
     */
    List<ContentModification> getModifications();

}
