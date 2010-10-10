/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;


/**
 * Used to override System.exit() calls. For our tests we don't
 * want System.exit to have any effect.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SystemExiter {
    private static Exiter exiter;

    public static void initialize(Exiter exiter) {
        SystemExiter.exiter = exiter;
    }

    public static void exit(int status) {
        getExiter().exit(status);
    }

    private static Exiter getExiter() {
        return exiter == null ? new DefaultExiter() : exiter;
    }

    public interface Exiter {
        void exit(int status);
    }

    private static class DefaultExiter implements Exiter{
        public void exit(int status) {
            System.exit(status);
        }
    }
}
