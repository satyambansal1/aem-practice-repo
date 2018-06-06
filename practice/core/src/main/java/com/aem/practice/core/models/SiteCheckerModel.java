package com.aem.practice.core.models;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

@Model(adaptables=SlingHttpServletRequest.class)
public class SiteCheckerModel {

    private String name;
    
    private ArrayList<String> sites;
    
    @SlingObject
    private ResourceResolver resourceResolver;
    
    @SlingObject
    SlingHttpServletRequest request;
    
    @SlingObject
    SlingHttpServletResponse response;

    @PostConstruct
    protected void init() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException, IOException {
    	Session session = resourceResolver.adaptTo(Session.class);
    	final UserManager userManager = ((JackrabbitSession) session).getUserManager(); 
    	if(null == userManager || StringUtils.isBlank(session.getUserID()) || "anonymous".equalsIgnoreCase(session.getUserID())) {
    		response.sendRedirect("/content/serve-fed/portal/login.html");
    	} else {
	    	User user = (User) userManager.getAuthorizable(session.getUserID());
	        name = user.getProperty("./profile/familyName") != null ? user.getProperty("./profile/familyName")[0].toString() : null;
	        sites = new ArrayList<String>();
	        if(user.getProperty("./profile/sites") != null) {
	        	Value[] sitesArr = user.getProperty("./profile/sites");
	        	for (Value value : sitesArr) {
					sites.add(value.getString());
				}
	        }
    	}
        
    }

	public String getName() {
		return name;
	}

	public ArrayList<String> getSites() {
		return sites;
	}


}
