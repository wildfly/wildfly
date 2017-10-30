/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.jpa.eclipselink.wildfly8954;

import org.jboss.as.test.compat.jpa.eclipselink.EclipseLinkSharedModuleProviderTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This base class is based on the {@link EclipseLinkSharedModuleProviderTestCase}
 *
 */
public class PersistenceXmlHelperTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceXmlHelperTest.class);

    @Test
    public void dummyTest() throws Exception {
        String persistenceXml = PersistenceXmlHelper.SINGLETON.getWFLY8954BaseTestPersistenceXml();
        LOGGER.info(persistenceXml);

    }

}
