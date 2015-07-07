package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.util.List;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.parsers.GroupType;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Groups extends BaseQuery {
    private ConnectionManager connectionManager = null;
    static String url = "/rs/groups/";

    public Groups(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Queries the API for the given case number. RESTful method:
     * https://api.access.redhat.com/rs/groups/
     *
     *
     * @return A case object that represents the given case number.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    @SuppressWarnings("unchecked")
    public List<GroupType> list() throws RequestException,
            MalformedURLException {
        String fullUrl = connectionManager.getConfig().getUrl() + url;
        com.redhat.gss.redhat_support_lib.parsers.GroupsType groups = get(
                connectionManager.getConnection(), fullUrl,
                com.redhat.gss.redhat_support_lib.parsers.GroupsType.class);
        return (List<GroupType>) FilterHelper.filterResults(groups.getGroup(),
                null);
    }
}