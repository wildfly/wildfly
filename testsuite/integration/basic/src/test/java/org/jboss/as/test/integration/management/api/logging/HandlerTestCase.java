/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.api.logging;

import com.sun.org.apache.xml.internal.resolver.helpers.FileURL;
import java.util.concurrent.TimeUnit;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HandlerTestCase extends AbstractMgmtTestBase {

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "LoggingServlet.war");
        war.addClass(LoggingServlet.class);
        return war;
    }

    @Before
    public void before() throws IOException {
        initModelControllerClient(url.getHost(), MGMT_PORT);
    }

    @AfterClass
    public static void after() throws IOException {
        closeModelControllerClient();
    }

    @Test
    public void testAddRemoveFileHandler() throws Exception {

        File logFile = new File(tempDir, "test-fh.log");
        if (logFile.exists()) assertTrue(logFile.delete());

        // add file handler
        ModelNode op = createOpNode("subsystem=logging/file-handler=test-fh", "add");
        op.get("name").set("test-fh");
        op.get("level").set("INFO");
        op.get("file").get("path").set(logFile.getAbsolutePath());
        executeOperation(op);

        // register it with root logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-assign-handler");
        op.get("name").set("test-fh");
        executeOperation(op);

        // check it is listed in root-logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "read-attribute");
        op.get("name").set("handlers");
        ModelNode handlers = executeOperation(op);
        List<String> loggers = ModelUtil.modelNodeAsStingList(handlers);
        assertTrue(loggers.contains("test-fh"));

        // force server to issue a log message
        String response = HttpRequest.get(url.toString() + "/LoggingServlet", 10, TimeUnit.SECONDS);
        assertTrue(response.contains("Logging servlet."));

        // deregister handler from logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-unassign-handler");
        op.get("name").set("test-fh");
        executeOperation(op);

        // check it is not listed in root-logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "read-attribute");
        op.get("name").set("handlers");
        handlers = executeOperation(op);
        loggers = ModelUtil.modelNodeAsStingList(handlers);
        assertFalse(loggers.contains("test-fh"));

        // remove handler
        op = createOpNode("subsystem=logging/file-handler=test-fh", "remove");
        executeOperation(op);

        // check generated log file
        String log = FileUtils.readFileToString(logFile);
        assertTrue(log.contains("Logging servlet."));

        // verify that the logger is stopped, no more logs are comming to the file
        long checksum = FileUtils.checksumCRC32(logFile);
        response = HttpRequest.get(url.toString() + "/LoggingServlet", 10, TimeUnit.SECONDS);
        assertTrue(response.contains("Logging servlet."));
        assertEquals(checksum, FileUtils.checksumCRC32(logFile));

        // remove log file
        assertTrue(logFile.delete());
    }

}
