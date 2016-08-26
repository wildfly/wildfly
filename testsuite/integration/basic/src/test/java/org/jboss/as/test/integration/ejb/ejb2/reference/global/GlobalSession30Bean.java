/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.ejb2.reference.global;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless(name = "GlobalSession30")
@Remote(Session30RemoteBusiness.class)
public class GlobalSession30Bean implements Session30RemoteBusiness {
    private static final Logger log = Logger.getLogger(GlobalSession30Bean.class);

    public String access() {
        return "Session30";
    }

    public String access21() {
        return null;
    }

    public String accessLocalStateful() {
        return "not supported";
    }

    public String accessLocalStateful(String value) {
        return "not supported";
    }

    public String accessLocalStateful(String value, Integer suffix) {
        return "not supported";
    }

    public String globalAccess21() {
        try {
            InitialContext jndiContext = new InitialContext();
            Session21Home home = (Session21Home) jndiContext.lookup("java:global/global-reference-ejb2/Session21!" + Session21Home.class.getName());
            Session21 session = home.create();
            return session.access();
        } catch (Exception e) {
            log.error("GlobalSession30Bean.globalAccess21()", e);
            return null;
        }
    }

}
