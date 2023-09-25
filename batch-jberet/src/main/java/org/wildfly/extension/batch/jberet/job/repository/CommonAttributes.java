/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.job.repository;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

public class CommonAttributes {

    /**
     * The limit of how many job execution records should be retrieved by the job repository.
     *
     * Setting this attribute allows improving application deployment time when the number of execution records in
     * the job repository grows too large.
     */
    public static final SimpleAttributeDefinition EXECUTION_RECORDS_LIMIT = SimpleAttributeDefinitionBuilder.create("execution-records-limit", ModelType.INT, true)
            .setRestartAllServices()
            .build();

}
