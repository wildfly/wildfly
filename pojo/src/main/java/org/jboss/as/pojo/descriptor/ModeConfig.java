/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
