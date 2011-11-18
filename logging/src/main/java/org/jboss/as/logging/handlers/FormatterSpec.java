/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.handlers;

import static org.jboss.as.logging.CommonAttributes.FORMATTER;

import java.io.Serializable;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.formatters.PatternFormatter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a> *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class FormatterSpec implements Serializable {

    private static final long serialVersionUID = -2383088369142242658L;

    public abstract void apply(Handler handler);

    public static FormatterSpec fromModelNode(final OperationContext context, final ModelNode node) throws OperationFailedException {
        return new PatternFormatterSpec(FORMATTER.resolveModelAttribute(context, node).asString());
    }


    private static final class PatternFormatterSpec extends FormatterSpec {

        private static final long serialVersionUID = -5660853321019821898L;

        private final String pattern;

        public PatternFormatterSpec(final String pattern) {
            this.pattern = pattern;
        }

        @Override
        public void apply(final Handler handler) {
            handler.setFormatter(new PatternFormatter(pattern));
        }
    }
}
