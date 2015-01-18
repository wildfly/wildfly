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

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.connector.subsystems.datasources.Namespace;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractDsRemove extends ContainerResourceMgmtTestBase {

    static final String DATASOURCE_NAME = "RemoveDS";

    static class DataSourceSetupTask extends AbstractMgmtServerSetupTask {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            String xml = FileUtils.readFile(this.getClass(), "remove-ds.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.CURRENT.getUriString(), new DataSourcesExtension.DataSourceSubsystemParser());
            boolean skipFirst = true;
            for (ModelNode operation: operations) {
                if (skipFirst) {
                    skipFirst = false;
                    continue;
                }
                executeOperation(operation);
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            remove(datasourceAddress());
        }
    }

    protected static ModelNode datasourceAddress() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "datasources");
        address.add("data-source", DATASOURCE_NAME);
        return address;
    }

    protected String performCall(URL url,String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, TimeoutUtil.adjust(2), SECONDS);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T lookupEJB(final String moduleName, Class<? extends T> beanImplClass, Class<T> remoteInterface) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);

        return (T) context.lookup("ejb:/" + moduleName + "/" + beanImplClass.getSimpleName() + "!"
                + remoteInterface.getName());
    }

}
