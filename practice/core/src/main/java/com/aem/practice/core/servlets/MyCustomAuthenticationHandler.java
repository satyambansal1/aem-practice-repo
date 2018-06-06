package com.aem.practice.core.servlets;

import java.io.IOException;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.crx.security.token.TokenCookie;
import com.day.crx.security.token.TokenUtil;

@Component(immediate = true, property = { Constants.SERVICE_DESCRIPTION + "=My Custom Authentication Handler",
		AuthenticationHandler.PATH_PROPERTY + "=/content" })
public class MyCustomAuthenticationHandler extends DefaultAuthenticationFeedbackHandler
		implements AuthenticationHandler, AuthenticationFeedbackHandler {

	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private static final String REPO_DESC_ID = "crx.repository.systemid"; 
    private static final String REPO_DESC_CLUSTER_ID = "crx.cluster.id"; 
 
    private String repositoryId; 

	/**
	 * OSGi Service References *
	 */

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private SlingSettingsService slingSettings;

	@Reference
	private SlingRepository slingRepository;

	
	/** AuthenticationHandler Methods **/

	/**
	 * Extract the credentials contained inside the request, parameter or cookie
	 * 
	 * @see com
	 *      .day.cq.auth.impl.AbstractHTTPAuthHandler#authenticate(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {

		log.error("Begin Extract credentials");

		String extractedUserId = StringUtils.EMPTY;
		Cookie[] cookies =  request.getCookies();
		boolean isAEMAlreadyLoggedIn = false;
		boolean isLoginReq = false;
		if(null != cookies && cookies.length > 0) {
			for (Cookie cookie : cookies) {
				if("login-token".equalsIgnoreCase(cookie.getName())) {
					isAEMAlreadyLoggedIn = true;
					break;
				} else if("jwt".equalsIgnoreCase(cookie.getName())) {
					extractedUserId = cookie.getValue();
					isLoginReq = true;
				}
			}
		}
		
		if(!isAEMAlreadyLoggedIn) {
			if(isLoginReq) {
				Session adminSession = null; 
	            try { 
	                /*adminSession = slingRepository.loginAdministrative(null); 
	                if(!this.userExists(extractedUserId, adminSession)) { 
	                    this.createUser(extractedUserId, adminSession); 
	                } 
	                adminSession.save();*/
	                return TokenUtil.createCredentials(request, response, slingRepository, extractedUserId, true); 
	            } catch (RepositoryException e) { 
	                log.error("Repository error authenticating user: {} ~> {}", extractedUserId, e); 
	            } finally { 
	                if(adminSession != null) { 
	                    adminSession.logout(); 
	                } 
	            } 
			} else {
				return null;
			}
		} else {
			return null;
		}
		return AuthenticationInfo.FAIL_AUTH;

	}

	@Override
	public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
		// Remove credentials from the request/response
		// This generally removed removing/expiring auth Cookies
		// Remove the CRX Login Token cookie from the request 
        TokenCookie.update(request, response, this.repositoryId, null, null, true); 
	}

	@Override
	public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) {
		log.error("++ Begin Request credentials");

		// Invoked when an anonymous request is made to a resource this
		// authetication handler handles (based on OSGi paths properties)
		log.error("-- Begin Request credentials");

		// Also invoked after authenticatedFailed if this auth handler is the best match
		return true;
	}

	/**
	 * AuthenticationFeedbackHandler Methods *
	 */

	@Override
	public void authenticationFailed(HttpServletRequest request, HttpServletResponse response,
			AuthenticationInfo authInfo) {
		// Executes if authentication by the LoginModule fails

		// Executes after extractCredentials(..) returns a credentials object
		// that CANNOT be authenticated by the LoginModule

		log.error(">>>> Authentication failed");
		AuthUtil.sendInvalid(request, response); 
	}

	@Override
	public boolean authenticationSucceeded(HttpServletRequest request, HttpServletResponse response,
			AuthenticationInfo authInfo) {
		// Executes if authentication by the LoginModule succeeds
		log.error(">>>> Authentication succeeded");

		// Executes after extractCredentials(..) returns a credentials object
		// that CAN be authenticated by the LoginModule
		try {
			response.sendRedirect(request.getRequestURI());
		} catch (IOException ex) {
			log.error("Can not redirect to [{}]", request.getRequestURI());
			AuthUtil.sendInvalid(request, response); // 403 Forbidden
		}

		// Return true if the handler sent back a response to the client and request
		// processing should terminate.
		// Return false if the request should proceed as authenticated through the
		// framework. (This is usually the desired behavior)
		return false;
	}

	/**
	 * OSGi Component Methods *
	 */

	@Activate
	protected void activate(ComponentContext componentContext) {
		this.repositoryId = slingRepository.getDescriptor(REPO_DESC_CLUSTER_ID); 
        if(StringUtils.isBlank(this.repositoryId)) { this.repositoryId = slingRepository.getDescriptor(REPO_DESC_ID); } 
        if(StringUtils.isBlank(this.repositoryId)) { this.repositoryId = slingSettings.getSlingId(); } 
        if(StringUtils.isBlank(this.repositoryId)) { 
            this.repositoryId = UUID.randomUUID().toString(); 
            log.error("Unable to get Repository ID; falling back to a random UUID."); 
        } 
	}

	@Deactivate
	protected void deactivate(ComponentContext componentContext) {
	}

    private Authorizable getAuthorizable(final String userID, final Session session) throws RepositoryException { 
        final UserManager userManager = this.getUserManager(session); 
        return userManager.getAuthorizable(userID); 
    } 
 
    private boolean userExists(final String userId, final Session session) throws RepositoryException { 
        return null != getAuthorizable(userId, session); 
    } 
 
    private String createUser(final String userId, final Session adminSession) throws RepositoryException { 
        final UserManager userManager = this.getUserManager(adminSession); 
        final User user = userManager.createUser(userId, UUID.randomUUID().toString()); 
        return user.getPrincipal().getName(); 
    } 
    
    private UserManager getUserManager(final Session session) throws RepositoryException { 
        if(session instanceof JackrabbitSession) { 
            final UserManager userManager = ((JackrabbitSession) session).getUserManager(); 
            //userManager.autoSave(true); 
            return userManager; 
        } else { 
            throw new IllegalArgumentException("Session must be an instanceof JackrabbitSession"); 
        } 
    } 
}