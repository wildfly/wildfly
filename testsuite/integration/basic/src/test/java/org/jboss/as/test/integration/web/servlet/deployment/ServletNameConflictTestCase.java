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
package org.jboss.as.test.integration.web.servlet.deployment;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TestCase of conflicted servlet-name
 * 
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServletNameConflictTestCase {

	@Ignore("AS7-4219")
	@Deployment(managed=false) /* After resolving AS7-4219, remove managed=false at all */
	@ShouldThrowException(Exception.class)
	public static Archive<?> deploy() {
		return ShrinkWrap
				.create(WebArchive.class, "conflict_servlet.war")
				.addClasses(DummyServlet.class)
				.setWebXML(
						new StringAsset(
								"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
										+ "\n"
										+ "<web-app version=\"3.0\"\n"
										+ "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
										+ "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
										+ "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
										+ "         metadata-complete=\"false\">\n"
										+ "<servlet>\n"
										+ "        <servlet-name>ConflictServlet</servlet-name>\n"
										+ "        <servlet-class>org.jboss.as.test.integration.web.servlet.deployment.DummyServlet</servlet-class>\n"
										+ "</servlet>\n"
										+ "<servlet>\n"
										+ "        <servlet-name>ConflictServlet</servlet-name>\n"
										+ "        <servlet-class>org.jboss.as.test.integration.web.servlet.deployment.DummyServlet</servlet-class>\n"
										+ "</servlet>\n" + "</web-app>"));
	}

	@Test
	public void test() {
	}

}
