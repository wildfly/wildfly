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

package org.jboss.as.test.integration.logging.misc;

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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.logging.util.LoggingServlet;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author Petr Křemenský <pkremens@redhat.com>
 */

@ServerSetup(LoggingPreferencesPerDeployFalseTestCase.LoggingPreferencesPerDeployFalseTestCaseSetup.class)
@RunWith(Arquillian.class)
public class LoggingPreferencesPerDeployFalseTestCase {

	private static Logger log = Logger
			.getLogger(LoggingPreferencesPerDeployFalseTestCase.class);

	@ContainerResource
	private ManagementClient managementClient;

	private static final String FS = System.getProperty("file.separator");
	private static final File logDir = new File(
	System.getProperty("jbossas.ts.submodule.dir"), "target" + FS
	+ "jbossas" + FS + "standalone" + FS + "log");
	private static final File loggingTestLog = new File(logDir,
			"logging-test.log");
	private static final File logFile = new File(logDir,
			"jboss-logging-properties-test.log");

	private static final File dummyLog = new File(logDir, "dummy-profile.log");

	static class LoggingPreferencesPerDeployFalseTestCaseSetup extends
			AbstractMgmtServerSetupTask {

		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();
			// clean test log files
			if (loggingTestLog.exists()) {
				loggingTestLog.delete();
			}
			if (dummyLog.exists()) {
				dummyLog.delete();
			}

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

			// remove dummy-profile
			op = new ModelNode();
			op.get(OP).set(REMOVE);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile");
			updates.add(op);

			// remove "org.jboss.as.logging.per-deployment=false" system
			// property
			op = new ModelNode();
			op.get(OP).set(REMOVE);
			op.get(OP_ADDR).add("system-property",
					"org.jboss.as.logging.per-deployment");
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

		@Override
		protected void doSetup(ManagementClient managementClient)
				throws Exception {

			final List<ModelNode> updates = new ArrayList<ModelNode>();

			ModelNode op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("periodic-rotating-file-handler",
					"LOGGING_TEST");
			op.get("append").set("true");
			op.get("suffix").set(".yyyy-MM-dd");
			ModelNode file = new ModelNode();
			file.get("relative-to").set("jboss.server.log.dir");
			file.get("path").set("logging-test.log");
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			op = new ModelNode();
			op.get(OP).set("root-logger-assign-handler");
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("name").set("LOGGING_TEST");
			updates.add(op);

			// create dummy-profile
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile");
			updates.add(op);

			// add file handler
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile");
			op.get(OP_ADDR).add("periodic-rotating-file-handler", "DUMMY");
			op.get("level").set("FATAL");
			op.get("append").set("true");
			op.get("suffix").set(".yyyy-MM-dd");
			file = new ModelNode();
			file.get("relative-to").set("jboss.server.log.dir");
			file.get("path").set("dummy-profile.log");
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			// add root logger
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("level").set("INFO");
			ModelNode handlers = op.get("handlers");
			handlers.add("DUMMY");
			op.get("handlers").set(handlers);
			updates.add(op);

			// add "org.jboss.as.logging.per-deployment=false" system property
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add("system-property",
					"org.jboss.as.logging.per-deployment");
			op.get("value").set("false");
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
		archive.setManifest(new Asset() {
			@Override
			public InputStream openStream() {
				ManifestBuilder builder = ManifestBuilder.newInstance();
				StringBuffer dependencies = new StringBuffer();
				builder.addManifestHeader("Dependencies",
						dependencies.toString());
				builder.addManifestHeader("Logging-Profile", "dummy-profile");
				return builder.openStream();
			}
		});
		return archive;
	}

	@ArquillianResource(LoggingServlet.class)
	URL url;

	@RunAsClient
	@Test
	@InSequence(1)
	public void checkDummyLogTest() throws IOException {
		// make some logs
		HttpURLConnection http = (HttpURLConnection) new URL(url, "Logger")
				.openConnection();
		int statusCode = http.getResponseCode();
		assertTrue("Invalid response statusCode: " + statusCode,
				statusCode == HttpServletResponse.SC_OK);
		// check logs
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(dummyLog), Charset.forName("UTF-8")));
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

	@Test
	@RunAsClient
	@InSequence(2)
	public void perDeployFilePresenceTest() {
		Assert.assertFalse("File: " + logFile.toString()
				+ " should not be created!", logFile.exists());
	}

}
