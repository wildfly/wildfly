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

package org.jboss.as.patching.tool;

import java.io.File;
import java.io.IOException;

import org.jboss.dmr.ModelNode;

/**
 * Builder to create common patch operations.
 *
 * @author Emanuel Muckenhuber
 */
public interface PatchOperationBuilder extends PatchTool.ContentPolicyBuilder {

    /**
     * Execute this operation on a target.
     *
     * @return a ModelNode describing the outcome of the execution
     * @param target the target
     * @throws IOException if an I/O error occurs while executing the operation.
     */
    ModelNode execute(PatchOperationTarget target) throws IOException;

    public class Factory {

        private Factory() {
            //
        }

        /**
         * Get the current patch info.
         *
         * @return the patch info
         */
        public static PatchOperationBuilder info() {
            return new AbstractOperationBuilder() {
                @Override
                public ModelNode execute(PatchOperationTarget target) throws IOException {
                    return target.info();
                }
            };
        }

        /**
         * Create the rollback builder.
         *
         * @param patchId the patch-id to rollback
         * @param rollbackTo rollback all one off patches until the given patch-id
         * @param restoreConfiguration whether to restore the configuration
         * @return the operation builder
         */
        public static PatchOperationBuilder rollback(final String patchId, final boolean rollbackTo, final boolean restoreConfiguration) {
            return new AbstractOperationBuilder() {
                @Override
                public ModelNode execute(PatchOperationTarget target) throws IOException {
                    return target.rollback(patchId, this, rollbackTo, restoreConfiguration);
                }
            };
        }

        /**
         * Create a builder to rollback the last applied patch.
         *
         * @param restoreConfiguration whether to restore the configuration
         * @return the operation builder
         */
        public static PatchOperationBuilder rollbackLast(final boolean restoreConfiguration) {
            return new AbstractOperationBuilder() {
                @Override
                public ModelNode execute(PatchOperationTarget target) throws IOException {
                    return target.rollbackLast(this, restoreConfiguration);
                }
            };
        }

        /**
         * Create a patch builder.
         *
         * @param file the patch file
         * @return the operation builder
         */
        public static PatchOperationBuilder patch(final File file) {
            return new AbstractOperationBuilder() {
                @Override
                public ModelNode execute(PatchOperationTarget target) throws IOException {
                    return target.applyPatch(file, this);
                }
            };
        }

    }


    abstract class AbstractOperationBuilder extends ContentPolicyBuilderImpl implements PatchOperationBuilder {

    }

}
