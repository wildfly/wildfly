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

package org.jboss.as.domain.management.plugin;

/**
 * An optional interface that plug-ins can implemented to receive lifecycle
 * notifications during the authentication process.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
interface PlugInLifecycleSupport extends PlugInConfigurationSupport {

    /**
     * Called after the authentication process has finished regardless of the outcome to allow the
     * plug-ins to clean up any resources.
     *
     * If a plug-in is defined for both the authentication and authorization steps the order that end
     * is called is opposite to the order for init so the authorization plug-in is called first and then
     * the authentication plug-in - the order is reversed in-case any resources of the authorization plug-in
     * build on resources opened by the authentication plug-in.
     *
     * The shared state map passed in to the init method is valid for sharing state until after the last
     * end method has returned - at that point the Map and any remaining contents will be left to be
     * garbage collected.
     */
    void end();

}
