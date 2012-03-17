/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jaxrs.cfg;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for RESTEasy configuration parameter 'resteasy.scan.resources'
 *
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ResteasyScanResourcesTestCase extends ResteasyParamsParent {

	private static final String depNameTrue = "dep_true";
	private static final String depNameFalse = "dep_false";
	private static final String depNameFalseAS7_3034 = "dep_false_as7_3034";
	private static final String depNameInvalid = "dep_invalid";

	@Deployment(name = depNameTrue, managed = false)
	public static Archive<?> deploy_true() {
		return getDeploy(depNameTrue,
				ResteasyScanResourcesTestCase.class,
				"resteasy.scan.resources", "true");
	}

	@Deployment(name = depNameFalse, managed = false)
	public static Archive<?> deploy_false() {
		return getDeploy(depNameFalse,
				ResteasyScanResourcesTestCase.class,
				"resteasy.scan.resources", "false");
	}

	@Deployment(name = depNameFalseAS7_3034, managed = false)
	public static Archive<?> deploy_false_as7_3034() {
		return getDeploy(
				depNameFalseAS7_3034,
				ResteasyScanResourcesTestCase.class,
				"resteasy.scan.resources",
				"false",
				"<servlet>\n"
						+ "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
						+ "        <servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher</servlet-class>\n"
						+ "</servlet>\n" + "\n", "");
	}

	@Deployment(name = depNameInvalid, managed = false)
	public static Archive<?> deploy_invalid() {
		return getDeploy(depNameInvalid,
				ResteasyScanResourcesTestCase.class,
				"resteasy.scan.resources", "blah");

	}

	@ArquillianResource
	private Deployer deployer;

	@Test
	@InSequence(-1)
	public void testDeployTrue() throws Exception {
		try {
			deployer.deploy(depNameTrue);
		} catch (Exception e) {
			Assert.fail("Unexpected exception occured during deploying - "
					+ e.toString());
		}
	}

	@Test
	@InSequence(1)
	@OperateOnDeployment(depNameTrue)
	public void testDeployTrue1(@ArquillianResource URL url) throws Exception {
		String result = performCall("myjaxrs/helloworld", url);
		assertEquals("Hello World!", result);
	}

	@Ignore("AS-3034")
	@Test
	@InSequence(-1)
	public void testDeployFalse() throws Exception {
		try {
			deployer.deploy(depNameFalse);
		} catch (Exception e) {
			Assert.fail("Unexpected exception occured during deploying - "
					+ e.toString());
		}
	}

	@Ignore("AS-3034")
	@Test
	@InSequence(1)
	@OperateOnDeployment(depNameFalse)
	public void testDeployFalse1(@ArquillianResource URL url) throws Exception {
		try {
			@SuppressWarnings("unused")
			String result = performCall("myjaxrs/helloworld", url);
			Assert.fail("Scan of Resources is disabled so we should not pass to there - HTTP 404 must occur!");
		} catch (Exception e) {
		}
	}

	@Ignore("AS-3034")
	@Test
	@InSequence(-1)
	public void testDeployFalse_AS7_3043() throws Exception {
		try {
			deployer.deploy(depNameFalseAS7_3034);
			Assert.fail("Test should not go there - invalid deployment (duplicated javax.ws.rs.core.Application)! Possible regression of AS7-3034 found");
		} catch (Exception e) {
		}
	}

	@Ignore("AS-3034")
	@Test
	@RunAsClient
	public void testDeployInvalid() throws Exception {
		try {
			deployer.deploy(depNameInvalid);
			Assert.fail("Test should not go here - invalid deployment (invalid value of resteasy.scan.resources)!");
		} catch (Exception e) {
		}
	}

}
