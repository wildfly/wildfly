/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.container.common;

import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ModuleApplicationProcessorTestCase {

    final static ArchivePath MANIFEST = ArchivePaths.create("META-INF", "MANIFEST.MF");

    @Test
    public void testAddManifestToJavaArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test.jar");
        archive.addClass(ModuleApplicationProcessorTestCase.class);

        Assert.assertNull(archive.get(MANIFEST));

        ModuleApplicationArchiveProcessor processor = new ModuleApplicationArchiveProcessor();
        processor.process(archive, null);
        checkManifest(archive, processor);
        processor.process(archive, null);
        checkManifest(archive, processor);
    }

    @Test
    public void testAddManifestToWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.jar");
        archive.addClass(ModuleApplicationProcessorTestCase.class);

        Assert.assertNull(archive.get(MANIFEST));

        ModuleApplicationArchiveProcessor processor = new ModuleApplicationArchiveProcessor();
        processor.process(archive, null);
        checkManifest(archive, processor);
        processor.process(archive, null);
        checkManifest(archive, processor);
    }

    private void checkManifest(Archive<?> archive, ModuleApplicationArchiveProcessor processor) {
        Node node = archive.get(MANIFEST);
        Assert.assertNotNull(node);

        Manifest mf = ManifestUtils.getOrCreateManifest(archive);
        Attributes attributes = mf.getMainAttributes();
        String value = attributes.getValue("Dependencies");
        Set<String> deps = new HashSet<String>();
        for (String dep : value.split(",")) {
            deps.add(dep.trim());
        }
        Assert.assertEquals(7, deps.size());
        Assert.assertTrue(deps.contains("org.jboss.arquillian.api"));
        Assert.assertTrue(deps.contains("org.jboss.arquillian.junit"));
        Assert.assertTrue(deps.contains("org.jboss.arquillian.spi"));
        Assert.assertTrue(deps.contains("org.jboss.modules"));
        Assert.assertTrue(deps.contains("org.jboss.msc"));
        Assert.assertTrue(deps.contains("org.jboss.shrinkwrap.api"));
        Assert.assertTrue(deps.contains("junit.junit"));

    }
}
