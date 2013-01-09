package org.jboss.as.iiop;

import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface IIOPServiceNames {
    ServiceName POA_SERVICE_NAME = ServiceName.JBOSS.append("jacorb", "poa-service");
    ServiceName ROOT_SERVICE_NAME = POA_SERVICE_NAME.append("rootpoa");
    ServiceName INTERFACE_REPOSITORY_SERVICE_NAME = POA_SERVICE_NAME.append("irpoa");
    ServiceName NAMING_SERVICE_NAME = ServiceName.JBOSS.append("jacorb", "naming-service");
    ServiceName ORB_SERVICE_NAME = ServiceName.JBOSS.append("jacorb", "orb-service");
}
