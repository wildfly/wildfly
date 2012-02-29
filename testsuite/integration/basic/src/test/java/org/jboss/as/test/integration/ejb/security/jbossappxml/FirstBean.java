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
package org.jboss.as.test.integration.ejb.security.jbossappxml;

import org.jboss.ejb3.annotation.RunAsPrincipal;

import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * Simple EJB3 bean that calls another bean with a run as
 *
 * @author anil saldhana
 */
@RunAs("Employee")
@RunAsPrincipal("javajoe")
@Stateless
@Remote(BeanInterface.class)
public class FirstBean implements BeanInterface {

    @EJB
    private SecondBean secondBean;

    public String getCallerPrincipal() {
        return secondBean.getCallerPrincipal();
    }

    @Override
    public boolean isCallerInRole(String roleName) {
        return secondBean.isCallerInRole(roleName);
    }
}