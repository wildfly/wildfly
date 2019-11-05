/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
