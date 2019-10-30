/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import org.jboss.msc.service.ServiceName;

/**
 * Service Names for Eclipse MicroProfile Config objects.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public interface ServiceNames {
    ServiceName MICROPROFILE_CONFIG = ServiceName.JBOSS.append("eclipse", "microprofile", "config");

    ServiceName CONFIG_PROVIDER = MICROPROFILE_CONFIG.append("config-provider");
    ServiceName CONFIG_SOURCE = MICROPROFILE_CONFIG.append("config-source");
    ServiceName CONFIG_SOURCE_PROVIDER = MICROPROFILE_CONFIG.append("config-source-provider");
}
