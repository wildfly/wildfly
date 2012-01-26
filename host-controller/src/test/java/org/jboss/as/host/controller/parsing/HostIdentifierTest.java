/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.host.controller.parsing;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class HostIdentifierTest {

    private HostIdentifier hostName;

    public HostIdentifierTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of toString method, of class HostName.
     */
    @Test
    public void testSimpleName() {
        hostName = new HostIdentifier("name");
        assertEquals("name", hostName.toString());
    }

    @Test
    public void testSystemVariable() {
        System.setProperty("jboss.host.name", "localhost");
        hostName = new HostIdentifier("${jboss.host.name}");
        assertEquals("localhost", hostName.toString());
    }

    @Test
    public void testGUID() throws InterruptedException {
        System.setProperty("jboss.domain.guid", "localhost");
        hostName = new HostIdentifier("${jboss.domain.guid}");
        assertTrue("GUID was not generated", hostName.toString().length() > 0);
        assertFalse("Varibale recognized as name", hostName.toString().equals("jboss.domain.guid"));
        assertFalse("System varibale overrides global ID", hostName.toString().equals("jboss.domain.guid"));
    }

}
