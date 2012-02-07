package org.jboss.as.test.integration.deployment.xml.datasource;

import java.util.Set;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface JpaRemote {

    public void addEmployee(String name);

    public Set<String> getEmployees();

}
