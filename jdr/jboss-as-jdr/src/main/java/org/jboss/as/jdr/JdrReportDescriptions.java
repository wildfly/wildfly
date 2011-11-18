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

package org.jboss.as.jdr;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Utility methods to generate detyped descriptions of JDR subsystem resources and operations.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public class JdrReportDescriptions {

    private static final String RESOURCE_NAME = JdrReportDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getJdrSubsystemDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(DESCRIPTION).set(bundle.getString("jdr.subsystem"));
        return result;
    }

    public static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set(JdrReportSubsystemAdd.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("jdr.add"));
        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    public static ModelNode getSubsystemRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set(JdrReportSubsystemRemove.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("jdr.remove"));
        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    public static ModelNode getJdrRequestDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set(JdrReportRequestHandler.OPERATION_NAME);
        result.get(DESCRIPTION).set(bundle.getString("jdr.request"));
        result.get(REQUEST_PROPERTIES).setEmptyObject();
        result.get(REPLY_PROPERTIES, DESCRIPTION).set("jdr.report.return");
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "start-time", DESCRIPTION).set(bundle.getString("jdr.report.return.starttime"));
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "start-time", TYPE).set(ModelType.STRING);
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "start-time", REQUIRED).set(true);
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "end-time", DESCRIPTION).set(bundle.getString("jdr.report.return.endtime"));
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "end-time", TYPE).set(ModelType.STRING);
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "end-time", REQUIRED).set(true);
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "report-location", DESCRIPTION).set(bundle.getString("jdr.report.return.location"));
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "report-location", TYPE).set(ModelType.STRING);
        result.get(REPLY_PROPERTIES, VALUE_TYPE, "report-location", REQUIRED).set(false);

        return result;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
