/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import org.jboss.jandex.DotName;

/**
 * Class that stores the {@link DotName}s of MP FT annotations.
 *
 * @author Radoslav Husar
 */
public enum MicroProfileFaultToleranceAnnotation {

    /**
     * @see org.eclipse.microprofile.faulttolerance.Retry
     */
    RETRY("Retry"),
    /**
     * @see org.eclipse.microprofile.faulttolerance.Timeout
     */
    TIMEOUT("Timeout"),
    /**
     * @see org.eclipse.microprofile.faulttolerance.Fallback
     */
    FALLBACK("Fallback"),
    /**
     * @see org.eclipse.microprofile.faulttolerance.CircuitBreaker
     */
    CIRCUIT_BREAKER("CircuitBreaker"),
    /**
     * @see org.eclipse.microprofile.faulttolerance.Bulkhead
     */
    BULKHEAD("Bulkhead"),
    /**
     * @see org.eclipse.microprofile.faulttolerance.Asynchronous
     */
    ASYNCHRONOUS("Asynchronous"),
    ;

    private final DotName dotName;

    MicroProfileFaultToleranceAnnotation(String simpleName) {
        this.dotName = DotName.createComponentized(FaultToleranceDotName.ORG_ECLIPSE_MICROPROFILE_FAULTTOLERANCE, simpleName);
    }

    private interface FaultToleranceDotName {
        DotName ORG_ECLIPSE_MICROPROFILE_FAULTTOLERANCE = DotName.createComponentized(DotName.createComponentized(DotName.createComponentized(DotName.createComponentized(null, "org"), "eclipse"), "microprofile"), "faulttolerance");
    }

    public DotName getDotName() {
        return dotName;
    }

}
