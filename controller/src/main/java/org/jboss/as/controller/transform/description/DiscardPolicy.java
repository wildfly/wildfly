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
 * Policy defining whether resource or operation transformations should be rejected.
 *
 * @author Emanuel Muckenhuber
 */
interface DiscardPolicy {

    /**
     * Determine whether to discard the current model or operation.
     *
     * @param node the model or operation
     * @param address the current address
     * @param context the transformation context
     * @return the discard type
     */
    DiscardType discard(ModelNode node, PathAddress address, TransformationContext context);

    DiscardPolicy ALWAYS = new DiscardPolicy() {
        @Override
        public DiscardType discard(final ModelNode node, final PathAddress address, final TransformationContext context) {
            return DiscardType.DISCARD_AND_WARN;
        }
    };

    DiscardPolicy REJECT = new DiscardPolicy() {
        @Override
        public DiscardType discard(ModelNode node, PathAddress address, TransformationContext context) {
            return DiscardType.REJECT_AND_WARN;
        }
    };

    DiscardPolicy NEVER = new DiscardPolicy() {
        @Override
        public DiscardType discard(final ModelNode node, final PathAddress address, final TransformationContext context) {
            return DiscardType.NEVER;
        }
    };

    DiscardPolicy SILENT = new DiscardPolicy() {
        @Override
        public DiscardType discard(ModelNode node, PathAddress address, TransformationContext context) {
            return DiscardType.SILENT;
        }
    };

    public enum DiscardType {
        /**
         * Don't discard the resource or operation.
         */
        NEVER,
        /**
         * Reject operations and only warn for resource transformations.
         */
        REJECT_AND_WARN,
        /**
         * Discard operations silently, but warn for resource transformations.
         */
        DISCARD_AND_WARN,
        /**
         * Discard silently.
         */
        SILENT,
        ;
    }

}
