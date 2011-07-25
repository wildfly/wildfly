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

package org.jboss.as.mc.service;

import org.jboss.logging.Logger;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;

/**
 * Abstract MC pojo phase; it handles install/uninstall
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractPojoPhase {
    protected Logger log = Logger.getLogger(getClass());

    private InjectedValue<Joinpoint>[] installs;
    private InjectedValue<Joinpoint>[] uninstalls;

    protected void executeInstalls() throws StartException {
        if (installs == null || installs.length == 0)
            return;

        int i = 0;
        try {
            for (i = 0; i < installs.length; i++)
                installs[i].getValue().dispatch();
        } catch (Throwable t) {
            considerUninstalls(uninstalls, i);
            throw new StartException(t);
        }
    }

    /**
     * Consider the uninstalls.
     *
     * This method is here to be able to override
     * the behavior after installs failed.
     * e.g. perhaps only running uninstalls from the index.
     *
     * By default we run all uninstalls in the case
     * at least one install failed.
     *
     * @param uninstalls the uninstalls
     * @param index current installs index
     */
    protected void considerUninstalls(InjectedValue<Joinpoint>[] uninstalls, int index) {
        if (uninstalls == null)
            return;

        for (int j = Math.min(index, uninstalls.length - 1); j >= 0; j--) {
            try {
                uninstalls[j].getValue().dispatch();
            } catch (Throwable t) {
                log.warn("Ignoring uninstall action on target: " + uninstalls[j], t);
            }
        }
    }

    protected void executeUninstalls() {
        considerUninstalls(uninstalls, Integer.MAX_VALUE);
    }

    public void setInstalls(InjectedValue<Joinpoint>[] installs) {
        this.installs = installs;
    }

    public void setUninstalls(InjectedValue<Joinpoint>[] uninstalls) {
        this.uninstalls = uninstalls;
    }
}
