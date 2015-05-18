package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.SearchResultType;
import com.redhat.gss.redhat_support_lib.parsers.SearchResultsType;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

/**
 * Utilizes methods from the search REST endpoint
 *
 * @author jkinlaw
 *
 */
public class Search extends BaseQuery {
    private static final Logger LOGGER = Logger.getLogger(API.class.getName());
    private ConnectionManager connectionManager = null;
    static String url = "/rs/search/";

    public Search(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Queries the API for the given ArticleType ID. RESTful method:
     * https://api.access.redhat.com/rs/ArticleTypes/<ArticleTypeID>
     *
     * @param artID
     *            The exact ArticleTypeID you are interested in.
     * @return A ArticleType object that represents the given ArticleType ID.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public List<SearchResultType> search(String[] keywords)
            throws RequestException, MalformedURLException {
        List<String> queryParams = new ArrayList<String>();
        for (String keyword : keywords) {
            queryParams.add("keyword=" + keyword);
        }
        String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig()
                .getUrl() + url, queryParams);
        SearchResultsType searchResults = get(
                connectionManager.getConnection(), fullUrl,
                SearchResultsType.class);
        return searchResults.getSearchResult();
    }
}