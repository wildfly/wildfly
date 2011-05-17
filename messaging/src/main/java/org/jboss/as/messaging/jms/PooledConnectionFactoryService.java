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

package org.jboss.as.messaging.jms;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.ra.HornetQResourceAdapter;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.services.ResourceAdapterActivatorService;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.as.txn.TxnServices;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.metadata.ironjacamar.IronJacamarParser;
import org.jboss.jca.common.metadata.ra.RaParser;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/13/11
 *         Time: 2:21 PM
 */
public class PooledConnectionFactoryService implements Service<String> {

        private static final String RAXML_START = "<connector xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
            "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            "           xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee" +
            "           http://java.sun.com/xml/ns/j2ee/connector_1_5.xsd\"" +
            "           version=\"1.5\">" +
            "" +
            "   <description>HornetQ 2.0 Resource Adapter</description>" +
            "   <display-name>HornetQ 2.0 Resource Adapter</display-name>" +
            "" +
            "   <vendor-name>Red Hat Middleware LLC</vendor-name>" +
            "   <eis-type>JMS 1.1 Server</eis-type>" +
            "   <resourceadapter-version>1.0</resourceadapter-version>" +
            "" +
            "   <license>" +
            "      <description>" +
            "Copyright 2009 Red Hat, Inc." +
            " Red Hat licenses this file to you under the Apache License, version" +
            " 2.0 (the \"License\"); you may not use this file except in compliance" +
            " with the License.  You may obtain a copy of the License at" +
            "   http://www.apache.org/licenses/LICENSE-2.0" +
            " Unless required by applicable law or agreed to in writing, software" +
            " distributed under the License is distributed on an \"AS IS\" BASIS," +
            " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or" +
            " implied.  See the License for the specific language governing" +
            " permissions and limitations under the License.  " +
            "      </description>" +
            "      <license-required>true</license-required>" +
            "   </license>" +
            "" +
            "   <resourceadapter>" +
            "      <resourceadapter-class>org.hornetq.ra.HornetQResourceAdapter</resourceadapter-class>";

    private static final String CONFIG_PROPERTY_TEMPLATE = "<config-property>"
                    + "<config-property-name>${NAME}</config-property-name>"
                    + "<config-property-type>${TYPE}</config-property-type>"
                    + "<config-property-value>${VALUE}</config-property-value>"
                    + "</config-property>";

    private static final String OUTBOUND_CONFIG =
            "     <outbound-resourceadapter>" +
            "         <connection-definition>" +
            "            <managedconnectionfactory-class>org.hornetq.ra.HornetQRAManagedConnectionFactory</managedconnectionfactory-class>" +
            "" +
            "            <config-property>" +
            "               <description>The default session type</description>" +
            "               <config-property-name>SessionDefaultType</config-property-name>" +
            "               <config-property-type>java.lang.String</config-property-type>" +
            "               <config-property-value>javax.jms.Queue</config-property-value>" +
            "            </config-property>" +
            "            <config-property>" +
            "               <description>Try to obtain a lock within specified number of seconds; less than or equal to 0 disable this functionality</description>" +
            "               <config-property-name>UseTryLock</config-property-name>" +
            "               <config-property-type>java.lang.Integer</config-property-type>" +
            "               <config-property-value>0</config-property-value>" +
            "            </config-property>" +
            "" +
            "            <connectionfactory-interface>org.hornetq.ra.HornetQRAConnectionFactory</connectionfactory-interface>" +
            "            <connectionfactory-impl-class>org.hornetq.ra.HornetQRAConnectionFactoryImpl</connectionfactory-impl-class>" +
            "            <connection-interface>javax.jms.Session</connection-interface>" +
            "            <connection-impl-class>org.hornetq.ra.HornetQRASession</connection-impl-class>" +
            "         </connection-definition>" +
            "         <transaction-support>XATransaction</transaction-support>" +
            "         <authentication-mechanism>" +
            "            <authentication-mechanism-type>BasicPassword</authentication-mechanism-type>" +
            "            <credential-interface>javax.resource.spi.security.PasswordCredential</credential-interface>" +
            "         </authentication-mechanism>" +
            "         <reauthentication-support>false</reauthentication-support>" +
            "      </outbound-resourceadapter>";

    private static final String INBOUND_CONFIG = "<inbound-resourceadapter>" +
            "         <messageadapter>" +
            "            <messagelistener>" +
            "               <messagelistener-type>javax.jms.MessageListener</messagelistener-type>" +
            "               <activationspec>" +
            "                  <activationspec-class>org.hornetq.ra.inflow.HornetQActivationSpec</activationspec-class>" +
            "                  <required-config-property>" +
            "                      <config-property-name>destination</config-property-name>" +
            "                  </required-config-property>" +
            "               </activationspec>" +
            "            </messagelistener>" +
            "         </messageadapter>" +
            "      </inbound-resourceadapter>";

    private String RAXML_END = "   </resourceadapter>" +
            "</connector>";

    private static final String JACAMAR_XML = "<ironjacamar xmlns=\"http://www.jboss.org/ironjacamar/schema\"" +
                "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\\\"" +
                "             xsi:schemaLocation=\"http://www.jboss.org/ironjacamar/schema http://www.jboss.org/ironjacamar/schema/ironjacamar_1_0.xsd\\\">" +
                "    <connection-definitions>" +
                "        <connection-definition class-name=\"org.hornetq.ra.HornetQRAManagedConnectionFactory\"" +
                "                               enabled=\"true\" jndi-name=\"${JNDI_NAME}\"" +
                "                               use-java-context=\"true\" pool-name=\"HornetQConnectionDefinition\">" +
                "        </connection-definition>" +
                "    </connection-definitions>" +
                "  <transaction-support>${transaction-support}</transaction-support>" +
                "</ironjacamar>";

    private static final String NAME = "${NAME}";

    private static final String TYPE = "${TYPE}";

    private static final String VALUE = "${VALUE}";

    private static final String JNDI_NAME = "${JNDI_NAME}";

    private static final String TX_SUPPORT = "${transaction-support}";

    private static final String CONNECTOR_CLASSNAME = "ConnectorClassName";

    private static final String CONNECTION_PARAMETERS = "ConnectionParameters";

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.hornet");

    private Injector<Object> transactionManager = new InjectedValue<Object>();

    private List<String> connectors;

    private List<PooledConnectionFactoryConfigProperties> adapterParams;

    private String name;

    private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();

    private InjectedValue<HornetQServer> hornetQService = new InjectedValue<HornetQServer>();

    private String jndiName;

    private String txSupport;

    public PooledConnectionFactoryService(String name, List<String> connectors, List<PooledConnectionFactoryConfigProperties> adapterParams, String jndiName, String txSupport) {
        this.name = name;
        this.connectors = connectors;
        this.adapterParams = adapterParams;
        this.jndiName = jndiName;
        this.txSupport = txSupport;
    }


    public String getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }


    public void start(StartContext context) throws StartException {
        ServiceTarget serviceTarget = context.getChildTarget();
        try {
            createService(serviceTarget);
        }
        catch (Exception e) {
            throw new StartException("failed to create resource adapter", e);
        }

    }

    private void createService(ServiceTarget serviceTarget) throws Exception {
        InputStream is = null;
        InputStream isIj = null;
        try {
            StringBuilder connectorClassname = new StringBuilder();
            StringBuilder connectorParams = new StringBuilder();
            for (String connector : connectors) {
                TransportConfiguration tc = hornetQService.getValue().getConfiguration().getConnectorConfigurations().get(connector);
                if(tc == null) {
                    throw new IllegalStateException("connector " + connector + " not defined");
                }
                if(connectorClassname.length() > 0) {
                    connectorClassname.append(",");
                    connectorParams.append(",");
                }
                connectorClassname.append(tc.getFactoryClassName());
                Map<String, Object> params = tc.getParams();
                boolean multiple = false;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if(multiple) {
                        connectorParams.append(";");
                    }
                    connectorParams.append(entry.getKey()).append("=").append(entry.getValue());
                    multiple = true;
                }
            }

            StringBuffer raxml = new StringBuffer(RAXML_START);

            if(connectorClassname.length() > 0) {
                String connectorClassnameConfig = CONFIG_PROPERTY_TEMPLATE;
                connectorClassnameConfig = connectorClassnameConfig.replace(NAME, CONNECTOR_CLASSNAME);
                connectorClassnameConfig = connectorClassnameConfig.replace(VALUE, connectorClassname);
                connectorClassnameConfig = connectorClassnameConfig.replace(TYPE, String.class.getName());
                raxml.append(connectorClassnameConfig);
            }
            if(connectorParams.length() > 0) {
                String connectorParamsConfig = CONFIG_PROPERTY_TEMPLATE;
                connectorParamsConfig = connectorParamsConfig.replace(NAME,  CONNECTION_PARAMETERS);
                connectorParamsConfig = connectorParamsConfig.replace(VALUE, connectorParams);
                connectorParamsConfig = connectorParamsConfig.replace(TYPE, String.class.getName());
                raxml.append(connectorParamsConfig);
            }
            for (PooledConnectionFactoryConfigProperties adapterParam : adapterParams) {
                String config = CONFIG_PROPERTY_TEMPLATE;
                config = config.replace(NAME, adapterParam.getName());
                config = config.replace(VALUE, adapterParam.getValue());
                config = config.replace(TYPE, adapterParam.getType());
                raxml.append(config);
            }

            raxml.append(OUTBOUND_CONFIG).append(INBOUND_CONFIG).append(RAXML_END);
            is = new ByteArrayInputStream(raxml.toString().getBytes("UTF-8"));
            RaParser raParser = new RaParser();
            Connector cmd = raParser.parse(is);
            String jacamarXml = JACAMAR_XML;
            jacamarXml = jacamarXml.replace(JNDI_NAME, jndiName);
            jacamarXml = jacamarXml.replace(TX_SUPPORT, txSupport);
            isIj = new ByteArrayInputStream(jacamarXml.getBytes("UTF-8"));
            IronJacamarParser ijParser = new IronJacamarParser();
            IronJacamar ijmd = ijParser.parse(isIj);
            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, ijmd,
                    HornetQResourceAdapter.class.getClassLoader(), name);

            serviceTarget
                    .addService(ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE, activator)
                    .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class,
                            activator.getMdrInjector())
                    .addDependency(ConnectorServices.RA_REPOSISTORY_SERVICE, ResourceAdapterRepository.class,
                            activator.getRaRepositoryInjector())
                    .addDependency(ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE, ManagementRepository.class,
                            activator.getManagementRepositoryInjector())
                    .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE,
                            ResourceAdapterDeploymentRegistry.class, activator.getRegistryInjector())
                    .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                            activator.getTxIntegrationInjector())
                    .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE,
                            JcaSubsystemConfiguration.class, activator.getConfigInjector())
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                            activator.getSubjectFactoryInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                            activator.getCcmInjector()).addDependency(NamingService.SERVICE_NAME)
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
        finally {
            if (is != null)
                is.close();
            if (isIj != null)
                isIj.close();
        }
    }


    public void stop(StopContext context) {

    }

//    public Injector<ResourceAdapterDeployment> getActivatorInjector() {
       // return activator;
   // }

    public Injector<Object> getTransactionManager() {
        return transactionManager;
    }

    Injector<SocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(socketBindings, name);
    }

    public Injector<HornetQServer> getHornetQService() {
        return hornetQService;
    }
}
