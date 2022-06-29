package org.jboss.as.test.integration.deployment.xml.datasource;

import java.util.Set;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface JpaRemote {

    void addEmployee(String name);

    Set<String> getEmployees();

}
