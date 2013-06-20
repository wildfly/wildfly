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

package org.jboss.as.patching.metadata;

import java.util.Collection;

/**
 * @author Emanuel Muckenhuber
 */
public interface UpgradeCondition {

    /**
     * Patch element provider name.
     *
     * @return  patch element provider name
     */
    String getName();

    /**
     * Get the patch type.
     *
     * @return the patch type
     */
    Patch.PatchType getPatchType();

    /**
     * List of the applied patch elements to this provider.
     *
     * @return  list of id's of the patch elements applied to this provider
     */
    Collection<String> getRequires();

    /**
     * Get a list of patch-ids, this patch is incompatible with.
     *
     * @return a list of incompatible patches
     */
    Collection<String> getIncompatibleWith();

}
