package com.aem.practice.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.mailer.MessageGatewayService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

@Component(service=Servlet.class,immediate = true, configurationPid = "com.aem.practice.core.servlets.impl.SiteCheckerServlet",
           property={
                   Constants.SERVICE_DESCRIPTION + "=Site Checker Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths="+ "/bin/servefed/sitechecker"
           })
@Designate(ocd = SiteCheckerServlet.SiteCheckerServletConfiguration.class)
public class SiteCheckerServlet extends SlingSafeMethodsServlet {

	/**
	 * The Interface Configuration.
	 */
	@ObjectClassDefinition(name = "Site Checker Servlet Configuration")
	public @interface SiteCheckerServletConfiguration {

		@AttributeDefinition(name = "Email Template", description = "Provide the email template to be used.",
				type = AttributeType.STRING)
		String getEmailTemplate();
		
		@AttributeDefinition(name = "Email Subject", description = "Provide the email subject.",
				type = AttributeType.STRING)
		String getEmailSubject();
		
		@AttributeDefinition(name = "Sender Email Id", description = "Provide the emailId of the sender",
				type = AttributeType.STRING)
		String getSenderEmailId();
		
		
		@AttributeDefinition(name =  "Receiver Email Id", description = "Provide the emailId of the receivers(',' separated)",
				type = AttributeType.STRING)
		String getReceiverEmailId();
		
	}
	
	private String emailTemplate;
	
	private String receiverEmailId;
	
	private String emailSubject;
	
	private String senderEmailId;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SiteCheckerServlet.class);
    
	private static final long serialVersionUID = 8490767019793394020L;
	
	@Reference
	private MessageGatewayService messageGatewayService;

	/**
	 * Activate.
	 * @param config the config
	 */
	@Activate
	@Modified
	protected void activate(final SiteCheckerServletConfiguration config) {
		this.emailTemplate = config.getEmailTemplate();
		this.receiverEmailId = config.getReceiverEmailId();
		this.emailSubject = config.getEmailSubject();
		this.senderEmailId = config.getSenderEmailId();
	}
	
    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {
    	String siteName = req.getParameter("siteName");
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        if(sendEmail(req, siteName)) {
            resp.setStatus(SlingHttpServletResponse.SC_OK);
        	resp.getWriter().write("Email sent successfully");
        } else {
            resp.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	resp.getWriter().write("Something went wrong while sending mail");
        }
    }

	private boolean sendEmail(SlingHttpServletRequest req, String siteName) {
		LOGGER.info("Inside SendMail");
		final HtmlEmail email = new HtmlEmail();
        email.setCharset("UTF-8");
        LOGGER.info("Template:: "+ emailTemplate);
/*      final StringBuilder templateBody = new StringBuilder();
        templateBody.append(processTemplateData(emailTemplate, req.getResourceResolver()));
        String html = templateBody.toString();
        LOGGER.info("Template Data:: "+ html);*/
        String browserTime = req.getParameter("browserTime");
        String browserTimeZone = req.getParameter("browserTimeZone");
        
        try {
            Session session = req.getResourceResolver().adaptTo(Session.class);
        	try {
				final UserManager userManager = ((JackrabbitSession) session).getUserManager();
				User user = (User) userManager.getAuthorizable(session.getUserID());
		        String lastName = user.getProperty("./profile/familyName") != null ? user.getProperty("./profile/familyName")[0].toString() : StringUtils.EMPTY;
		        String firstName = user.getProperty("./profile/givenName") != null ? user.getProperty("./profile/givenName")[0].toString() : StringUtils.EMPTY;
		        emailSubject = emailSubject.replace("${firstName}", firstName);
		        emailSubject = emailSubject.replace("${lastName}", lastName);
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOGGER.info("RepositoryException mail::" + e);
			} 
        	emailSubject = emailSubject.replace("${siteName}", siteName);
        	emailSubject = emailSubject.replace("${time}", browserTime.concat(" ").concat(browserTimeZone));
	        LOGGER.info("ReceiverEmailId:: "+ receiverEmailId);
	        email.setFrom(senderEmailId);
	        email.setSubject(emailSubject);
	        email.setHtmlMsg(".");
	        if(receiverEmailId.contains(",")) {
				InternetAddress[] ia = InternetAddress.parse(receiverEmailId);
				for (InternetAddress internetAddress : ia) {
			        email.addTo(internetAddress.getAddress());
			        messageGatewayService.getGateway(HtmlEmail.class).send(email);
				}
	        } else {
	        	email.addTo(receiverEmailId);
		        messageGatewayService.getGateway(HtmlEmail.class).send(email);
	        }
	        LOGGER.info("successfully mail sent");
	        return true;
		} catch (EmailException | AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.info("error mail::" + e);
		}
		return false;
	}
	
    public static String processTemplateData(final String templatePath, final ResourceResolver resourceResolver) {
        String templateContent = StringUtils.EMPTY;
        final Node templateNode = resourceResolver.resolve(templatePath).adaptTo(Node.class);
        if (templateNode != null) {
            try {
                Node jcrContentNode = templateNode.getNode(JcrConstants.JCR_CONTENT);
                if (jcrContentNode != null && jcrContentNode.hasProperty(JcrConstants.JCR_DATA)) {
                    templateContent = IOUtils.toString(jcrContentNode.getProperty(JcrConstants.JCR_DATA).getBinary()
                            .getStream());
                }
			} catch (RepositoryException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        }
        return templateContent;
    }
}
