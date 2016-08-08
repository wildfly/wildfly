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
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jfree.util.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author Petr Kremensky <pkremens@redhat.com>
 */
@ServerSetup(LoggingProfilesTestCase.LoggingProfilesTestCaseSetup.class)
@RunWith(Arquillian.class)
public class LoggingProfilesTestCase {

	@ContainerResource
	private ManagementClient managementClient;

	private static final String FS = System.getProperty("file.separator");
	private static final File logDir = new File(
			System.getProperty("jbossas.ts.submodule.dir"), "target" + FS
					+ "jbossas" + FS + "standalone" + FS + "log");

	private static final File loggingTestLog = new File(logDir,
			"logging-test.log");
	private static final File dummyLog1 = new File(logDir, "dummy-profile1.log");
	private static final File dummyLog2 = new File(logDir, "dummy-profile2.log");
	private static final File dummyLog1Changed = new File(logDir,
			"dummy-profile1-changed.log");

	static class LoggingProfilesTestCaseSetup extends
			AbstractMgmtServerSetupTask {

		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			final List<ModelNode> updates = new ArrayList<ModelNode>();
			// clean test log files
			if (loggingTestLog.exists()) {
				loggingTestLog.delete();
			}
			if (dummyLog1.exists()) {
				dummyLog1.delete();
			}
			if (dummyLog2.exists()) {
				dummyLog2.delete();
			}
			if (dummyLog1Changed.exists()) {
				dummyLog1Changed.delete();
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
					Log.warn(exp.getMessage());
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
			file.get("path").set("dummy-profile1.log");
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
			file.get("path").set("dummy-profile2.log");
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
					Log.warn(exp.getMessage());
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
				new FileInputStream(loggingTestLog), Charset.forName("UTF-8")));
		String line;
		while ((line = br.readLine()) != null) {
			// Look for message id in order to support all languages.
			if (line.contains("JBAS011509")) {
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
				new FileInputStream(dummyLog1), Charset.forName("UTF-8")));
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
				new FileInputStream(dummyLog2), Charset.forName("UTF-8")));
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
				new FileInputStream(loggingTestLog), Charset.forName("UTF-8")));
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
		file.get("path").set("dummy-profile1-changed.log");
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
						Charset.forName("UTF-8")));
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
