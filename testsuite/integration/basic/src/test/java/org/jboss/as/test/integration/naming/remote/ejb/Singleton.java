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

package org.jboss.as.test.integration.naming.remote.ejb;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author John Bailey
 */
@Stateless
public class Singleton implements BinderRemote {
    public String echo(String value) {
        return "Echo: " + value;
    }

    private final String JNDI_NAME = "java:jboss/exported/some/entry";

    // methods to do JNDI binding for remote access
    public void bind() {
        try {
            InitialContext ic = new InitialContext();
            ic.bind(JNDI_NAME, "Test");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void rebind() {
        try {
            InitialContext ic = new InitialContext();
            ic.rebind(JNDI_NAME, "Test2");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void unbind() {
        try {
            InitialContext ic = new InitialContext();
            ic.unbind(JNDI_NAME);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
