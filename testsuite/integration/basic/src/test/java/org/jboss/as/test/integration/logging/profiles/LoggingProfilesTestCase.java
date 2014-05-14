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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.logging.util.AbstractLoggingTest;
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
@ServerSetup(LoggingProfilesTestCase.LoggingProfilesTestCaseSetup.class)
@RunWith(Arquillian.class)
public class LoggingProfilesTestCase extends AbstractLoggingTest {
	private static Logger log = Logger.getLogger(LoggingProfilesTestCase.class);

	@ContainerResource
	private ManagementClient managementClient;
	private static final String LOG_FILE_NAME = "profiles-test.log";
	private static final String PROFILE1_LOG_NAME = "dummy-profile1.log";
	private static final String PROFILE2_LOG_NAME = "dummy-profile2.log";
	private static final String CHANGED_LOG_NAME = "dummy-profile1-changed.log";
	private static File logFile;
	private static File dummyLog1;
	private static File dummyLog2;
	private static File dummyLog1Changed;

	static class LoggingProfilesTestCaseSetup extends
			AbstractMgmtServerSetupTask {

		@Override
		protected void doSetup(ManagementClient managementClient)
				throws Exception {

			// prepare log files
			logFile = prepareLogFile(managementClient,
					LOG_FILE_NAME);
			dummyLog1 = prepareLogFile(managementClient, PROFILE1_LOG_NAME);
			dummyLog2 = prepareLogFile(managementClient, PROFILE2_LOG_NAME);
			dummyLog1Changed = prepareLogFile(managementClient,
					CHANGED_LOG_NAME);

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
			file.get("path").set(LOG_FILE_NAME);
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			op = new ModelNode();
			op.get(OP).set("root-logger-assign-handler");
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("name").set("LOGGING_TEST");
			updates.add(op);

			// create dummy-profile1
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile1");
			updates.add(op);

			// add file handler
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile1");
			op.get(OP_ADDR).add("periodic-rotating-file-handler", "DUMMY1");
			op.get("level").set("FATAL");
			op.get("append").set("true");
			op.get("suffix").set(".yyyy-MM-dd");
			file = new ModelNode();
			file.get("relative-to").set("jboss.server.log.dir");
			file.get("path").set(PROFILE1_LOG_NAME);
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			// add root logger
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile1");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("level").set("INFO");
			ModelNode handlers = op.get("handlers");
			handlers.add("DUMMY1");
			op.get("handlers").set(handlers);
			updates.add(op);

			// create dummy-profile2
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile2");
			updates.add(op);

			// add file handler
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile2");
			op.get(OP_ADDR).add("periodic-rotating-file-handler", "DUMMY2");
			op.get("level").set("INFO");
			op.get("append").set("true");
			op.get("suffix").set(".yyyy-MM-dd");
			file = new ModelNode();
			file.get("relative-to").set("jboss.server.log.dir");
			file.get("path").set(PROFILE2_LOG_NAME);
			op.get("file").set(file);
			op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
			updates.add(op);

			// add root logger
			op = new ModelNode();
			op.get(OP).set(ADD);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile2");
			op.get(OP_ADDR).add("root-logger", "ROOT");
			op.get("level").set("INFO");
			handlers = op.get("handlers");
			handlers.add("DUMMY2");
			op.get("handlers").set(handlers);
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
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();

			// delete log files
			logFile.delete();
			dummyLog1.delete();
			dummyLog2.delete();
			dummyLog1Changed.delete();

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

			// remove dummy-profile1
			op = new ModelNode();
			op.get(OP).set(REMOVE);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile1");
			updates.add(op);

			// remove dummy-profile2
			op = new ModelNode();
			op.get(OP).set(REMOVE);
			op.get(OP_ADDR).add(SUBSYSTEM, "logging");
			op.get(OP_ADDR).add("logging-profile", "dummy-profile2");
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
	public static WebArchive createDeployment1() {
		WebArchive archive1 = ShrinkWrap.create(WebArchive.class,
				"logging1.war");
		archive1.addClasses(LoggingServlet.class);
		archive1.setManifest(new Asset() {
			@Override
			public InputStream openStream() {
				ManifestBuilder builder = ManifestBuilder.newInstance();
				StringBuffer dependencies = new StringBuffer();
				builder.addManifestHeader("Dependencies",
						dependencies.toString());
				builder.addManifestHeader("Logging-Profile", "dummy-profile1");
				return builder.openStream();
			}
		});
		return archive1;
	}

	@Deployment(name = "Deployment2")
	public static WebArchive createDeployment2() {
		WebArchive archive2 = ShrinkWrap.create(WebArchive.class,
				"logging2.war");
		archive2.addClasses(LoggingServlet.class);
		archive2.setManifest(new Asset() {
			@Override
			public InputStream openStream() {
				ManifestBuilder builder = ManifestBuilder.newInstance();
				StringBuffer dependencies = new StringBuffer();
				builder.addManifestHeader("Dependencies",
						dependencies.toString());
				builder.addManifestHeader("Logging-Profile", "dummy-profile2");
				return builder.openStream();
			}
		});
		return archive2;
	}

	@RunAsClient
	@Test
	@InSequence(1)
	public void noWarningTest() throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(logFile), StandardCharsets.UTF_8));
		String line;
		while ((line = br.readLine()) != null) {
			// Look for message id in order to support all languages.
			if (line.contains("WFLYLOG0010")) {
				br.close();
				Assert.fail("Every deployment should have defined its own logging profile. But found this line in logs: "
						+ line);
			}
		}
		br.close();
	}

	@RunAsClient
	@Test
	@InSequence(2)
	public void useDummyProfile1Test(
			@ArquillianResource(LoggingServlet.class) URL deployementUrl)
			throws MalformedURLException, IOException {

		URL url = new URL(deployementUrl, "Logger?text=DummyProfile1");
		// make some logs
		int statusCode = testResponse(url);
		assertTrue("Invalid response statusCode: " + statusCode,
				statusCode == HttpServletResponse.SC_OK);
	}

	@RunAsClient
	@Test
	@InSequence(2)
	// can not use @ArquillianResource Url url on annotated deployment
	public void useDummyProfile2Test(
			@ContainerResource ManagementClient managementClient)
			throws MalformedURLException, IOException {

		URL url = new URL("http://" + managementClient.getMgmtAddress()
				+ ":8080/logging2/Logger?text=DummyProfile2");
		// make some logs
		int statusCode = testResponse(url);
		assertTrue("Invalid response statusCode: " + statusCode,
				statusCode == HttpServletResponse.SC_OK);
	}

	private int testResponse(URL url) throws MalformedURLException, IOException {
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		return http.getResponseCode();
	}

	@RunAsClient
	@Test
	@InSequence(3)
	public void checkDummyLog1Test() throws IOException {
		Assert.assertTrue("dummy-profile1.log was not created",
				dummyLog1.exists());
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(dummyLog1), StandardCharsets.UTF_8));
		String line = br.readLine();
		Assert.assertTrue(
				"\"LoggingServlet is logging fatal message\" should be presented in dummy-profile1.log",
				line.contains("DummyProfile1: LoggingServlet is logging fatal message"));
		Assert.assertTrue("Only one log should be found in dummy-profile1.log",
				br.readLine() == null);
		br.close();
	}

	@RunAsClient
	@Test
	@InSequence(3)
	public void checkDummyLog2Test() throws IOException {
		Assert.assertTrue("dummy-profile2.log was not created",
				dummyLog2.exists());
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(dummyLog2), StandardCharsets.UTF_8));
		String line;
		while ((line = br.readLine()) != null) {
			if (!line.contains("DummyProfile2: LoggingServlet is logging")) {
				Assert.fail("dummy-profile2 should not contains this line: "
						+ line);
			}
		}
	}

	@RunAsClient
	@Test
	@InSequence(3)
	public void loggingTestLogTest() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(logFile), StandardCharsets.UTF_8));
		String line;
		while ((line = br.readLine()) != null) {
			// look for message id in order to support all languages
			if (line.contains("LoggingServlet is logging")) {
				br.close();
				Assert.fail("LoggingServlet messages should be presented only in files specified in profiles, but found: "
						+ line);
			}
		}
		br.close();
	}

	@RunAsClient
	@Test
	@InSequence(4)
	public void runtimeChangesTest(
			@ArquillianResource(LoggingServlet.class) URL deployementUrl)
			throws IOException {
		// Change logging level of file handler on dummy-profile1 from FATAL to
		// INFO
		ModelNode op = new ModelNode();
		op.get(OP).set("change-file");
		op.get(OP_ADDR).add(SUBSYSTEM, "logging");
		op.get(OP_ADDR).add("logging-profile", "dummy-profile1");
		op.get(OP_ADDR).add("periodic-rotating-file-handler", "DUMMY1");
		ModelNode file = new ModelNode();
		file.get("relative-to").set("jboss.server.log.dir");
		file.get("path").set(CHANGED_LOG_NAME);
		op.get("file").set(file);
		applyUpdate(op, managementClient.getControllerClient());

		// make some logs
		URL url = new URL(deployementUrl, "Logger?text=DummyProfile1");
		int statusCode = testResponse(url);
		assertTrue("Invalid response statusCode: " + statusCode,
				statusCode == HttpServletResponse.SC_OK);

		// check logs, after logging level change we should see also INFO and
		// ... messages
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(dummyLog1Changed),
				                StandardCharsets.UTF_8));
		String line = br.readLine();
		Assert.assertTrue(
				"\"LoggingServlet is logging fatal message\" should be presented in dummy-profile1.log",
				line.contains("DummyProfile1: LoggingServlet is logging fatal message"));
		Assert.assertTrue("Only one log should be found in dummy-profile1.log",
				br.readLine() == null);
		br.close();
	}

	static void applyUpdate(ModelNode update, final ModelControllerClient client)
			throws IOException {
		ModelNode result = client.execute(new OperationBuilder(update).build());
		if (result.hasDefined("outcome")
				&& "success".equals(result.get("outcome").asString())) {
			if (result.hasDefined("result")) {
				System.out.println(result.get("result"));
			}
		} else if (result.hasDefined("failure-description")) {
			throw new RuntimeException(result.get("failure-description")
					.toString());
		} else {
			throw new RuntimeException("Operation not successful; outcome = "
					+ result.get("outcome"));
		}
	}
}
