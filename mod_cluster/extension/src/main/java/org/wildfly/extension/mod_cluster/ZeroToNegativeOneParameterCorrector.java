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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.ParameterCorrector;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ParameterCorrector} that converts 0 to -1. Used for attributes that have historically allowed -1 as
 * a "disabled" value but otherwise require a positive value. This parameter corrector allows an IntRangeValidator
 * with a range starting at -1 to be used, while avoiding having 0 as a value. Ideally, "undefined" would mean disabled,
 * and an IntRangeValidator starting at 1 would be used, but these -1 attributes have been out in the wild for quite
 * a while now so they need to be supported.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ZeroToNegativeOneParameterCorrector implements ParameterCorrector {

    static final ZeroToNegativeOneParameterCorrector INSTANCE = new ZeroToNegativeOneParameterCorrector();

    @Override
    public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
        if (newValue.isDefined() && newValue.getType() != ModelType.EXPRESSION) {
            try {
                long val = newValue.asLong();
                if (val == 0) {
                    return new ModelNode(-1);
                }
            } catch (Exception e) {
                // not convertible; let the validator that the caller will invoke later deal with this
            }
        }
        return newValue;
    }
}
