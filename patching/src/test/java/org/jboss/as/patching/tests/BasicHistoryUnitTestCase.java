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

import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.as.patching.PatchingException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class BasicHistoryUnitTestCase extends AbstractPatchingTest {

    static final String[] FILE_ONE = {"bin", "standalone.sh"};
    static final String[] FILE_TWO = {"bin", "standalone.conf"};
    static final String[] FILE_THREE = {"bin", "standalone.test"};
    static final String[] FILE_EXISTING = {"bin", "test"};

    @Test
    public void testBasicPatchHistory() throws IOException, PatchingException {

        final PatchingTestBuilder builder = createDefaultBuilder();
        final byte[] standaloneHash = new byte[20];
        final byte[] moduleHash = new byte[20];

        // Create a file
        final File existing = builder.getFile(FILE_EXISTING);
        touch(existing);
        dump(existing, randomString());
        final byte[] existingHash = hashFile(existing);
        final byte[] initialHash = Arrays.copyOf(existingHash, existingHash.length);

        final PatchingTestStepBuilder cp1 = builder.createBuilder();
        cp1.setPatchId("CP1")
                .cumulativePatchIdentity(PRODUCT_VERSION)
                .cumulativePatchElement("base:CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING);
        ;
        // Apply CP1
        apply(cp1);

        //
        Assert.assertTrue(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        Assert.assertTrue(Arrays.equals(existingHash, hashFile(existing)));

        final PatchingTestStepBuilder oneOff1 = builder.createBuilder();
        oneOff1.setPatchId("oneOff1")
                .oneOffPatchIdentity(PRODUCT_VERSION, "CP1")
                .oneOffPatchElement("base:oneOff1", "base", "base:CP1", false)
                .updateModuleWithRandomContent("org.jboss.test", moduleHash, null)
                .getParent()
                .updateFileWithRandomContent(standaloneHash, null, FILE_ONE)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply oneOff1
        apply(oneOff1);

        //
        Assert.assertTrue(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        Assert.assertTrue(Arrays.equals(existingHash, hashFile(existing)));

        final PatchingTestStepBuilder cp2 = builder.createBuilder();
        cp2.setPatchId("CP2")
                .cumulativePatchIdentity(PRODUCT_VERSION)
                .cumulativePatchElement("base:CP2", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_TWO)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply CP2
        apply(cp2);

        // Needs to invalidate cp1
        Assert.assertTrue(builder.hasFile(FILE_TWO));
        Assert.assertFalse(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        Assert.assertTrue(Arrays.equals(existingHash, hashFile(existing)));

        final PatchingTestStepBuilder release = builder.createBuilder();
        release.setPatchId("CP3")
                .cumulativePatchIdentity(PRODUCT_VERSION)
                .cumulativePatchElement("base:CP3", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_THREE )
        ;
        // Apply release
        apply(release);

        // Needs to invalidate all CPs
        Assert.assertTrue(builder.hasFile(FILE_THREE ));
        Assert.assertFalse(builder.hasFile(FILE_TWO));
        Assert.assertFalse(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        // Assert.assertTrue(Arrays.equals(initialHash, hashFile(existing)));

    }

}
