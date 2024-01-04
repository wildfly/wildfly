/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import org.jboss.msc.service.ServiceName;

/**
 * Service Names for MicroProfile Config objects.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public interface ServiceNames {
    ServiceName MICROPROFILE_CONFIG = ServiceName.JBOSS.append("eclipse", "microprofile", "config");

    ServiceName CONFIG_SOURCE = MICROPROFILE_CONFIG.append("config-source");

    ServiceName CONFIG_SOURCE_PROVIDER = MICROPROFILE_CONFIG.append("config-source-provider");

    ServiceName CONFIG_SOURCE_ROOT = MICROPROFILE_CONFIG.append("config-source-root");

}
