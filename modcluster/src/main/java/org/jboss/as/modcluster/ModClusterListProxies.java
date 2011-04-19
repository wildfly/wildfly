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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

// implements ModelQueryOperationHandler, DescriptionProvider
public class ModClusterListProxies implements OperationHandler, DescriptionProvider{

    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");

    static final String RESOURCE_NAME = ModClusterListProxies.class.getPackage().getName() + ".LocalDescriptions";
    static final ModClusterListProxies INSTANCE = new ModClusterListProxies();

    // private final InjectedValue<ModCluster> modcluster = new InjectedValue<ModCluster>();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get("list-proxies").set("list-proxies");
        node.get(DESCRIPTION).set(bundle.getString("modcluster.list-proxies"));
        // TODO Auto-generated method stub
        return node;
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceController<?> controller = context.getServiceRegistry().getService(ModClusterService.NAME);
                    ModCluster modcluster = (ModCluster) controller.getValue();
                    Map<InetSocketAddress, String>map = modcluster.getProxyInfo();
                    log.debugf("Mod_cluster ListProxies " + map);
                    if (!map.isEmpty()) {
                        final ModelNode result = new ModelNode();
                        Object [] addr = map.keySet().toArray();
                        for (int i=0; i<addr.length; i++) {
                            InetSocketAddress address = (InetSocketAddress) addr[i];
                            result.add(address.getHostName() + ":" + address.getPort());
                        }
                        resultHandler.handleResultFragment(new String[0], result);
                    }
                    resultHandler.handleResultComplete();

                }
            });
            } else {
                resultHandler.handleResultComplete();
            }

        return new BasicOperationResult();
    }
    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
