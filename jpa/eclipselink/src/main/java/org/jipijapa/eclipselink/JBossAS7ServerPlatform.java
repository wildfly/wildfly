/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jipijapa.eclipselink;

import org.eclipse.persistence.sessions.DatabaseSession;

/**
 * The fully qualified name of JBossASServerPlatform must be set as the value
 * of the eclipselink.target-server property on EclipseLink version 2.3.2 and
 * older. In newer versions where bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=365704
 * has been fixed, setting eclipselink.target-server to "jboss" is sufficient.
 *
 * @author Craig Ringer <ringerc@ringerc.id.au>
 *
 * @deprecated use WildFlyServerPlatform instead
 */
public class JBossAS7ServerPlatform extends WildFlyServerPlatform {

    public JBossAS7ServerPlatform(DatabaseSession newDatabaseSession) {
            super(newDatabaseSession);
        }
}
