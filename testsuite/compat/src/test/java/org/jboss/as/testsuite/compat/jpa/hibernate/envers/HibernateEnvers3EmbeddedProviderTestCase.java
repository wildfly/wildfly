package org.jboss.as.testsuite.compat.jpa.hibernate.envers;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

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

    private static final String web_persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"web_mypc\">" +
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
        File dom4j = new File(testdir, "dom4j.jar");
        File slf4j = new File(testdir, "slf4j.jar");
        File slf4jApi = new File(testdir, "slf4j-api.jar");
        File commonCollections = new File(testdir, "commons-collections.jar");
        File antlr = new File(testdir, "antlr.jar");
        ear.addAsLibraries(
            hibernatecore,
            hibernateannotations,
            hibernateentitymanager,
            hibernateenvers,
            dom4j,
            slf4j,
            slf4jApi,
            commonCollections,
            antlr
        );

    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create( EnterpriseArchive.class, ARCHIVE_NAME + ".ear" );
        addHibernate3JarsToEar(ear);

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SLSBPU.class, HttpRequest.class);
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Address.class, Person.class);
        lib.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(HibernateEnvers3EmbeddedProviderTestCase.class);
        ear.addAsModule(main);

        // add war that contains its own pu
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(SimpleServlet.class, Person.class, Address.class);
        war.addAsResource(new StringAsset(web_persistence_xml), "META-INF/persistence.xml");

        war.addAsWebInfResource(
            new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<web-app version=\"3.0\"\n" +
                "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
                "         metadata-complete=\"false\">\n" +
                "<servlet-mapping>\n" +
                "        <servlet-name>SimpleServlet</servlet-name>\n" +
                "        <url-pattern>/simple/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "</web-app>"),
            "web.xml");

        ear.addAsModule(war);

        return ear;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + "beans/" + beanName + "!" + interfaceType.getName()));
        } catch (NamingException e) {
            dumpJndi("");
            throw e;
        }
    }

    // TODO: move this logic to a common base class (might be helpful for writing new tests)
    private static void dumpJndi(String s) {
        try {
            dumpTreeEntry(iniCtx.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private static void dumpTreeEntry(NamingEnumeration<NameClassPair> list, String s) throws NamingException {
        System.out.println("\ndump " + s);
        while (list.hasMore()) {
            NameClassPair ncp = list.next();
            System.out.println(ncp.toString());
            if (s.length() == 0) {
                dumpJndi(ncp.getName());
            } else {
                dumpJndi(s + "/" + ncp.getName());
            }
        }
    }

    @Test
    public void testSimpleEnversOperation() throws Exception {
        SLSBPU slsbpu = lookup("SLSBPU",SLSBPU.class);
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

    @Test
    public void testServletSubDeploymentRead() throws Exception {
        String result = HttpRequest.get("http://localhost:8080/hibernate3_test/simple?type=query", 10, SECONDS);
        assertEquals("2", result);

        result = HttpRequest.get("http://localhost:8080/hibernate3_test/simple?type=revision", 10, SECONDS);
        assertEquals("1", result);
    }
}
