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

package org.jboss.as.core.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.controller.ModelVersion;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Makes adjustments to a model node to bring it in conformance with another model node, in order to prevent
 * spurious differences between the two being detected when a subsequent comparison is performed.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public interface ModelHarmonizer {

    /**
     * Make some adjustment to {@code target} to bring it in conformance with {@code source} so
     * subsequent comparisons between {@code source} and {@code target} will not report spurious differences.
     *
     * @param modelVersion the version of the models
     * @param source the source mode
     * @param target the target model
     */
    void harmonizeModel(ModelVersion modelVersion, ModelNode source, ModelNode target);

    ModelHarmonizer UNDEFINED_TO_EMPTY = new ModelHarmonizer() {
        @Override
        public void harmonizeModel(ModelVersion modelVersion, ModelNode source, ModelNode target) {
            if (source.getType() == ModelType.OBJECT && source.asInt() == 0 && !target.isDefined()) {
                target.setEmptyObject();
            }
        }
    };

    ModelHarmonizer MISSING_NAME = new ModelHarmonizer() {
        @Override
        public void harmonizeModel(ModelVersion modelVersion, ModelNode source, ModelNode target) {
            if (source.hasDefined(NAME) && !target.hasDefined(NAME)) {
                target.get(NAME).set(source.get(NAME));
            }
        }
    };
}
