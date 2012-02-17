package org.jboss.as.test.compat.jpa.hibernate.envers;

import java.io.File;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.compat.common.JndiUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author Strong Liu
 */
@RunWith(Arquillian.class)
public class HibernateEnvers3EmbeddedProviderTestCase {
    private static final String ARCHIVE_NAME = "hibernate3_test";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "<property name=\"hibernate.show_sql\" value=\"true\"/>" +
            "<property name=\"jboss.as.jpa.providerModule\" value=\"hibernate3-bundled\"/>" +
            "<property name=\"hibernate.ejb.event.post-insert\" value=\"org.hibernate.ejb.event.EJB3PostInsertEventListener,org.hibernate.envers.event.AuditEventListener\"/>"+
            "<property name=\"hibernate.ejb.event.post-update\" value=\"org.hibernate.ejb.event.EJB3PostUpdateEventListener,org.hibernate.envers.event.AuditEventListener\"/>"+
            "<property name=\"hibernate.ejb.event.post-delete\" value=\"org.hibernate.ejb.event.EJB3PostDeleteEventListener,org.hibernate.envers.event.AuditEventListener\"/>"+
            "<property name=\"hibernate.ejb.event.pre-collection-update\" value=\"org.hibernate.envers.event.AuditEventListener\"/>"+
            "<property name=\"hibernate.ejb.event.pre-collection-remove\" value=\"org.hibernate.envers.event.AuditEventListener\"/>"+
            "<property name=\"hibernate.ejb.event.post-collection-recreate\" value=\"org.hibernate.envers.event.AuditEventListener\"/>"+
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    private static void addHibernate3JarsToEar(EnterpriseArchive ear) {
        final String basedir = System.getProperty("basedir");
        final String testdir = basedir + File.separatorChar + "target" + File.separatorChar + "test-libs";
        File hibernatecore = new File(testdir, "hibernate3-core.jar");
        File hibernateannotations = new File(testdir, "hibernate3-commons-annotations.jar");
        File hibernateentitymanager = new File(testdir, "hibernate3-entitymanager.jar");
        File hibernateenvers = new File(testdir, "hibernate3-envers.jar");
        ear.addAsLibraries(
            hibernatecore,
            hibernateannotations,
            hibernateentitymanager,
            hibernateenvers
        );

    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create( EnterpriseArchive.class, ARCHIVE_NAME + ".ear" );
        addHibernate3JarsToEar(ear);

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SLSBPU.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Address.class, Person.class);
        lib.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(HibernateEnvers3EmbeddedProviderTestCase.class, JndiUtil.class);
        ear.addAsModule(main);

        // add application dependency on H2 JDBC driver, so that the Hibernate classloader (same as app classloader)
        // will see the H2 JDBC driver.
        // equivalent hack for use of shared Hiberante module, would be to add the H2 dependency directly to the
        // shared Hibernate module.
        // also add dependency on org.slf4j
        ear.addAsManifestResource(new StringAsset(
                "<jboss-deployment-structure>" +
                " <deployment>" +
                "  <dependencies>" +
                "   <module name=\"com.h2database.h2\" />" +
                "   <module name=\"org.antlr\" />" +
                "   <module name=\"org.apache.commons.collections\" />" +
                "   <module name=\"org.dom4j\" />" +
                "   <module name=\"org.slf4j\"/>" +
                "  </dependencies>" +
                " </deployment>" +
                "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");

        return ear;
    }

    @Test
    public void testSimpleEnversOperation() throws Exception {
        final SLSBPU slsbpu = JndiUtil.lookup(iniCtx, ARCHIVE_NAME, SLSBPU.class, SLSBPU.class);
		Person p1= slsbpu.createPerson( "Strong","Liu","kexueyuan source road",307 );
		Person p2= slsbpu.createPerson( "tom","cat","apache",34 );
		Address a1 =p1.getAddress();
		a1.setHouseNumber( 5 );

		p2.setAddress( a1 );
		slsbpu.updateAddress( a1 );
		slsbpu.updatePerson( p2 );

		int size = slsbpu.retrieveOldPersonVersionFromAddress( a1.getId() );
		Assert.assertEquals( 1, size );
    }


}
