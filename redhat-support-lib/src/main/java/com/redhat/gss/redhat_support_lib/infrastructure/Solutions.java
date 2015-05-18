package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.SolutionType;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Solutions extends BaseQuery {
    private static final Logger LOGGER = Logger.getLogger(API.class.getName());
    private ConnectionManager connectionManager = null;
    static String url = "/rs/solutions/";

    public Solutions(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Queries the API for the given solution ID. RESTful method:
     * https://api.access.redhat.com/rs/solutions/<solutionID>
     *
     * @param solNum
     *            The exact solutionID you are interested in.
     * @return A solution object that represents the given solution ID.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public SolutionType get(String solNum) throws RequestException,
            MalformedURLException {

        String fullUrl = connectionManager.getConfig().getUrl() + url + solNum;
        return get(connectionManager.getConnection(), fullUrl,
                SolutionType.class);
    }

    /**
     * Queries the solutions RESTful interface with a given set of keywords.
     * RESTful method: https://api.access.redhat.com/rs/solutions?keyword=NFS
     *
     * @param keywords
     *            A String array of keywords to search on.
     * @param kwargs
     *            Additional properties to filter on. The RESTful interface can
     *            only search on keywords; however, you can use this method to
     *            post-filter the results returned. Simply supply a String array
     *            of valid properties and their associated values.
     * @return A list of solution objects
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    @SuppressWarnings("unchecked")
    public List<SolutionType> list(String[] keywords, String[] kwargs)
            throws RequestException, MalformedURLException {

        List<String> queryParams = new ArrayList<String>();
        for (String keyword : keywords) {
            queryParams.add("keyword=" + keyword);
        }
        String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig()
                .getUrl() + url, queryParams);
        com.redhat.gss.redhat_support_lib.parsers.SolutionsType solutions = get(
                connectionManager.getConnection(), fullUrl,
                com.redhat.gss.redhat_support_lib.parsers.SolutionsType.class);
        return (List<SolutionType>) FilterHelper.filterResults(
                solutions.getSolution(), kwargs);
    }

    /**
     * Add a new solution
     *
     * @param sol
     *            The solution to be added.
     * @return The same solution with the ID and view_uri set if successful.
     * @throws Exception
     *             An exception if there was a connection, file open, etc.
     *             issue.
     */
    public SolutionType add(SolutionType sol) throws Exception {
        // TODO: Test once implemented

        String fullUrl = connectionManager.getConfig().getUrl() + url;
        Response resp = add(connectionManager.getConnection(), fullUrl, sol);
        MultivaluedMap<String, String> headers = resp.getStringHeaders();
        URL url = null;
        try {
            url = new URL(headers.getFirst("view-uri"));
        } catch (MalformedURLException e) {
            LOGGER.debug("Failed : Adding solution " + sol.getTitle()
                    + " was unsuccessful.");
            throw new Exception();
        }
        String path = url.getPath();
        sol.setId(path.substring(path.lastIndexOf('/') + 1, path.length()));
        sol.setViewUri(url.toString());
        return sol;
    }
}
