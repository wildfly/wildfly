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
package org.jboss.as.test.integration.auditlog.module.custom;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.audit.spi.AuditLogEventFormatterSupport;
import org.jboss.as.controller.audit.spi.CustomAuditLogEventFormatter;
import org.jboss.as.controller.audit.spi.JmxAccessAuditLogEvent;
import org.jboss.as.controller.audit.spi.ModelControllerAuditLogEvent;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestCustomAuditLogEventFormatter extends AuditLogEventFormatterSupport implements CustomAuditLogEventFormatter {
    private static Map<String, String> properties = new HashMap<String, String>();

    public TestCustomAuditLogEventFormatter(String name) {
        super(name);
    }

    @Override
    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    @Override
    public String updateProperty(String name, String value) {
        return properties.put(name, value);
    }

    @Override
    public void deleteProperty(String name) {
        properties.remove(name);
    }

    @Override
    public String formatAuditLogItem(ModelControllerAuditLogEvent item) {
        return format(item.getOperations().get(0).get(OP).asString());
    }

    @Override
    public String formatAuditLogItem(JmxAccessAuditLogEvent item) {
        return format(item.getMethodName());
    }

    private String format(String opName) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append("[");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            sb.append("]");
        }
        sb.append(opName);
        return sb.toString();
    }
}
