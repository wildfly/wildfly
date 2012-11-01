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

package org.jboss.as.patching.tool;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.runner.PatchingException;

import java.io.File;
import java.io.InputStream;

/**
 * Builder to create common patch operations.
 *
 * @author Emanuel Muckenhuber
 */
public interface PatchOperationBuilder extends PatchTool.ContentPolicyBuilder {

    /**
     * Execute this operation on a target.
     *
     * @param target the target
     * @throws PatchingException
     */
    void execute(PatchOperationTarget target) throws PatchingException;

    public class Factory {

        private Factory() {
            //
        }

        /**
         * Create the rollback builder.
         *
         * @param patchId the patch-id to rollback
         * @param restoreConfiguration whether to restore the configuration
         * @return the operation builder
         */
        public static PatchOperationBuilder rollback(final String patchId, final boolean restoreConfiguration) {
            return new AbstractOperationBuilder() {
                @Override
                public void execute(PatchOperationTarget target) throws PatchingException {
                    target.rollback(patchId, this, restoreConfiguration);
                }
            };
        }

        /**
         * Patch a standalone instance.
         *
         * @param file the patch file
         * @return the operation builder
         */
        public static PatchOperationBuilder patch(final File file) {
            return new AbstractOperationBuilder() {
                @Override
                public void execute(PatchOperationTarget target) throws PatchingException {
                    target.applyPatch(file, this);
                }
            };
        }

    }


    abstract class AbstractOperationBuilder extends ContentPolicyBuilderImpl implements PatchOperationBuilder {

    }

}
