package org.wildfly.iiop.openjdk;

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

/**
 *  @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;

public class AttributeConstants {

    private AttributeConstants() {
    }

    static final ModelNode FALSE_PROPERTY = new ModelNode().set("false");

    static final ModelNode TRUE_PROPERTY = new ModelNode().set("true");

    static final ModelNode NONE_PROPERTY = new ModelNode().set("none");

    static final ParameterValidator SSL_CONFIG_VALIDATOR =
            new EnumValidator<SSLConfigValue>(SSLConfigValue.class, true, false);

    static final ParameterValidator TRUE_FALSE_VALIDATOR = new EnumValidator<TrueFalse>(
            TrueFalse.class, true, false, TrueFalse.TRUE, TrueFalse.FALSE);

    static final SensitivityClassification IIOP_SECURITY =
            new SensitivityClassification(IIOPExtension.SUBSYSTEM_NAME, "iiop-security", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition IIOP_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(
            IIOP_SECURITY);

    public static enum TrueFalse {
        TRUE("true"), FALSE("false");

        private String value;

        private TrueFalse(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
