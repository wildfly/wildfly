package org.jboss.as.test.integration.ejb.remote.client.api.moreconnection;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;

@Stateless
public class HelloBean implements HelloBeanRemote {

    @Resource
    private DataSource injectedResource;

    /**
     *
     * @throws Throwable
     */
    public String hello(){
        return VALUE;
    }

}

