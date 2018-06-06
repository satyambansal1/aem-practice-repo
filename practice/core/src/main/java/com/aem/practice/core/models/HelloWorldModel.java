/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.aem.practice.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;

@Model(adaptables=Resource.class)
public class HelloWorldModel {

    @Inject
    private SlingSettingsService settings;

    @Inject @Named("sling:resourceType") @Default(values="No resourceType")
    protected String resourceType;

    private String message;
    
    @SlingObject
    private ResourceResolver resourceResolver;

    @PostConstruct
    protected void init() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
    	Session session = resourceResolver.adaptTo(Session.class);
    	final UserManager userManager = ((JackrabbitSession) session).getUserManager(); 
    	User user = (User) userManager.getAuthorizable(session.getUserID());
        message = "\tHello ";
        message += user.getProperty("./profile/familyName") != null ? user.getProperty("./profile/familyName")[0].toString() : null;
    }

    public String getMessage() {
        return message;
    }
}
