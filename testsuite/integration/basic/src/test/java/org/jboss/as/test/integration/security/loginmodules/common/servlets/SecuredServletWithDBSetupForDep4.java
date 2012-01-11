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
package org.jboss.as.test.integration.security.loginmodules.common.servlets;

import javax.annotation.sql.DataSourceDefinition;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;

/**
 * @author Jan Lanik
 *
 * Servlet class to be used in DatabaseLoginModule test cases
 */
@DataSourceDefinition(
   name = "java:jboss/datasources/LoginDSdep4",
   user = "sa",
   password = "sa",
   className = "org.h2.jdbcx.JdbcDataSource",
   url = "jdbc:h2:tcp://localhost/mem:test4"
)
@WebServlet(name = "SecuredServlet", urlPatterns = { "/secured/" }, loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = { "gooduser" }))
public class SecuredServletWithDBSetupForDep4 extends AbstractLoginModuleTestServlet {
}
