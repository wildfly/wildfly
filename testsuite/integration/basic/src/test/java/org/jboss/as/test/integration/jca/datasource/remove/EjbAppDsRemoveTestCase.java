/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.datasource.remove;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

/**
 * Tests the removal of a data-source used in an ejb deployment.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(AbstractDsRemove.DataSourceSetupTask.class)
public class EjbAppDsRemoveTestCase extends AbstractDsRemove {

    private static final String MODULE_NAME = "remove-ds";

    @Deployment
    public static JavaArchive deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(RemoveDsBean.class, RemoveDsBeanRemote.class, TimeoutUtil.class);
        return jar;
    }

    @Test
    public void testDatasourceRemove() throws Exception {
        final RemoveDsBeanRemote bean = lookupEJB(MODULE_NAME, RemoveDsBean.class, RemoveDsBeanRemote.class);
        bean.testDatasource();
        try {
            remove(datasourceAddress());
            fail("Removal of the datasource should have failed.");
        } catch (MgmtOperationException expected) {
        }
        bean.testDatasource();
    }

}
