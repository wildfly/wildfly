/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.subsystem;

import org.jboss.ejb.client.EJBIdentifier;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Constants {
    public static final String APP_NAME ="jsr-77";
    public static final String EJB_NAME ="EJB";
    public static final String MODULE_NAME ="jsr-77";
    public static final String DISTINCT_NAME = "";
    public static final EJBIdentifier EJB_IDENTIFIER = new EJBIdentifier(APP_NAME, MODULE_NAME, EJB_NAME, DISTINCT_NAME);

    public static final String JNDI_NAME = "ejb/mgmt/MEJB";
    public static final String JMX_DOMAIN = "jboss.jsr77";
}
