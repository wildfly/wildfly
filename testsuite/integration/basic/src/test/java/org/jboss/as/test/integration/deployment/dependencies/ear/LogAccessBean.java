/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.deployment.dependencies.ear;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

/**
 * A Singleton EJB - implementation of {@link LogAccess} interface.
 *
 * @author Josef Cacek
 */
@Singleton
@Startup
@Remote(LogAccess.class)
public class LogAccessBean implements LogAccess {

    private static Logger LOGGER = Logger.getLogger(LogAccessBean.class);

    // Public methods --------------------------------------------------------

    /**
     * Lifecycle hook.
     */
    @PostConstruct
    @PreDestroy
    public void logLifecycleAction() {
        LOGGER.trace("logLifecycleAction");
        Log.SB.append(getClass().getSimpleName());
    }

    /**
     * Returns {@link Log#SB} content.
     *
     * @return
     * @see org.jboss.as.test.integration.deployment.dependencies.ear.LogAccess#getLog()
     */
    public String getLog() {
        return Log.SB.toString();
    }

}
