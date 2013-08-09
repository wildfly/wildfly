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

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.runner.TestUtils.createModule0;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.tool.PatchTool.Factory.policyBuilder;

import java.io.File;

import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.ContentConflictsException;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchConflictsUnitTestCase extends AbstractPatchingTest {

    @Test
    public void testOverrideModules() throws Exception {
        testModuleConflicts(policyBuilder().ignoreModuleChanges().createPolicy());
    }

    @Test
    public void testOverrideAllImpliesOverrideModules() throws Exception {
        // Override-all should also imply override modules
        testModuleConflicts(policyBuilder().overrideAll().createPolicy());
    }

    @Test
    public void testModuleOverrideAndRollback() throws Exception {
        final PatchingTestStepBuilder step = testModuleConflicts(policyBuilder().overrideAll().createPolicy());
        rollback(step);
    }

    protected PatchingTestStepBuilder testModuleConflicts(final ContentVerificationPolicy resolvePolicy) throws Exception {
        final PatchingTestBuilder builder = createDefaultBuilder();
        final byte[] moduleHash = new byte[20];

        // Create a conflict
        final File base = builder.getFile("modules", "system", "layers", "base");
        createModule0(base, "org.jboss.test", randomString());

        final PatchingTestStepBuilder oneOff1 = builder.createStepBuilder();
        oneOff1.setPatchId("one-off-1")
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .oneOffPatchElement("base-oo1", BASE, false)
                .updateModuleWithRandomContent("org.jboss.test", NO_CONTENT, moduleHash)
        ;
        try {
            apply(oneOff1);
            Assert.fail("should have detected conflicts");
        } catch (ContentConflictsException expected) {
            final ModuleItem item = new ModuleItem("org.jboss.test", "main", moduleHash);
            Assert.assertTrue(expected.getConflicts().contains(item));
        }
        // Apply patch and override all
        apply(oneOff1, resolvePolicy);

        return oneOff1;
    }

}
