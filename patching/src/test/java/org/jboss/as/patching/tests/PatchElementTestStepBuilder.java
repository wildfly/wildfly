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

package org.jboss.as.patching.tests;

import java.io.File;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.PatchElementBuilder;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchElementTestStepBuilder extends AbstractPatchTestBuilder<PatchElementTestStepBuilder> {

    private final String patchId;
    private final PatchElementBuilder builder;
    private final PatchingTestStepBuilder parent;

    public PatchElementTestStepBuilder(String patchId, PatchElementBuilder builder, PatchingTestStepBuilder parent) {
        this.patchId = patchId;
        this.builder = builder;
        this.parent = parent;
    }

    @Override
    protected String getPatchId() {
        return patchId;
    }

    @Override
    protected File getPatchDir() {
        return parent.getPatchDir();
    }

    public PatchingTestStepBuilder getParent() {
        return parent;
    }

    @Override
    protected PatchElementTestStepBuilder internalAddModification(ContentModification modification) {
        builder.addContentModification(modification);
        return returnThis();
    }

    @Override
    protected PatchElementTestStepBuilder returnThis() {
        return this;
    }

}
