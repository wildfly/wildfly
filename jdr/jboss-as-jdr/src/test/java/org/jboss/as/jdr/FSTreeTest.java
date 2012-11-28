package org.jboss.as.jdr;

import org.apache.commons.io.FileUtils;
import org.jboss.as.jdr.util.FSTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class FSTreeTest {

    private File baseDirectory;

    @Before
    public void setUp() throws Exception {
        File tmpDir = FileUtils.getTempDirectory();
        baseDirectory = FileUtils.getFile(tmpDir, "FSTreeTest");
        FileUtils.forceMkdir(baseDirectory);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(baseDirectory);
    }

    @Test
    public void testTree() throws Exception {
        FSTree tree = new FSTree(baseDirectory.getPath());
        assertEquals(tree.toString(), "FSTreeTest\n");
    }
}
