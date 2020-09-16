/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.manual.arquillian;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ServerKillProcessor;

/**
 * <p>
 * Provides a functionality for {@link Container#kill()} as WildFly managed container implementation
 * simulates it with stop. But the stop call fails as the WildFly handler expects the container being started.
 * </p>
 * <p>
 * As the kill call is used in some tests only for purpose announcing of crashed server to Arquillian
 * (ie. the container was stopped by the test intentionally without use of the Arquillian)
 * then this processor only receives the <code>kill</code> call and does nothing.
 * Arquillian knows that container is stopped and the call does not fail.
 * </p>
 * <p>
 * This Arquillian test issue was fixed by <a href="">WFARQ-70</a> and when 3.0.0.Final
 * will be used for WildFly testsuite this processor may be removed.
 * </p>
 */
public class WildFlyServerKillProcessor implements ServerKillProcessor {
    public void kill(Container container) throws Exception {
        // do nothing
    }
}
