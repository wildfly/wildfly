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
package org.jboss.as.controller.audit;

import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.audit.AuditLogItem.JmxAccessAuditLogItem;
import org.jboss.as.controller.audit.AuditLogItem.ModelControllerAuditLogItem;
import org.jboss.dmr.ModelNode;

/**
 * All methods on this class should be called with {@link ManagedAuditLoggerImpl}'s lock taken.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JsonAuditLogItemFormatter extends AuditLogItemFormatter {

    private static final ModelNode UNDEFINED = new ModelNode();
    static {
        UNDEFINED.protect();
    }
    public static final String TYPE = "type";
    public static final String READ_ONLY = "r/o";
    public static final String BOOTING = "booting";
    public static final String AS_VERSION = "version";
    public static final String USER_ID = "user";
    public static final String DOMAIN_UUID = "domainUUID";
    public static final String ACCESS_MECHANISM = "access";
    public static final String REMOTE_ADDRESS = "remote-address";
    public static final String OPERATIONS = "ops";
    public static final String SUCCESS = "success";
    public static final String METHOD_NAME = "method";
    public static final String METHOD_SIGNATURE = "sig";
    public static final String METHOD_PARAMETERS = "params";
    public static final String ERROR = "error";

    private volatile boolean compactJson;
    private volatile boolean escapeNewLine;
    private volatile boolean escapeControlCharacters;

    public JsonAuditLogItemFormatter(String name, boolean includeDate, String dateSeparator, String dateFormat,
            boolean compactJson, boolean escapeNewLine, boolean escapeControlCharacters) {
        super(name, includeDate, dateSeparator, dateFormat);
        this.compactJson = compactJson;
        this.escapeNewLine = escapeNewLine;
        this.escapeControlCharacters = escapeControlCharacters;
    }

    public boolean isCompactJson() {
        return compactJson;
    }

    public void setCompactJson(boolean compactJson) {
        this.compactJson = compactJson;
    }

    public void setEscapeNewLine(boolean escapeNewLine) {
        this.escapeNewLine = escapeNewLine;
    }

    public void setEscapeControlCharacters(boolean escapeControlCharacters) {
        this.escapeControlCharacters = escapeControlCharacters;
    }

    @Override
    public String formatAuditLogItem(ModelControllerAuditLogItem item) {
        String formattedString = getCachedString();
        if (formattedString != null) {
            return formattedString;
        }

        ModelNode formatted = new ModelNode();
        formatted.get(TYPE).set(TYPE_CORE);
        addCommonFields(item, formatted);
        formatted.get(SUCCESS).set(item.getResultAction() == ResultAction.KEEP);
        formatted.get(OPERATIONS).set(item.getOperations());

        return cacheString(createRecordText(item, formatted));
    }

    @Override
    public String formatAuditLogItem(JmxAccessAuditLogItem item) {
        String formattedString = getCachedString();
        if (formattedString != null) {
            return formattedString;
        }
        ModelNode formatted = new ModelNode();
        formatted.get(TYPE).set(TYPE_JMX);
        addCommonFields(item, formatted);

        formatted.get(METHOD_NAME).set(item.getMethodName());

        formatted.get(METHOD_SIGNATURE);
        for (String sig : item.getMethodSignature()) {
            formatted.get(METHOD_SIGNATURE).add(sig);
        }

        formatted.get(METHOD_PARAMETERS);
        for (Object param : item.getMethodParams()) {
            //TODO handle arrays better
            formatted.get(METHOD_PARAMETERS).add(param == null ? UNDEFINED : new ModelNode(param.toString()));
        }

        final Throwable throwable = item.getError();
        if (throwable != null) {
            //TODO include stack trace?
            formatted.get(ERROR).set(throwable.getMessage());
        }

        return cacheString(createRecordText(item, formatted));
    }

    private String createRecordText(AuditLogItem item, ModelNode formatted) {
        StringBuilder sb = new StringBuilder();

        appendDate(sb, item);

        sb.append(formatted.toJSONString(compactJson));

        String formattedString;
        if (escapeNewLine && !escapeControlCharacters) {
            //Escape all instances of "\n" with "#012"
            formattedString = sb.toString().replace("\n", "#012");
        } else if (escapeControlCharacters) {
            StringBuilder escaped = new StringBuilder();
            for (int i = 0 ; i < sb.length() ; i++) {
                char c = sb.charAt(i);
                if (c >= 0 && c < 32) {
                    escaped.append('#');
                    if (c < 8) {
                        escaped.append('0').append('0').append((int) c);
                    } else {
                        escaped.append('0').append(c >> 3).append(c & 0x07);
                    }
                } else {
                    escaped.append(c);
                }
            }
            formattedString = escaped.toString();
        } else {
            formattedString = sb.toString();
        }

        return formattedString;
    }

    private void addCommonFields(AuditLogItem item, ModelNode formatted) {
        formatted.get(READ_ONLY).set(item.isReadOnly());
        formatted.get(BOOTING).set(item.isBooting());
        formatted.get(AS_VERSION).set(item.getAsVersion());
        formatted.get(USER_ID);
        if (item.getUserId() != null) {
            formatted.get(USER_ID).set(item.getUserId());
        }
        formatted.get(DOMAIN_UUID);
        if (item.getDomainUUID() != null) {
            formatted.get(DOMAIN_UUID).set(item.getDomainUUID());
        }
        formatted.get(ACCESS_MECHANISM);
        if (item.getAccessMechanism() != null) {
            formatted.get(ACCESS_MECHANISM).set(item.getAccessMechanism().toString());
        }
        formatted.get(REMOTE_ADDRESS);
        if (item.getRemoteAddress() != null) {
            formatted.get(REMOTE_ADDRESS).set(item.getRemoteAddress().toString());
        }
    }

    public static void getJsonFormatter(String nameFromAddress) {
    }

}
