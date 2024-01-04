/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import org.jboss.as.controller.OperationFailedException;

/**
 * Used to create a JBoss Diagnostic Report (JDR).
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public interface JdrReportCollector {
    /**
     * Create a JDR report.
     *
     * @return information about the generated report.
     */
    JdrReport collect() throws OperationFailedException;
}
