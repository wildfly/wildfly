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
package org.jboss.as.xts.jandex;

import org.jboss.as.xts.XTSException;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public enum BridgeType {

    DEFAULT, NONE, JTA;

    public static BridgeType build(AnnotationInstance annotationInstance) throws XTSException {

        if (annotationInstance.value("bridgeType") == null) {
            return DEFAULT;
        }

        String bridgeType = annotationInstance.value("bridgeType").asString();

        if (bridgeType.equals("DEFAULT")) {
            return DEFAULT;
        } else if (bridgeType.equals("NONE")) {
            return NONE;
        } else if (bridgeType.equals("JTA")) {
            return JTA;
        } else {
            throw new XTSException("Unexpected bridge type: '" + bridgeType + "'");
        }
    }
}