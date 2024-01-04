/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.descriptor;

import org.jboss.as.server.deployment.AttachmentKey;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The object representation of a legacy "jboss-service.xml" descriptor file.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class JBossServiceXmlDescriptor implements Serializable {

    private static final long serialVersionUID = 3148478338698997486L;

    public static final AttachmentKey<JBossServiceXmlDescriptor> ATTACHMENT_KEY = AttachmentKey.create(JBossServiceXmlDescriptor.class);

    private ControllerMode controllerMode = ControllerMode.PASSIVE;
    private List<JBossServiceConfig> serviceConfigs;

    public List<JBossServiceConfig> getServiceConfigs() {
        return serviceConfigs;
    }

    public void setServiceConfigs(List<JBossServiceConfig> serviceConfigs) {
        this.serviceConfigs = serviceConfigs;
    }

    public ControllerMode getControllerMode() {
        return controllerMode;
    }

    public void setControllerMode(ControllerMode controllerMode) {
        this.controllerMode = controllerMode;
    }

    public static enum ControllerMode {
         ACTIVE("active"), PASSIVE("passive"), ON_DEMAND("on demand"), NEVER("never");

        private static final Map<String, ControllerMode> MAP = new HashMap<String, ControllerMode>();

        static {
            for(ControllerMode mode : ControllerMode.values()) {
                MAP.put(mode.value, mode);
            }
        }

        private final String value;

        private ControllerMode(final String value) {
            this.value = value;
        }

        static ControllerMode of(String value) {
            final ControllerMode controllerMode = MAP.get(value);
            return controllerMode == null ? PASSIVE : controllerMode;
        }
    }
}
