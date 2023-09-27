/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

/**
 * Strings holding the attributes used in the configuration of the jdr subsystem.
 *
 * @author Mike M. Clark
 * @author Tomaz Cerar
 */
public interface CommonAttributes {

    SimpleAttributeDefinition START_TIME = new SimpleAttributeDefinitionBuilder("start-time", ModelType.LONG, false).build();
    SimpleAttributeDefinition END_TIME = new SimpleAttributeDefinitionBuilder("end-time", ModelType.LONG, false).build();
    SimpleAttributeDefinition REPORT_LOCATION = new SimpleAttributeDefinitionBuilder("report-location", ModelType.STRING, true).build();
}
