/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.access;

import org.jboss.dmr.ModelNode;

/**
 * The result of an access control decision.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AuthorizationResult {

    public enum Decision {
        PERMIT,
        DENY
    }

    public static final AuthorizationResult PERMITTED = new AuthorizationResult(Decision.PERMIT);

    private static final ModelNode NO_EXPLANATION = new ModelNode();
    static {
        NO_EXPLANATION.protect();
    }

    private final Decision decision;
    private final ModelNode explanation;

    public AuthorizationResult(Decision decision) {
        this(decision, NO_EXPLANATION);
    }

    public AuthorizationResult(Decision decision, ModelNode explanation) {
        this.decision = decision;
        this.explanation = explanation == null ? NO_EXPLANATION : explanation;
    }

    public Decision getDecision() {
        return decision;
    }

    public ModelNode getExplanation() {
        return explanation;
    }
}
