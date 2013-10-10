package org.jboss.as.model.test;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.junit.Assert;
import org.junit.Test;

public class MavenUtilTestCase {


    @Test
    public void testRepositorySystem() {
        RepositorySystem repositorySystem = MavenUtil.newRepositorySystem();
        Assert.assertNotNull(repositorySystem);
    }

    @Test
    public void testRepositorySystemDirect() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setErrorHandler(new MavenUtil.MyErrorHandler());

        RepositorySystem repoSys = locator.getService(RepositorySystem.class);
        Assert.assertNotNull(repoSys);
    }



}