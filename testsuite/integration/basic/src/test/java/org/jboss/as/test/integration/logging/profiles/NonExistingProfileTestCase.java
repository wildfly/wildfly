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
package org.jboss.as.test.integration.logging.profiles;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.logging.util.AbstractLoggingTest;
import org.jboss.as.test.integration.logging.util.LoggingServlet;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Petr Křemenský <pkremens@redhat.com>
 */
@ServerSetup(NonExistingProfileTestCase.NonExistingProfileTestCaseSetup.class)
@RunWith(Arquillian.class)
public class NonExistingProfileTestCase extends AbstractLoggingTest {

	private static Logger log = Logger
			.getLogger(NonExistingProfileTestCase.class);

	@ArquillianResource(LoggingServlet.class)
	URL url;
	private static final String LOG_FILE_NAME = "non-existing-profile-test.log";
	private static File loggingTestLog;

	static class NonExistingProfileTestCaseSetup extends
			AbstractMgmtServerSetupTask {
		@Override
		protected void doSetup(ManagementClient managementClient)
				throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();

			// prepare log files
			loggingTestLog = prepareLogFile(managementClient,
					LOG_FILE_NAME);

			// add custom file-handler
			ModelNode op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("periodic-rotating-file-handler",
					"LOGGING_TEST");
			op.get("append").set("true");
			op.get("suffix").set(".yyyy-MM-dd");
			ModelNode file = new ModelNode();
			file.get("relative-to").set("jboss.server.log.dir");
			file.get("path").set(LOG_FILE_NAME);
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			// add handler to root-logger
			op = new ModelNode();
			op.get(OP).set("root-logger-assign-handler");
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("name").set("LOGGING_TEST");
			updates.add(op);

			// we want all operations to perform
			for (ModelNode modelNode : updates) {
				try {
					executeOperation(modelNode);
				} catch (MgmtOperationException exp) {
					log.warn(exp.getMessage());
				}
			}
		}

		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();

			// delete log file
			loggingTestLog.delete();

			// remove LOGGING_TEST from root-logger
			ModelNode op = new ModelNode();
			op.get(OP).set("root-logger-unassign-handler");
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("name").set("LOGGING_TEST");
			updates.add(op);

			// remove custom file handler
			op = new ModelNode();
			op.get(OP).set(REMOVE);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("periodic-rotating-file-handler",
					"LOGGING_TEST");
			updates.add(op);

			// we want to perform all operations
			for (ModelNode modelNode : updates) {
				try {
					executeOperation(modelNode);
				} catch (MgmtOperationException exp) {
					log.warn(exp.getMessage());
				}
			}
		}
	}

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive archive = ShrinkWrap.create(WebArchive.class, "logging.war");
		archive.addClasses(LoggingServlet.class);
		archive.setManifest(new StringAsset(
				Descriptors.create(ManifestDescriptor.class)
						.attribute("Logging-Profile", "non-existing-profile")
						.exportAsString()));
		return archive;
	}

	@AfterClass
	@RunAsClient
	public static void cleanCustomFile() {
		loggingTestLog.delete();
	}

	@Test
	@RunAsClient
	public void warningMessageTest() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(loggingTestLog), StandardCharsets.UTF_8));
		String line;
		boolean warningFound = false;
		while ((line = br.readLine()) != null) {
			// Look for message id in order to support all languages
			if (line.contains("WFLYLOG0010")) {
				warningFound = true;
				break;
			}
		}
		br.close();
		Assert.assertTrue(warningFound);
	}

	@Test
	@RunAsClient
	public void defaultLoggingTest() throws IOException {
		// make some logs
		HttpURLConnection http = (HttpURLConnection) new URL(url, "Logger")
				.openConnection();
		int statusCode = http.getResponseCode();
		assertTrue("Invalid response statusCode: " + statusCode,
				statusCode == HttpServletResponse.SC_OK);
		// check logs
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(loggingTestLog), StandardCharsets.UTF_8));
		String line;
		boolean logFound = false;
		while ((line = br.readLine()) != null) {
			if (line.contains("LoggingServlet is logging")) {
				logFound = true;
				break;
			}
		}
		br.close();
		Assert.assertTrue(logFound);
	}
}
