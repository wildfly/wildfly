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
package org.jboss.as.test.integration.logging.perdeploy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.logging.util.Log4jLoggingBean;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Petr Křemenský <pkremens@redhat.com>
 */
@RunWith(Arquillian.class)
public class Log4jXmlTestCase {

	@Inject
	private Log4jLoggingBean loggingBean;

	private static final String FS = System.getProperty("file.separator");
	private static final File logDir = new File(
	System.getProperty("jbossas.ts.submodule.dir"), "target" + FS
	+ "jbossas" + FS + "standalone" + FS + "log");

	private static final File logFile = new File(logDir, "log4j-xml-test.log");

	@Deployment
	public static JavaArchive createDeployment() {
		JavaArchive jar = ShrinkWrap
				.create(JavaArchive.class)
				.addClasses(Log4jLoggingBean.class)
				.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
				.addAsResource(Log4jLoggingBean.class.getPackage(),
						"log4j.xml", "META-INF/log4j.xml");
		return jar;
	}

	@Before
	public void makeLog() {
		loggingBean.makeLog();
	}

	@After
	public void cleanUp() {
		logFile.delete();
	}

	@Test
	public void logsTest() throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					logFile), StandardCharsets.UTF_8));
		} catch (FileNotFoundException ex) {
			Assert.fail("Log file was not found");
		}
		String line;
		boolean trace = false;
		boolean fatal = false;
		String traceLine = "Log4j logging bean - trace";
		String fatalLine = "Log4j logging bean - fatal";
		while ((line = br.readLine()) != null) {
			if (line.contains(traceLine)) {
				trace = true;
			}
			if (line.contains(fatalLine)) {
				fatal = true;
			}
			lines.add(line);
		}
		br.close();
		Assert.assertTrue("Log file should contain line: " + traceLine, trace);
		Assert.assertTrue("Log file should contain line: " + fatalLine, fatal);
	}
}
