/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data;


import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.smoke.jakarta.data.lib.Author;
import org.jboss.as.test.smoke.jakarta.data.library.LibraryBoard;
import org.jboss.as.test.smoke.jakarta.data.publisher.Publisher;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.smoke.jakarta.data.lib.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Test for Jakarta Data support in EAR files, covering multiple deployments as well.
 * Also serves as a basic smoke test of Jakarta Data integration.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Preview.class, JakartaDataEarTestCase.JakartaDataSetupTask.class})
public class JakartaDataEarTestCase {

    private static final String PUBLISHER = "publisher";
    private static final String LIBRARY_BOARD = "libraryBoard";
    private static final String API = "api";
    private static final String PERSON = "person";
    private static final String AUTHOR = "author";
    private static final String BOOK = "book";
    private static final String BOOK_A = "Book A";
    private static final String BOOK_B = "Book B";
    private static final String BOOK_C = "Book C";
    private static final String LIBRARY = "library";
    private static final String LIBRARIAN = "librarian";

    public static final LocalDate ANDREA_HIRE_DATE = LocalDate.of(2025, 10, 17);

    private static final String GRAND_GLAIZE = "Grand Glaize";

    public static class JakartaDataSetupTask extends ManagementServerSetupTask {

        public JakartaDataSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .add("/extension=org.wildfly.extension.jakarta.data:add")
                            .add("/subsystem=jakarta-data:add()")
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=jakarta-data:remove()")
                            .add("/extension=org.wildfly.extension.jakarta.data:remove")
                            .endBatch()
                            .build())
                    .build());
        }

        @Override
        public void setup(ManagementClient client, String containerId) throws Exception {
            if (noneOf("ts.layers", "ts.bootable", "ts.bootable.preview")) {
                super.setup(client, containerId);
            }
        }

        @Override
        public void tearDown(ManagementClient client, String containerId) throws Exception {
            if (noneOf("ts.layers", "ts.bootable", "ts.bootable.preview")) {
                super.tearDown(client, containerId);
            }
        }

        private static boolean noneOf(final String... keys) {
            for (String key : keys) {
                if (System.getProperty(key) != null) {
                    return false;
                }
            }
            return true;
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        // Create the library JAR containing the base entities, recruiter repository
        JavaArchive libraryJar = ShrinkWrap.create(JavaArchive.class, "jakarta-data-ear-lib.jar")
                .addPackage(Author.class.getPackage())
                .addAsManifestResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"3.0\">\n" +
                        "    <persistence-unit name=\"jakarta-data-test\">\n" +
                        "        <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>\n" +
                        "        <properties>\n" +
                        "            <property name=\"jakarta.persistence.schema-generation.database.action\" value=\"drop-and-create\"/>\n" +
                        "        </properties>\n" +
                        "    </persistence-unit>\n" +
                        "</persistence>"), "persistence.xml");

        // Create the WAR containing the publisher logic
        WebArchive publisherWar = ShrinkWrap.create(WebArchive.class, PUBLISHER + ".war")
                .addPackage(Publisher.class.getPackage());

        // Create the WAR containing the library logic
        WebArchive libraryWar = ShrinkWrap.create(WebArchive.class, LIBRARY_BOARD + ".war")
                .addPackage(LibraryBoard.class.getPackage());

        // Create the EAR with the library JAR in the lib directory
        return ShrinkWrap.create(EnterpriseArchive.class, JakartaDataEarTestCase.class.getSimpleName() + ".ear")
                .addAsLibrary(libraryJar)
                .addAsModule(publisherWar)
                .addAsModule(libraryWar);
    }

    @Test
    public void testRepositories(@ContainerResource ManagementClient managementClient) {

        Assertions.assertNotNull(managementClient, "managementClient is null");
        String base = managementClient.getWebUri().toString();
        try (Client client = ClientBuilder.newClient()) {

            // Using both wars, confirm @Observes @Initialized(ApplicationScoped.class) activity was done

            performCheck(performGet(client, base, PUBLISHER, API, PERSON, Constants.ANDREA), 200, Constants.ANDREA, Constants.ANDREA_BDAY.toString());
            performCheck(performGet(client, base, PUBLISHER, API, PERSON, Constants.CHANTAL), 200, Constants.CHANTAL, Constants.CHANTAL_BDAY.toString());
            performCheck(performGet(client, base, LIBRARY_BOARD, API, PERSON, Constants.BARRY), 200, Constants.BARRY, Constants.BARRY_BDAY.toString());

            // Sign an author
            performCheck(performGet(client, base, PUBLISHER, API, AUTHOR, Constants.BARRY), 404);
            performCheck(performPut(client, base, PUBLISHER, API, AUTHOR, Constants.BARRY), 200, Constants.BARRY, Constants.BARRY_BDAY.toString());

            // Publish two books
            performCheck(performGet(client, base, PUBLISHER, API, BOOK, BOOK_A), 404);
            performCheck(performPut(client, bookParams(125, Constants.BARRY), base, PUBLISHER, API, BOOK, BOOK_A), 200, BOOK_A, "125", Constants.BARRY);
            performCheck(performGet(client, base, PUBLISHER, API, AUTHOR, Constants.BARRY), 200, Constants.BARRY, Constants.BARRY_BDAY.toString(), BOOK_A);

            performCheck(performGet(client, base, PUBLISHER, API, BOOK, BOOK_B), 404);
            performCheck(performPut(client, bookParams(983, Constants.BARRY), base, PUBLISHER, API, BOOK, BOOK_B), 200, BOOK_B, "983", Constants.BARRY);
            performCheck(performGet(client, base, PUBLISHER, API, AUTHOR, Constants.BARRY), 200, Constants.BARRY, Constants.BARRY_BDAY.toString(), BOOK_A, BOOK_B);

            // Recruit and Sign an author
            performCheck(performGet(client, base, PUBLISHER, API, PERSON, Constants.DEXTER), 404);
            performCheck(performPut(client, Map.of("birthday", Constants.DEXTER_BDAY.toString()), base, PUBLISHER, API, PERSON, Constants.DEXTER), 200);
            performCheck(performPut(client, base, PUBLISHER, API, AUTHOR, Constants.DEXTER), 200, Constants.DEXTER, Constants.DEXTER_BDAY.toString());

            // Publish a third book
            performCheck(performGet(client, base, PUBLISHER, API, BOOK, BOOK_C), 404);
            performCheck(performPut(client, bookParams(203, Constants.DEXTER), base, PUBLISHER, API, BOOK, BOOK_C), 200, BOOK_C, "203", Constants.DEXTER);
            performCheck(performGet(client, base, PUBLISHER, API, AUTHOR, Constants.DEXTER), 200, Constants.DEXTER, Constants.DEXTER_BDAY.toString(), BOOK_C);

            // Open a library

            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE), 404);
            performCheck(performPut(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE), 200, GRAND_GLAIZE);
            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE), 200, GRAND_GLAIZE);

            // Hire a librarian

            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, LIBRARIAN, Constants.ANDREA), 404);
            performCheck(performPut(client, Map.of("hireDate", ANDREA_HIRE_DATE.toString()), base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, LIBRARIAN, Constants.ANDREA),
                    200, Constants.ANDREA, Constants.ANDREA_BDAY.toString(), GRAND_GLAIZE, ANDREA_HIRE_DATE.toString());
            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, LIBRARIAN), 200, Constants.ANDREA);

            // Add two books

            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK, BOOK_A), 404);
            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK, BOOK_C), 404);

            performCheck(performPut(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK, BOOK_A), 200, BOOK_A, "125", Constants.BARRY);
            performCheck(performPut(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK, BOOK_C), 200, BOOK_C, "203", Constants.DEXTER);

            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK), 200, BOOK_A, "125", Constants.BARRY, BOOK_C, "203", Constants.DEXTER);

            // Remove a book

            performCheck(performDelete(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK, BOOK_A), 204);
            performCheck(performGet(client, base, LIBRARY_BOARD, API, LIBRARY, GRAND_GLAIZE, BOOK), 200, BOOK_C, "203", Constants.DEXTER);

        }
    }


    private void performCheck(final Response response, int status, String... expected) {
        try (response) {
            Assertions.assertEquals(status, response.getStatus(), response::toString);
            if (status == 200) {
                String entity = response.readEntity(String.class);
                for (String check : expected) {
                    Assertions.assertTrue(entity.contains(check), entity::toString);
                }
            }
        }
    }

    private Response performGet(final Client client, final String... requestElements)  {
        return getBuilder(client, Collections.emptyMap(), requestElements).get();
    }

    private Response performPut(final Client client, final String... requestElements)  {
        return performPut(client, Collections.emptyMap(), requestElements);
    }

    private Response performPut(final Client client, Map<String, String> queryParameters, final String... requestElements)  {

        return getBuilder(client, queryParameters, requestElements).put(null);
    }

    private Response performDelete(final Client client, final String... requestElements)  {
        return getBuilder(client, Collections.emptyMap(), requestElements).delete();
    }

    private Invocation.Builder getBuilder(final Client client, Map<String, String> queryParameters, final String... requestElements) {
        WebTarget target = client.target(String.join("/", requestElements));
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }
        return target.request()
                .accept(MediaType.APPLICATION_JSON);
    }

    private static Map<String, String> bookParams(int pageCount, String authorName) {
        return Map.of("pageCount", String.valueOf(pageCount), "authorName", authorName);
    }
}