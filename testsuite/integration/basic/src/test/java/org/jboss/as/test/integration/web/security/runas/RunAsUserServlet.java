/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.security.runas;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.servlet.annotation.WebServlet;

/**
 * RunAs annotated servlet which calls protected EJB method {@link Hello#sayHello()}. Role used in RunAs in not correct role for
 * protected EJB method.
 *
 * @author olukas
 */
@WebServlet(RunAsUserServlet.SERVLET_PATH)
@DeclareRoles({HelloBean.AUTHORIZED_ROLE, HelloBean.NOT_AUTHORIZED_ROLE})
@RunAs(HelloBean.NOT_AUTHORIZED_ROLE)
public class RunAsUserServlet extends CallProtectedEjbServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/RunAsUserServlet";

}
