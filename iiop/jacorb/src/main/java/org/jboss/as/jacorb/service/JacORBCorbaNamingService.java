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

package org.jboss.as.jacorb.service;

import org.jacorb.config.Configuration;
import org.jboss.as.iiop.naming.CorbaNamingContext;
import org.jboss.as.iiop.service.CorbaNamingService;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;

/**
 * <p>
 * This class implements a {@code Service} that provides the default CORBA naming service for JBoss to use.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JacORBCorbaNamingService extends CorbaNamingService {

    @Override
    protected void initNamingContext(final ORB orb, final POA namingPOA, final CorbaNamingContext ns) {
        Configuration configuration = ((org.jacorb.orb.ORB) orb).getConfiguration();
        boolean doPurge = configuration.getAttribute("jacorb.naming.purge", "off").equals("on");
        boolean noPing = configuration.getAttribute("jacorb.naming.noping", "off").equals("on");
        ns.init(namingPOA, doPurge, noPing);
    }
}
