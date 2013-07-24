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

package org.jboss.as.patching.runner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchMergeUnitTestCase {

    static final String name = "jboss-client.jar";
    static final String[] path = new String[] { "bin", "client" };

    static byte[] one = new byte[] { 'a', 'b', 'c', '1', '2', '3' };
    static byte[] two = new byte[] { 'c', 'd', 'e', '4', '5', '6' };
    static byte[] three = new byte[] { 'f', 'g', 'h', '7', '8', '9' };
    static byte[] four = new byte[] { 'i', 'j', 'k', '9', '8', '7' };

    @Test
    public void testSimple() throws Exception {

        // content-item 'two' replacing 'one'
        final RollbackInfo patch01 = createRollbackInfo("patch01", two, one);
        // content-item 'three' replacing 'two'
        final RollbackInfo patch02 = createRollbackInfo("patch02", three, two);

        // [patch-two, patch-one]
        final Map<Location, PatchingTasks.ContentTaskDefinition> defs = process(patch02, patch01);

        Assert.assertEquals(1, defs.size());
        final PatchingTasks.ContentTaskDefinition def = defs.get(new Location(new MiscContentItem(name, path, one)));
        Assert.assertNotNull(def);
        Assert.assertFalse(def.hasConflicts());
        // We want to restore one (from the backup)
        Assert.assertEquals(one, def.getTarget().getItem().getContentHash());
        // The original target was two
        Assert.assertEquals(two, def.getTarget().getTargetHash());
        // The current content however is three
        Assert.assertEquals(three, def.getLatest().getTargetHash());
        // And originally replaced two
        Assert.assertEquals(two, def.getLatest().getItem().getContentHash());

        // The resulting operation should replace 'three' with 'one'
        final ContentModification modification = PatchingTaskDescription.resolveDefinition(def);
        Assert.assertEquals(one, modification.getItem().getContentHash());
        Assert.assertEquals(three, modification.getTargetHash());
    }

    @Test
    public void testOverrideExisting() throws Exception {

        // content-item 'two' replacing 'four', originally targeting 'one'
        final RollbackInfo patch01 = createRollbackInfo("patch01", two, one, four, two);
        // content-item 'three' replacing 'two'
        final RollbackInfo patch02 = createRollbackInfo("patch02", three, two);

        // [patch-two, patch-one]
        final Map<Location, PatchingTasks.ContentTaskDefinition> defs = process(patch02, patch01);

        Assert.assertEquals(1, defs.size());
        final PatchingTasks.ContentTaskDefinition def = defs.get(new Location(new MiscContentItem(name, path, one)));
        Assert.assertNotNull(def);
        Assert.assertTrue(def.hasConflicts());

        // We want to restore four (from the backup)
        Assert.assertEquals(four, def.getTarget().getItem().getContentHash());
        // The original target was two
        Assert.assertEquals(two, def.getTarget().getTargetHash());
        // The current content however is three
        Assert.assertEquals(three, def.getLatest().getTargetHash());
        // And originally replaced two
        Assert.assertEquals(two, def.getLatest().getItem().getContentHash());

        // The resulting operation should replace 'three' with 'four'
        final ContentModification modification = PatchingTaskDescription.resolveDefinition(def);
        Assert.assertEquals(four, modification.getItem().getContentHash());
        Assert.assertEquals(three, modification.getTargetHash());
    }

    @Test
    public void testPreserveExisting() throws Exception {

        // content-item 'two' replacing 'one', but kept 'four'
        final RollbackInfo patch01 = createRollbackInfo("patch01", two, one, four, four);
        // content-item 'three' replacing 'two'
        final RollbackInfo patch02 = createRollbackInfo("patch02", three, four);

        // [patch-two, patch-one]
        final Map<Location, PatchingTasks.ContentTaskDefinition> defs = process(patch02, patch01);

        Assert.assertEquals(1, defs.size());
        final PatchingTasks.ContentTaskDefinition def = defs.get(new Location(new MiscContentItem(name, path, one)));
        Assert.assertNotNull(def);
        Assert.assertTrue(def.hasConflicts());

        // We want to got back to four
        Assert.assertEquals(four, def.getTarget().getItem().getContentHash());
        // The recorded action was preserving four
        Assert.assertEquals(four, def.getTarget().getTargetHash());
        // The current content however is three
        Assert.assertEquals(three, def.getLatest().getTargetHash());
        // And originally replaced four
        Assert.assertEquals(four, def.getLatest().getItem().getContentHash());

        // The resulting operation should replace 'three' with 'four'
        final ContentModification modification = PatchingTaskDescription.resolveDefinition(def);
        Assert.assertEquals(four, modification.getItem().getContentHash());
        Assert.assertEquals(three, modification.getTargetHash());

    }

    static Map<Location, PatchingTasks.ContentTaskDefinition> process(final RollbackInfo... rollbackInfos) {
        final Map<Location, PatchingTasks.ContentTaskDefinition> foo = new HashMap<Location, PatchingTasks.ContentTaskDefinition>();
        for(final RollbackInfo info : rollbackInfos) {
            PatchingTasks.rollback(info.original.getPatchId(), info.original.getModifications(), info.rollback.getModifications(), foo, ContentItemFilter.MISC_ONLY, PatchingTaskContext.Mode.APPLY);
        }
        return foo;
    }

    static RollbackInfo createRollbackInfo(String id, byte[] ih, byte[] rh) {
        return createRollbackInfo(id, ih, rh, rh, ih);
    }

    static RollbackInfo createRollbackInfo(String id, byte[] oih, byte[] oth, byte[] rih, byte[] rth) {
        //
        final MiscContentItem oi = new MiscContentItem(name, path, oih);
        final MiscContentItem ri = new MiscContentItem(name, path, rih);
        //
        final Patch o = createPatch(id, Patch.PatchType.ONE_OFF, new ContentModification(oi, oth, ModificationType.MODIFY));
        final Patch r = createPatch(id, Patch.PatchType.ONE_OFF, new ContentModification(ri, rth, ModificationType.MODIFY));
        //
        return new RollbackInfo(o, r);
    }

    static class RollbackInfo {
        final Patch original;
        final Patch rollback;
        RollbackInfo(Patch original, Patch rollback) {
            this.original = original;
            this.rollback = rollback;
        }
    }

    static Patch createPatch(final String id, final Patch.PatchType type, final ContentModification... item) {
        return new PatchImpl(id, "test", null, Collections.<PatchElement>emptyList(), Arrays.asList(item));
    }

}
