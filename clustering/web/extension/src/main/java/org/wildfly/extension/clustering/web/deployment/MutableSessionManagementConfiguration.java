/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.UnaryOperator;

import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.extension.clustering.web.SessionGranularity;

/**
 * @author Paul Ferraro
 */
public class MutableSessionManagementConfiguration implements DistributableSessionManagementConfiguration, UnaryOperator<String> {

    private final UnaryOperator<String> replacer;

    private SessionGranularity granularity;

    MutableSessionManagementConfiguration(UnaryOperator<String> replacer) {
        this.replacer = replacer;
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return (this.granularity != null) ? this.granularity.getAttributePersistenceStrategy() : null;
    }

    public void setSessionGranularity(String value) {
        this.granularity = SessionGranularity.valueOf(this.replacer.apply(value));
    }

    @Override
    public String apply(String value) {
        return this.replacer.apply(value);
    }
}
