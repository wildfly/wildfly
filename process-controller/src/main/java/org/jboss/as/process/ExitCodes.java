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
package org.jboss.as.process;

/**
 * Reserved process exit codes handled differently
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExitCodes {

    /**
     * Exit code which indicates normal process exit.
     */
    public static final int NORMAL = 0;

    /**
     * Exit code which indicates a process failure. In a managed domain, if the process emitting this code is
     * a {@code HostController}, the process controller will react to this code by attempting to respawn the host
     * controller. For other processes this code has no special semantic associated with it.
     */
    public static final int FAILED = 1;

    /**
     * Exit code which serves two functions:
     * <ul>
     *   <li>
     *     <b>standalone mode:</b> - if a standalone server's exit code the startup script will start up the server again.
     *  </li>
     *  <li>
     *     <b>domain mode:</b> - if the host controller process returns this exit code, the process controller will terminate
     *      with this exit code, and the domain's startup script will start up the process controller again, which in turn will start up
     *      the host controller and any {@code autostart="true"} servers.
     *  </li>
     * </ul>
     */
    public static final int RESTART_PROCESS_FROM_STARTUP_SCRIPT = 10;

    /**
     * Exit code from host controller which indicates a critical boot failure. The process controller
     * will only try to respawn the host controller if there are no running servers; if there aren't,
     * the process controller will shut down in turn. If there are running servers, the process controller
     * will attempt to respawn the host controller, but with longer delays between respawn attempts
     * than would be the case with {@link #FAILED}. The expectation is a critical boot failure will require
     * some sort of human intervention to resolve, so immediately respawning will just result in
     * noise in the logs.
     */
    public static final int HOST_CONTROLLER_ABORT_EXIT_CODE = 99;
}
