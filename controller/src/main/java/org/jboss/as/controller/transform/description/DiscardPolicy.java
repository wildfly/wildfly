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

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public interface DiscardPolicy {

    /**
     * Determine whether to discard the current model or operation.
     *
     * @param node the model or operation
     * @param address the current address
     * @param context the transformation context
     * @return {@code true} to discard, {@code false} otherwise
     */
    boolean discard(ModelNode node, PathAddress address, TransformationContext context);

    DiscardPolicy ALWAYS = new DiscardPolicy() {
        @Override
        public boolean discard(final ModelNode node, final PathAddress address, final TransformationContext context) {
            return true;
        }
    };

    DiscardPolicy NEVER = new DiscardPolicy() {
        @Override
        public boolean discard(final ModelNode node, final PathAddress address, final TransformationContext context) {
            return false;
        }
    };

}
