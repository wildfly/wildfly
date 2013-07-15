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

package org.jboss.as.patching.generator;

import java.util.Set;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.Patch;

/**
 * @author Emanuel Muckenhuber
 */
public interface PatchElementConfig {

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
     * Get the layer name.
     *
     * @return the layer name
     */
    String getLayerName();

    /**
     * Get the patch type.
     *
     * @return the type of the patch
     */
    Patch.PatchType getPatchType();

    /**
     * Gets the modifications specifically specified in the patch config, if the config doesn't specify
     * {@link PatchConfig#isGenerateByDiff() generating the modifications by differencing the two distributions}.
     *
     * @return the modified content items
     */
    Set<ContentItem> getSpecifiedContent();

}
