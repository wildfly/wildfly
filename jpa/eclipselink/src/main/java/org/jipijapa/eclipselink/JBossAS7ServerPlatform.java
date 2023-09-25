/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
