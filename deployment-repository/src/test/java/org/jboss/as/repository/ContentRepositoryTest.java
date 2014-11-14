/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ContentRepositoryTest {

    private ContentRepository repository;
    private final File rootDir = new File("target", "repository");

    public ContentRepositoryTest() {
    }

    @Before
    public void createRepository() {
        if(rootDir.exists()) {
            deleteRecursively(rootDir);
        }
        rootDir.mkdirs();
        repository = ContentRepository.Factory.create(rootDir, 0L);
    }

    @After
    public void destroyRepository() {
        deleteRecursively(rootDir);
        repository = null;
    }

    private void deleteRecursively(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    deleteRecursively(new File(file, name));
                }
            }
            file.delete();
        }
    }

    private String readFileContent(File file) throws Exception {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = new FileInputStream(file);
            StreamUtils.copyStream(in, out);
            return out.toString("UTF-8");
        } finally {
            StreamUtils.safeClose(in);
            StreamUtils.safeClose(out);
        }
    }
    
    private void copyFileContent(File src, File target) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(target);
            StreamUtils.copyStream(in, out);
        } finally {
            StreamUtils.safeClose(in);
            StreamUtils.safeClose(out);
        }
    }

    /**
     * Test of addContent method, of class ContentRepository.
     */
    @Test
    public void testAddContent() throws Exception {
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream("overlay.xhtml");
            String expResult = "0c40ffacd15b0f66d5081a93407d3ff5e3c65a71";
            byte[] result = repository.addContent(stream);
            assertThat(result, is(notNullValue()));
            assertThat(HashUtil.bytesToHexString(result), is(expResult));
        } finally {
            StreamUtils.safeClose(stream);
        }
    }

    /**
     * Test of addContentReference method, of class ContentRepository.
     */
    @Test
    public void testAddContentReference() throws Exception {
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream("overlay.xhtml");
            String expResult = "0c40ffacd15b0f66d5081a93407d3ff5e3c65a71";
            byte[] result = repository.addContent(stream);
            assertThat(result, is(notNullValue()));
            assertThat(HashUtil.bytesToHexString(result), is(expResult));
            ContentReference reference = new ContentReference("contentReferenceIdentifier", result);
            repository.addContentReference(reference);
        } finally {
            StreamUtils.safeClose(stream);
        }
    }

    /**
     * Test of getContent method, of class ContentRepository.
     */
    @Test
    public void testGetContent() throws Exception {
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream("overlay.xhtml");
            String expResult = "0c40ffacd15b0f66d5081a93407d3ff5e3c65a71";
            byte[] result = repository.addContent(stream);
            assertThat(result, is(notNullValue()));
            assertThat(HashUtil.bytesToHexString(result), is(expResult));
            VirtualFile content = repository.getContent(result);
            String contentHtml = readFileContent(content.getPhysicalFile());
            String expectedContentHtml = readFileContent(new File(this.getClass().getClassLoader().getResource("overlay.xhtml").toURI()));
            assertThat(contentHtml, is(expectedContentHtml));
        } finally {
            StreamUtils.safeClose(stream);
        }
    }

    /**
     * Test of hasContent method, of class ContentRepository.
     */
    @Test
    public void testHasContent() throws Exception {
        String expResult = "0c40ffacd15b0f66d5081a93407d3ff5e3c65a71";
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream("overlay.xhtml");
            assertThat(repository.hasContent(HashUtil.hexStringToByteArray(expResult)), is(false));
            byte[] result = repository.addContent(stream);
            assertThat(result, is(notNullValue()));
            assertThat(HashUtil.bytesToHexString(result), is(expResult));
            assertThat(repository.hasContent(HashUtil.hexStringToByteArray(expResult)), is(true));
        } finally {
            StreamUtils.safeClose(stream);
        }
    }

    /**
     * Test of removeContent method, of class ContentRepository.
     */
    @Test
    public void testRemoveContent() throws Exception {
        String expResult = "0c40ffacd15b0f66d5081a93407d3ff5e3c65a71";
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream("overlay.xhtml");
            assertThat(repository.hasContent(HashUtil.hexStringToByteArray(expResult)), is(false));
            byte[] result = repository.addContent(stream);
            assertThat(result, is(notNullValue()));
            assertThat(HashUtil.bytesToHexString(result), is(expResult));
            assertThat(repository.hasContent(HashUtil.hexStringToByteArray(expResult)), is(true));
            repository.removeContent(new ContentReference("overlay.xhtml", expResult));
            assertThat(repository.hasContent(HashUtil.hexStringToByteArray(expResult)), is(false));
            VirtualFile content = repository.getContent(result);
            assertThat(content.exists(), is(false));
        } finally {
            StreamUtils.safeClose(stream);
        }
    }

    /**
     * Test that an empty dir will be removed during cleaning.
     */
    @Test
    public void testCleanEmptyParentDir() throws Exception {
        File emptyGrandParent = new File(rootDir, "ae");
        emptyGrandParent.mkdir();
        File emptyParent = new File(emptyGrandParent, "ffacd15b0f66d5081a93407d3ff5e3c65a71");
        emptyParent.mkdir();
        assertThat(emptyGrandParent.exists(), is(true));
        assertThat(emptyParent.exists(), is(true));
        Map<String, Set<String>> result = repository.cleanObsoleteContent(); //To mark content for deletion
        assertThat(result.get(ContentRepository.MARKED_CONTENT).size(), is(1));
        assertThat(result.get(ContentRepository.DELETED_CONTENT).size(), is(0));
        assertThat(result.get(ContentRepository.MARKED_CONTENT).contains(emptyParent.getAbsolutePath()), is(true));
        Thread.sleep(10);
        result = repository.cleanObsoleteContent();
        assertThat(emptyGrandParent.exists(), is(false));
        assertThat(emptyParent.exists(), is(false));
        assertThat(result.get(ContentRepository.MARKED_CONTENT).size(), is(0));
        assertThat(result.get(ContentRepository.DELETED_CONTENT).size(), is(1));
        assertThat(result.get(ContentRepository.DELETED_CONTENT).contains(emptyParent.getAbsolutePath()), is(true));
    }

    /**
     * Test that an empty dir will be removed during cleaning.
     */
    @Test
    public void testCleanEmptyGrandParentDir() throws Exception {
        File emptyGrandParent = new File(rootDir, "ae");
        emptyGrandParent.mkdir();
        assertThat(emptyGrandParent.exists(), is(true));
        Map<String, Set<String>>  result = repository.cleanObsoleteContent(); //Mark content for deletion
        assertThat(result.get(ContentRepository.MARKED_CONTENT).size(), is(1));
        assertThat(result.get(ContentRepository.DELETED_CONTENT).size(), is(0));
        assertThat(result.get(ContentRepository.MARKED_CONTENT).contains(emptyGrandParent.getAbsolutePath()), is(true));
        Thread.sleep(10);
        result = repository.cleanObsoleteContent();
        assertThat(emptyGrandParent.exists(), is(false));
        assertThat(result.get(ContentRepository.DELETED_CONTENT).size(), is(1));
        assertThat(result.get(ContentRepository.MARKED_CONTENT).size(), is(0));
        assertThat(result.get(ContentRepository.DELETED_CONTENT).contains(emptyGrandParent.getAbsolutePath()), is(true));

    }

    /**
     * Test that an dir not empty with no content will not be removed during cleaning.
     */
    @Test
    public void testNotEmptyDir() throws Exception {
        File grandParentDir = new File(rootDir, "ae");
        File parentDir = new File(grandParentDir, "ffacd15b0f66d5081a93407d3ff5e3c65a71");
        File overlay = new File(parentDir, "overlay.xhtml");
        File content = new File(parentDir, "content");
        parentDir.mkdirs();
        InputStream stream = null;
        try {
            copyFileContent(new File(this.getClass().getClassLoader().getResource("overlay.xhtml").toURI()), overlay);
            copyFileContent(new File(this.getClass().getClassLoader().getResource("overlay.xhtml").toURI()), content);
            assertThat(content.exists(), is(true));
            assertThat(overlay.exists(), is(true));
            Map<String, Set<String>> result = repository.cleanObsoleteContent(); //Mark content for deletion
            assertThat(result.get(ContentRepository.MARKED_CONTENT).size(), is(1));
            assertThat(result.get(ContentRepository.DELETED_CONTENT).size(), is(0));
            assertThat(result.get(ContentRepository.MARKED_CONTENT).contains(parentDir.getAbsolutePath()), is(true));
            Thread.sleep(10);
            result = repository.cleanObsoleteContent();
            assertThat(overlay.exists(), is(true));
            assertThat(content.exists(), is(false));
            assertThat(result.get(ContentRepository.MARKED_CONTENT).size(), is(0));
            assertThat(result.get(ContentRepository.DELETED_CONTENT).size(), is(1));
            assertThat(result.get(ContentRepository.DELETED_CONTENT).contains(parentDir.getAbsolutePath()), is(true));
        } finally {
            overlay.delete();
            parentDir.delete();
            grandParentDir.delete();
        }

    }
}
