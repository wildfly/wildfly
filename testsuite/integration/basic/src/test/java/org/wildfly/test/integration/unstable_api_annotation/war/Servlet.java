/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.unstable_api_annotation.war;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(name = "WarClassA", urlPatterns = { "/WarClassA" })
public class Servlet extends HttpServlet {

}
