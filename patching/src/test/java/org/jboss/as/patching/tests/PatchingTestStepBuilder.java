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

import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;

import org.jboss.as.patching.metadata.Builder;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.PatchElementBuilder;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchingTestStepBuilder extends AbstractPatchTestBuilder<PatchingTestStepBuilder> implements Builder {

    private String patchId;
    private final PatchBuilder builder;
    private final PatchingTestBuilder testBuilder;
    private final File root;

    public PatchingTestStepBuilder(PatchingTestBuilder testBuilder) {
        this.testBuilder = testBuilder;
        this.builder = PatchBuilder.create();
        this.root = new File(testBuilder.getRoot(), randomString());
        this.root.mkdir();
    }

    @Override
    protected String getPatchId() {
        return patchId;
    }

    protected File getPatchDir() {
        return root;
    }

    public PatchingTestStepBuilder setPatchId(String id) {
        this.patchId = id;
        builder.setPatchId(id);
        return returnThis();
    }

    public PatchingTestStepBuilder upgradeIdentity(String version, String resultingVersion) {
        builder.upgradeIdentity(AbstractPatchingTest.PRODUCT_NAME, version, resultingVersion);
        return returnThis();
    }

    public PatchingTestStepBuilder oneOffPatchIdentity(String version) {
        builder.oneOffPatchIdentity(AbstractPatchingTest.PRODUCT_NAME, version);
        return returnThis();
    }

    public PatchElementTestStepBuilder upgradeElement(String patchId, String layerName, boolean isAddon) {
        final PatchElementBuilder elementBuilder = builder.upgradeElement(patchId, layerName, isAddon);
        return new PatchElementTestStepBuilder(patchId, elementBuilder, this);
    }

    public PatchElementTestStepBuilder oneOffPatchElement(String patchId, String layerName, boolean isAddon) {
        final PatchElementBuilder elementBuilder = builder.oneOffPatchElement(patchId, layerName, isAddon);
        return new PatchElementTestStepBuilder(patchId, elementBuilder, this);
    }

    @Override
    protected PatchingTestStepBuilder internalAddModification(ContentModification modification) {
        builder.addContentModification(modification);
        return returnThis();
    }

    @Override
    public Patch build() {
        return builder.build();
    }

    protected PatchingTestStepBuilder returnThis() {
        return this;
    }

}
