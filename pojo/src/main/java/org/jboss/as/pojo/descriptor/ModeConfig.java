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

package org.jboss.as.pojo.descriptor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.msc.service.ServiceController;

/**
 * Mode config.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public enum ModeConfig {
    ACTIVE("active", ServiceController.Mode.ACTIVE),
    PASSIVE("passive", ServiceController.Mode.PASSIVE),
    ON_DEMAND("on demand", ServiceController.Mode.ON_DEMAND),
    NEVER("never", ServiceController.Mode.NEVER);

    private static final Map<String, ModeConfig> MAP = new HashMap<String, ModeConfig>();

    static {
        for(ModeConfig mode : values()) {
            MAP.put(mode.value, mode);
        }
    }

    private final String value;
    private final ServiceController.Mode mode;

    private ModeConfig(final String value, final ServiceController.Mode mode) {
        this.value = value;
        this.mode = mode;
    }

    public ServiceController.Mode getMode() {
        return mode;
    }

    static ModeConfig of(String value) {
        if (value == null)
            throw PojoLogger.ROOT_LOGGER.nullValue();

        final ModeConfig controllerMode = MAP.get(value.toLowerCase(Locale.ENGLISH));
        return controllerMode == null ? PASSIVE : controllerMode;
    }
}
