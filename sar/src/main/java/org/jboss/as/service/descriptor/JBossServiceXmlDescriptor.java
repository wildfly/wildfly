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
