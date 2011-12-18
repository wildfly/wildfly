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

package org.jboss.as.xts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.as.webservices.service.EndpointPublishService;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jbossts.XTSService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.publish.Context;


/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
class XTSSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final XTSSubsystemAdd INSTANCE = new XTSSubsystemAdd();

    private static final String[] WAR_DEPLOYMENT_NAMES = {
            "ws-c11.war",
            "ws-t11-coordinator.war",
            "ws-t11-participant.war",
            "ws-t11-client.war",
    };


    /**
     * class used to record the url pattern and service endpoint implementation class name of
     * an XTS JaxWS endpoint associatd wiht one of the XTS context paths. this is equivalent
     * to the information contained in a single matched pair of servlet:servletName and
     * servlet-mapping:url-pattern fields in the web.xml
     */
    private static class EndpointInfo {
        String SEIClassname;
        String URLPattern;
        EndpointInfo(String seiClassname, String urlPattern) {
            this.SEIClassname = seiClassname;
            this.URLPattern = urlPattern;
        }
    }

    /**
     * class grouping togeher details of all XTS JaxWS endpoints associated with a given XTS context
     * path. this groups together all the paired servlet:servletName and* servlet-mapping:url-pattern
     * fields in the web.xml
     */
    static class ContextInfo {
        String contextPath;
        EndpointInfo[] endpointInfo;
        ContextInfo(String contextPath, EndpointInfo[] endpointInfo) {
            this.contextPath = contextPath;
            this.endpointInfo = endpointInfo;
        }
    }

    /**
     * a collection of all the context and associated endpoint information for the XTS JaxWS endpoints.
     * this is the bits of the variosu web.xml files whcih are necessary to deploy via the endpoint
     * publisher API rather than via war files containing web.xml descriptors
     */
    private static final ContextInfo[] contextDefinitions = {
            new ContextInfo("ws-c11",
                    new EndpointInfo[] {
                            new EndpointInfo("com.arjuna.webservices11.wscoor.sei.ActivationPortTypeImpl", "ActivationService"),
                            new EndpointInfo("com.arjuna.webservices11.wscoor.sei.RegistrationPortTypeImpl", "RegistrationService")
                            }),
            new ContextInfo("ws-t11-coordinator",
                    new EndpointInfo[] {
                            new EndpointInfo("com.arjuna.webservices11.wsat.sei.CoordinatorPortTypeImpl", "CoordinatorService"),
                            new EndpointInfo("com.arjuna.webservices11.wsat.sei.CompletionCoordinatorPortTypeImpl", "CompletionCoordinatorService"),
                            new EndpointInfo("com.arjuna.webservices11.wsat.sei.CompletionCoordinatorRPCPortTypeImpl", "CompletionCoordinatorRPCService"),
                            new EndpointInfo("com.arjuna.webservices11.wsba.sei.BusinessAgreementWithCoordinatorCompletionCoordinatorPortTypeImpl", "BusinessAgreementWithCoordinatorCompletionCoordinatorService"),
                            new EndpointInfo("com.arjuna.webservices11.wsba.sei.BusinessAgreementWithParticipantCompletionCoordinatorPortTypeImpl", "BusinessAgreementWithParticipantCompletionCoordinatorService"),
                            new EndpointInfo("com.arjuna.webservices11.wsarjtx.sei.TerminationCoordinatorPortTypeImpl", "TerminationCoordinatorService"),
                            new EndpointInfo("com.arjuna.webservices11.wsarjtx.sei.TerminationCoordinatorRPCPortTypeImpl", "TerminationCoordinatorRPCService")
                            }),
            new ContextInfo("ws-t11-participant",
                    new EndpointInfo[] {
                            new EndpointInfo("com.arjuna.webservices11.wsat.sei.ParticipantPortTypeImpl", "ParticipantService"),
                            new EndpointInfo("com.arjuna.webservices11.wsba.sei.BusinessAgreementWithCoordinatorCompletionParticipantPortTypeImpl", "BusinessAgreementWithCoordinatorCompletionParticipantService"),
                            new EndpointInfo("com.arjuna.webservices11.wsba.sei.BusinessAgreementWithParticipantCompletionParticipantPortTypeImpl", "BusinessAgreementWithParticipantCompletionParticipantService"),
                            }),
            new ContextInfo("ws-t11-client",
                    new EndpointInfo[] {
                            new EndpointInfo("com.arjuna.webservices11.wsat.sei.CompletionInitiatorPortTypeImpl", "CompletionInitiatorService"),
                            new EndpointInfo("com.arjuna.webservices11.wsarjtx.sei.TerminationParticipantPortTypeImpl", "TerminationParticipantService")
                            })
            };

    /**
     * the hsot name used when deploying endpoints for the local host via the endpoint publisher service
     */
    private static final String ENDPOINT_SERVICE_HOST_NAME = "default-host";

    private static final Logger log = Logger.getLogger("org.jboss.as.transactions");

    private XTSSubsystemAdd() {
    }

    static ContextInfo[] getContextDefinitions() {
        return contextDefinitions;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get(CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.URL).set(operation.get(CommonAttributes.XTS_ENVIRONMENT).require(ModelDescriptionConstants.URL));

    }


    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String coordinatorURL = model.get(CommonAttributes.XTS_ENVIRONMENT).hasDefined(ModelDescriptionConstants.URL) ? model.get(CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.URL).asString() : null;
        if (coordinatorURL != null && log.isDebugEnabled()) {
            log.debugf("nodeIdentifier=%s\n", coordinatorURL);
        }


        final ServiceTarget target = context.getServiceTarget();

                // TODO eventually we should add a config service which manages the XTS configuration
                // this will allow us to include a switch enabling or disabling deployment of
                // endpoints specific to client, coordinator or participant and then deploy
                // and redeploy the relevant endpoints as needed/ the same switches can be used
                // byte the XTS service to decide whether to perfomr client, coordinator or
                // participant initialisation. we shoudl also provide config switches which
                // decide whether to initialise classes and deploy services for AT, BA or both.
                // for now we will just deploy all the endpoints and always do client, coordinator
                // and participant init for both AT and BA.

                // add an endpoint publisher service for each of the required endpoint contexts
                // specifying all the relevant URL patterns and SEI classes

                final ClassLoader loader = XTSService.class.getClassLoader();
                ServiceBuilder<Context> endpointBuilder;
                ArrayList<ServiceController<Context>> controllers = new ArrayList<ServiceController<Context>>();
                for (ContextInfo contextInfo : contextDefinitions) {
                    String contextName = contextInfo.contextPath;
                    Map<String, String> map = new HashMap<String, String>();
                    for (EndpointInfo endpointInfo : contextInfo.endpointInfo) {
                        map.put(endpointInfo.URLPattern, endpointInfo.SEIClassname);
                    }
                    endpointBuilder = EndpointPublishService.createServiceBuilder(target, contextName, loader,
                            ENDPOINT_SERVICE_HOST_NAME, map);

                    controllers.add(endpointBuilder.setInitialMode(Mode.ACTIVE)
                        .install());
                }

                // add an XTS service which depends on all the WS endpoints

                final XTSManagerService xtsService = new XTSManagerService(coordinatorURL);

                // this service needs to depend on the transaction recovery service
                // because it can only initialise XTS recovery once the transaction recovery
                // service has initialised the orb layer

                ServiceBuilder<?> xtsServiceBuilder = target.addService(XTSServices.JBOSS_XTS_MAIN, xtsService);
                xtsServiceBuilder
                        .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER);

                // this service needs to depend on JBossWS Config Service to be notified of the JBoss WS config (bind address, port etc)
                xtsServiceBuilder.addDependency(WSServices.CONFIG_SERVICE, ServerConfig.class, xtsService.getWSServerConfig());

                // the service also needs to depend on the endpoint services
                for (ServiceController<Context> controller : controllers) {
                    xtsServiceBuilder.addDependency(controller.getName());
                }

                xtsServiceBuilder
                        .setInitialMode(Mode.ACTIVE)
                        .install();

                // WS-AT / JTA Transaction bridge services:

                final TxBridgeInboundRecoveryService txBridgeInboundRecoveryService = new TxBridgeInboundRecoveryService();
                ServiceBuilder<?> txBridgeInboundRecoveryServiceBuilder =
                        target.addService(XTSServices.JBOSS_XTS_TXBRIDGE_INBOUND_RECOVERY, txBridgeInboundRecoveryService);
                txBridgeInboundRecoveryServiceBuilder.addDependency(XTSServices.JBOSS_XTS_MAIN);

                txBridgeInboundRecoveryServiceBuilder.setInitialMode(Mode.ACTIVE).install();

                final TxBridgeOutboundRecoveryService txBridgeOutboundRecoveryService = new TxBridgeOutboundRecoveryService();
                ServiceBuilder<?> txBridgeOutboundRecoveryServiceBuilder =
                        target.addService(XTSServices.JBOSS_XTS_TXBRIDGE_OUTBOUND_RECOVERY, txBridgeOutboundRecoveryService);
                txBridgeOutboundRecoveryServiceBuilder.addDependency(XTSServices.JBOSS_XTS_MAIN);

                txBridgeOutboundRecoveryServiceBuilder.setInitialMode(Mode.ACTIVE).install();

    }
}
