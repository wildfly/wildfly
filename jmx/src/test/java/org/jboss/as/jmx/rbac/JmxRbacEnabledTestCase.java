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
package org.jboss.as.jmx.rbac;

import org.jboss.as.controller.access.rbac.StandardRole;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JmxRbacEnabledTestCase extends JmxRbacTestCase {

    public JmxRbacEnabledTestCase() {
        super(true);
    }

    @Override
    protected boolean canRead(StandardRole standardRole, boolean sensitiveMBeans) {
        if (!sensitiveMBeans) {
            return true;
        }
        switch (standardRole) {
        case SUPERUSER:
        case ADMINISTRATOR:
        case AUDITOR:
            return true;
        default:
            return false;
        }
    }

    @Override
    protected boolean canWrite(StandardRole standardRole, boolean sensitiveMBeans) {
        if (!sensitiveMBeans) {
            switch (standardRole) {
            case MONITOR:
            case DEPLOYER:
            case AUDITOR:
                return false;
            default:
                return true;
            }
        }
        switch (standardRole) {
        case SUPERUSER:
        case ADMINISTRATOR:
            return true;
        default:
            return false;
        }
    }

    @Override
    protected boolean canAccessSpecial(StandardRole standardRole) {
        return standardRole == StandardRole.ADMINISTRATOR || standardRole == StandardRole.SUPERUSER;
    }

}
