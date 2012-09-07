/*
 * Copyright 2011 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.PropsUtil;
import com.vaadin.DefaultDeploymentConfiguration;
import com.vaadin.server.AbstractCommunicationManager.Callback;
import com.vaadin.server.ServletPortletHelper.ApplicationClassException;
import com.vaadin.server.VaadinSession.SessionStartEvent;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * Portlet 2.0 base class. This replaces the servlet in servlet/portlet 1.0
 * deployments and handles various portlet requests from the browser.
 * 
 * TODO Document me!
 * 
 * @author peholmst
 */
public class VaadinPortlet extends GenericPortlet implements Constants {

    public static final String RESOURCE_URL_ID = "APP";

    public static class PortletService extends AbstractVaadinService {
        private final VaadinPortlet portlet;

        public PortletService(VaadinPortlet portlet,
                DeploymentConfiguration deploymentConfiguration) {
            super(deploymentConfiguration);
            this.portlet = portlet;
        }

        protected VaadinPortlet getPortlet() {
            return portlet;
        }

        @Override
        public String getConfiguredWidgetset(WrappedRequest request) {

            String widgetset = getDeploymentConfiguration()
                    .getApplicationOrSystemProperty(PARAMETER_WIDGETSET, null);

            if (widgetset == null) {
                // If no widgetset defined for the application, check the
                // portal property
                widgetset = WrappedPortletRequest.cast(request)
                        .getPortalProperty(PORTAL_PARAMETER_VAADIN_WIDGETSET);
            }

            if (widgetset == null) {
                // If no widgetset defined for the portal, use the default
                widgetset = DEFAULT_WIDGETSET;
            }

            return widgetset;
        }

        @Override
        public String getConfiguredTheme(WrappedRequest request) {

            // is the default theme defined by the portal?
            String themeName = WrappedPortletRequest.cast(request)
                    .getPortalProperty(Constants.PORTAL_PARAMETER_VAADIN_THEME);

            if (themeName == null) {
                // no, using the default theme defined by Vaadin
                themeName = DEFAULT_THEME_NAME;
            }

            return themeName;
        }

        @Override
        public boolean isStandalone(WrappedRequest request) {
            return false;
        }

        @Override
        public String getStaticFileLocation(WrappedRequest request) {
            String staticFileLocation = WrappedPortletRequest.cast(request)
                    .getPortalProperty(
                            Constants.PORTAL_PARAMETER_VAADIN_RESOURCE_PATH);
            if (staticFileLocation != null) {
                // remove trailing slash if any
                while (staticFileLocation.endsWith(".")) {
                    staticFileLocation = staticFileLocation.substring(0,
                            staticFileLocation.length() - 1);
                }
                return staticFileLocation;
            } else {
                // default for Liferay
                return "/html";
            }
        }

        @Override
        public String getMimeType(String resourceName) {
            return getPortlet().getPortletContext().getMimeType(resourceName);
        }

        @Override
        public SystemMessages getSystemMessages() {
            return ServletPortletHelper.DEFAULT_SYSTEM_MESSAGES;
        }

        @Override
        public File getBaseDirectory() {
            PortletContext context = getPortlet().getPortletContext();
            String resultPath = context.getRealPath("/");
            if (resultPath != null) {
                return new File(resultPath);
            } else {
                try {
                    final URL url = context.getResource("/");
                    return new File(url.getFile());
                } catch (final Exception e) {
                    // FIXME: Handle exception
                    getLogger()
                            .log(Level.INFO,
                                    "Cannot access base directory, possible security issue "
                                            + "with Application Server or Servlet Container",
                                    e);
                }
            }
            return null;
        }

    }

    public static class WrappedHttpAndPortletRequest extends
            WrappedPortletRequest {

        public WrappedHttpAndPortletRequest(PortletRequest request,
                HttpServletRequest originalRequest, PortletService vaadinService) {
            super(request, vaadinService);
            this.originalRequest = originalRequest;
        }

        private final HttpServletRequest originalRequest;

        @Override
        public String getParameter(String name) {
            String parameter = super.getParameter(name);
            if (parameter == null) {
                parameter = originalRequest.getParameter(name);
            }
            return parameter;
        }

        @Override
        public String getRemoteAddr() {
            return originalRequest.getRemoteAddr();
        }

        @Override
        public String getHeader(String name) {
            String header = super.getHeader(name);
            if (header == null) {
                header = originalRequest.getHeader(name);
            }
            return header;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> parameterMap = super.getParameterMap();
            if (parameterMap == null) {
                parameterMap = originalRequest.getParameterMap();
            }
            return parameterMap;
        }
    }

    public static class WrappedGateinRequest extends
            WrappedHttpAndPortletRequest {
        public WrappedGateinRequest(PortletRequest request,
                PortletService vaadinService) {
            super(request, getOriginalRequest(request), vaadinService);
        }

        private static final HttpServletRequest getOriginalRequest(
                PortletRequest request) {
            try {
                Method getRealReq = request.getClass().getMethod(
                        "getRealRequest");
                HttpServletRequestWrapper origRequest = (HttpServletRequestWrapper) getRealReq
                        .invoke(request);
                return origRequest;
            } catch (Exception e) {
                throw new IllegalStateException("GateIn request not detected",
                        e);
            }
        }
    }

    public static class WrappedLiferayRequest extends
            WrappedHttpAndPortletRequest {

        public WrappedLiferayRequest(PortletRequest request,
                PortletService vaadinService) {
            super(request, getOriginalRequest(request), vaadinService);
        }

        @Override
        public String getPortalProperty(String name) {
            return PropsUtil.get(name);
        }

        private static HttpServletRequest getOriginalRequest(
                PortletRequest request) {
            try {
                // httpRequest = PortalUtil.getHttpServletRequest(request);
                HttpServletRequest httpRequest = (HttpServletRequest) PortalClassInvoker
                        .invoke("com.liferay.portal.util.PortalUtil",
                                "getHttpServletRequest", request);

                // httpRequest =
                // PortalUtil.getOriginalServletRequest(httpRequest);
                httpRequest = (HttpServletRequest) PortalClassInvoker.invoke(
                        "com.liferay.portal.util.PortalUtil",
                        "getOriginalServletRequest", httpRequest);
                return httpRequest;
            } catch (Exception e) {
                throw new IllegalStateException("Liferay request not detected",
                        e);
            }
        }

    }

    public static class AbstractApplicationPortletWrapper implements Callback {

        private final VaadinPortlet portlet;

        public AbstractApplicationPortletWrapper(VaadinPortlet portlet) {
            this.portlet = portlet;
        }

        @Override
        public void criticalNotification(WrappedRequest request,
                WrappedResponse response, String cap, String msg,
                String details, String outOfSyncURL) throws IOException {
            portlet.criticalNotification(WrappedPortletRequest.cast(request),
                    (WrappedPortletResponse) response, cap, msg, details,
                    outOfSyncURL);
        }
    }

    /**
     * This portlet parameter is used to add styles to the main element. E.g
     * "height:500px" generates a style="height:500px" to the main element.
     */
    public static final String PORTLET_PARAMETER_STYLE = "style";

    /**
     * This portal parameter is used to define the name of the Vaadin theme that
     * is used for all Vaadin applications in the portal.
     */
    public static final String PORTAL_PARAMETER_VAADIN_THEME = "vaadin.theme";

    public static final String WRITE_AJAX_PAGE_SCRIPT_WIDGETSET_SHOULD_WRITE = "writeAjaxPageScriptWidgetsetShouldWrite";

    // TODO some parts could be shared with AbstractApplicationServlet

    // TODO Can we close the application when the portlet is removed? Do we know
    // when the portlet is removed?

    private PortletService vaadinService;
    private AddonContext addonContext;

    @Override
    public void init(PortletConfig config) throws PortletException {
        super.init(config);
        Properties initParameters = new Properties();

        // Read default parameters from the context
        final PortletContext context = config.getPortletContext();
        for (final Enumeration<String> e = context.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, context.getInitParameter(name));
        }

        // Override with application settings from portlet.xml
        for (final Enumeration<String> e = config.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, config.getInitParameter(name));
        }

        DeploymentConfiguration deploymentConfiguration = createDeploymentConfiguration(initParameters);
        vaadinService = createPortletService(deploymentConfiguration);

        addonContext = new AddonContext(vaadinService);
        addonContext.init();
    }

    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        return new DefaultDeploymentConfiguration(getClass(), initParameters);
    }

    protected PortletService createPortletService(
            DeploymentConfiguration deploymentConfiguration) {
        return new PortletService(this, deploymentConfiguration);
    }

    @Override
    public void destroy() {
        super.destroy();

        addonContext.destroy();
    }

    protected enum RequestType {
        FILE_UPLOAD, UIDL, RENDER, STATIC_FILE, APPLICATION_RESOURCE, DUMMY, EVENT, ACTION, UNKNOWN, BROWSER_DETAILS, CONNECTOR_RESOURCE, HEARTBEAT;
    }

    protected RequestType getRequestType(WrappedPortletRequest wrappedRequest) {
        PortletRequest request = wrappedRequest.getPortletRequest();
        if (request instanceof RenderRequest) {
            return RequestType.RENDER;
        } else if (request instanceof ResourceRequest) {
            ResourceRequest resourceRequest = (ResourceRequest) request;
            if (ServletPortletHelper.isUIDLRequest(wrappedRequest)) {
                return RequestType.UIDL;
            } else if (isBrowserDetailsRequest(resourceRequest)) {
                return RequestType.BROWSER_DETAILS;
            } else if (ServletPortletHelper.isFileUploadRequest(wrappedRequest)) {
                return RequestType.FILE_UPLOAD;
            } else if (ServletPortletHelper
                    .isConnectorResourceRequest(wrappedRequest)) {
                return RequestType.CONNECTOR_RESOURCE;
            } else if (ServletPortletHelper
                    .isApplicationResourceRequest(wrappedRequest)) {
                return RequestType.APPLICATION_RESOURCE;
            } else if (ServletPortletHelper.isHeartbeatRequest(wrappedRequest)) {
                return RequestType.HEARTBEAT;
            } else if (isDummyRequest(resourceRequest)) {
                return RequestType.DUMMY;
            } else {
                return RequestType.STATIC_FILE;
            }
        } else if (request instanceof ActionRequest) {
            return RequestType.ACTION;
        } else if (request instanceof EventRequest) {
            return RequestType.EVENT;
        }
        return RequestType.UNKNOWN;
    }

    private boolean isBrowserDetailsRequest(ResourceRequest request) {
        return request.getResourceID() != null
                && request.getResourceID().equals("browserDetails");
    }

    private boolean isDummyRequest(ResourceRequest request) {
        return request.getResourceID() != null
                && request.getResourceID().equals("DUMMY");
    }

    protected void handleRequest(PortletRequest request,
            PortletResponse response) throws PortletException, IOException {
        RequestTimer requestTimer = new RequestTimer();
        requestTimer.start();

        AbstractApplicationPortletWrapper portletWrapper = new AbstractApplicationPortletWrapper(
                this);

        WrappedPortletRequest wrappedRequest = createWrappedRequest(request);

        WrappedPortletResponse wrappedResponse = new WrappedPortletResponse(
                response, getVaadinService());

        CurrentInstance.set(WrappedRequest.class, wrappedRequest);
        CurrentInstance.set(WrappedResponse.class, wrappedResponse);

        RequestType requestType = getRequestType(wrappedRequest);

        if (requestType == RequestType.UNKNOWN) {
            handleUnknownRequest(request, response);
        } else if (requestType == RequestType.DUMMY) {
            /*
             * This dummy page is used by action responses to redirect to, in
             * order to prevent the boot strap code from being rendered into
             * strange places such as iframes.
             */
            ((ResourceResponse) response).setContentType("text/html");
            final OutputStream out = ((ResourceResponse) response)
                    .getPortletOutputStream();
            final PrintWriter outWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(out, "UTF-8")));
            outWriter.print("<html><body>dummy page</body></html>");
            outWriter.close();
        } else if (requestType == RequestType.STATIC_FILE) {
            serveStaticResources((ResourceRequest) request,
                    (ResourceResponse) response);
        } else {
            VaadinSession application = null;
            boolean applicationRunning = false;

            try {
                // TODO What about PARAM_UNLOADBURST & redirectToApplication??

                /* Find out which application this request is related to */
                application = findApplicationInstance(wrappedRequest,
                        requestType);
                if (application == null) {
                    return;
                }
                VaadinSession.setCurrent(application);
                request.setAttribute(VaadinSession.class.getName(), application);

                /*
                 * Get or create an application context and an application
                 * manager for the session
                 */
                VaadinPortletSession applicationContext = (VaadinPortletSession) application;

                PortletCommunicationManager applicationManager = (PortletCommunicationManager) applicationContext
                        .getApplicationManager();

                if (requestType == RequestType.CONNECTOR_RESOURCE) {
                    applicationManager.serveConnectorResource(wrappedRequest,
                            wrappedResponse);
                    return;
                } else if (requestType == RequestType.HEARTBEAT) {
                    applicationManager.handleHeartbeatRequest(wrappedRequest,
                            wrappedResponse, application);
                    return;
                }

                /* Update browser information from request */
                applicationContext.getBrowser().updateRequestDetails(
                        wrappedRequest);

                applicationRunning = true;

                /* Notify listeners */

                // Finds the window within the application
                UI uI = null;
                synchronized (application) {
                    if (application.isRunning()) {
                        switch (requestType) {
                        case RENDER:
                        case ACTION:
                            // Both action requests and render requests are ok
                            // without a UI as they render the initial HTML
                            // and then do a second request
                            uI = application.getUIForRequest(wrappedRequest);
                            break;
                        case BROWSER_DETAILS:
                            // Should not try to find a UI here as the
                            // combined request details might change the UI
                            break;
                        case FILE_UPLOAD:
                            // no window
                            break;
                        case APPLICATION_RESOURCE:
                            // use main window - should not need any window
                            // UI = application.getUI();
                            break;
                        default:
                            uI = application.getUIForRequest(wrappedRequest);
                        }
                        // if window not found, not a problem - use null
                    }
                }

                // TODO Should this happen before or after the transaction
                // starts?
                if (request instanceof RenderRequest) {
                    applicationContext.firePortletRenderRequest(uI,
                            (RenderRequest) request, (RenderResponse) response);
                } else if (request instanceof ActionRequest) {
                    applicationContext.firePortletActionRequest(uI,
                            (ActionRequest) request, (ActionResponse) response);
                } else if (request instanceof EventRequest) {
                    applicationContext.firePortletEventRequest(uI,
                            (EventRequest) request, (EventResponse) response);
                } else if (request instanceof ResourceRequest) {
                    applicationContext.firePortletResourceRequest(uI,
                            (ResourceRequest) request,
                            (ResourceResponse) response);
                }

                /* Handle the request */
                if (requestType == RequestType.FILE_UPLOAD) {
                    // UI is resolved in handleFileUpload by
                    // PortletCommunicationManager
                    applicationManager.handleFileUpload(application,
                            wrappedRequest, wrappedResponse);
                    return;
                } else if (requestType == RequestType.BROWSER_DETAILS) {
                    applicationManager.handleBrowserDetailsRequest(
                            wrappedRequest, wrappedResponse, application);
                    return;
                } else if (requestType == RequestType.UIDL) {
                    // Handles AJAX UIDL requests
                    applicationManager.handleUidlRequest(wrappedRequest,
                            wrappedResponse, portletWrapper, uI);
                    return;
                } else {
                    /*
                     * Removes the application if it has stopped
                     */
                    if (!application.isRunning()) {
                        endApplication(request, response, application);
                        return;
                    }

                    handleOtherRequest(wrappedRequest, wrappedResponse,
                            requestType, application, applicationContext,
                            applicationManager);
                }
            } catch (final SessionExpiredException e) {
                // TODO Figure out a better way to deal with
                // SessionExpiredExceptions
                getLogger().finest("A user session has expired");
            } catch (final GeneralSecurityException e) {
                // TODO Figure out a better way to deal with
                // GeneralSecurityExceptions
                getLogger()
                        .fine("General security exception, the security key was probably incorrect.");
            } catch (final Throwable e) {
                handleServiceException(wrappedRequest, wrappedResponse,
                        application, e);
            } finally {

                if (applicationRunning) {
                    application.closeInactiveUIs();
                }

                CurrentInstance.clearAll();

                if (application != null) {
                    requestTimer.stop(application);
                }
            }
        }
    }

    /**
     * Wraps the request in a (possibly portal specific) wrapped portlet
     * request.
     * 
     * @param request
     *            The original PortletRequest
     * @return A wrapped version of the PorletRequest
     */
    protected WrappedPortletRequest createWrappedRequest(PortletRequest request) {
        String portalInfo = request.getPortalContext().getPortalInfo()
                .toLowerCase();
        if (portalInfo.contains("liferay")) {
            return new WrappedLiferayRequest(request, getVaadinService());
        } else if (portalInfo.contains("gatein")) {
            return new WrappedGateinRequest(request, getVaadinService());
        } else {
            return new WrappedPortletRequest(request, getVaadinService());
        }

    }

    protected PortletService getVaadinService() {
        return vaadinService;
    }

    private void handleUnknownRequest(PortletRequest request,
            PortletResponse response) {
        getLogger().warning("Unknown request type");
    }

    /**
     * Handle a portlet request that is not for static files, UIDL or upload.
     * Also render requests are handled here.
     * 
     * This method is called after starting the application and calling portlet
     * and transaction listeners.
     * 
     * @param request
     * @param response
     * @param requestType
     * @param application
     * @param applicationContext
     * @param applicationManager
     * @throws PortletException
     * @throws IOException
     * @throws MalformedURLException
     */
    private void handleOtherRequest(WrappedPortletRequest request,
            WrappedResponse response, RequestType requestType,
            VaadinSession application, VaadinPortletSession applicationContext,
            PortletCommunicationManager applicationManager)
            throws PortletException, IOException, MalformedURLException {
        if (requestType == RequestType.APPLICATION_RESOURCE
                || requestType == RequestType.RENDER) {
            if (!applicationManager.handleApplicationRequest(request, response)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Not found");
            }
        } else if (requestType == RequestType.EVENT) {
            // nothing to do, listeners do all the work
        } else if (requestType == RequestType.ACTION) {
            // nothing to do, listeners do all the work
        } else {
            throw new IllegalStateException(
                    "handleRequest() without anything to do - should never happen!");
        }
    }

    @Override
    public void processEvent(EventRequest request, EventResponse response)
            throws PortletException, IOException {
        handleRequest(request, response);
    }

    private void serveStaticResources(ResourceRequest request,
            ResourceResponse response) throws IOException, PortletException {
        final String resourceID = request.getResourceID();
        final PortletContext pc = getPortletContext();

        InputStream is = pc.getResourceAsStream(resourceID);
        if (is != null) {
            final String mimetype = pc.getMimeType(resourceID);
            if (mimetype != null) {
                response.setContentType(mimetype);
            }
            final OutputStream os = response.getPortletOutputStream();
            final byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
            int bytes;
            while ((bytes = is.read(buffer)) >= 0) {
                os.write(buffer, 0, bytes);
            }
        } else {
            getLogger().info(
                    "Requested resource [" + resourceID
                            + "] could not be found");
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_NOT_FOUND));
        }
    }

    @Override
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doDispatch(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        try {
            // try to let super handle - it'll call methods annotated for
            // handling, the default doXYZ(), or throw if a handler for the mode
            // is not found
            super.doDispatch(request, response);

        } catch (PortletException e) {
            if (e.getCause() == null) {
                // No cause interpreted as 'unknown mode' - pass that trough
                // so that the application can handle
                handleRequest(request, response);

            } else {
                // Something else failed, pass on
                throw e;
            }
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response)
            throws PortletException, IOException {
        handleRequest(request, response);
    }

    boolean requestCanCreateApplication(PortletRequest request,
            RequestType requestType) {
        if (requestType == RequestType.UIDL && isRepaintAll(request)) {
            return true;
        } else if (requestType == RequestType.RENDER) {
            // In most cases the first request is a render request that renders
            // the HTML fragment. This should create an application instance.
            return true;
        } else if (requestType == RequestType.EVENT) {
            // A portlet can also be sent an event even though it has not been
            // rendered, e.g. portlet on one page sends an event to a portlet on
            // another page and then moves the user to that page.
            return true;
        }
        return false;
    }

    private boolean isRepaintAll(PortletRequest request) {
        return (request.getParameter(URL_PARAMETER_REPAINT_ALL) != null)
                && (request.getParameter(URL_PARAMETER_REPAINT_ALL).equals("1"));
    }

    private void endApplication(PortletRequest request,
            PortletResponse response, VaadinSession application)
            throws IOException {
        application.removeFromSession();
        // Do not send any redirects when running inside a portlet.
    }

    private VaadinSession findApplicationInstance(
            WrappedPortletRequest wrappedRequest, RequestType requestType)
            throws PortletException, SessionExpiredException,
            MalformedURLException {
        PortletRequest request = wrappedRequest.getPortletRequest();

        boolean requestCanCreateApplication = requestCanCreateApplication(
                request, requestType);

        /* Find an existing application for this request. */
        VaadinSession application = getExistingApplication(request,
                requestCanCreateApplication);

        if (application != null) {
            /*
             * There is an existing application. We can use this as long as the
             * user not specifically requested to close or restart it.
             */

            final boolean restartApplication = (wrappedRequest
                    .getParameter(URL_PARAMETER_RESTART_APPLICATION) != null);
            final boolean closeApplication = (wrappedRequest
                    .getParameter(URL_PARAMETER_CLOSE_APPLICATION) != null);

            if (restartApplication) {
                closeApplication(application, request.getPortletSession(false));
                return createAndRegisterApplication(request);
            } else if (closeApplication) {
                closeApplication(application, request.getPortletSession(false));
                return null;
            } else {
                return application;
            }
        }

        // No existing application was found

        if (requestCanCreateApplication) {
            return createAndRegisterApplication(request);
        } else {
            throw new SessionExpiredException();
        }
    }

    private void closeApplication(VaadinSession application,
            PortletSession session) {
        if (application == null) {
            return;
        }

        application.close();
        application.removeFromSession();
    }

    private VaadinSession createAndRegisterApplication(PortletRequest request)
            throws PortletException {
        VaadinSession newApplication = createApplication(request);

        try {
            ServletPortletHelper.checkUiProviders(newApplication);
        } catch (ApplicationClassException e) {
            throw new PortletException(e);
        }

        newApplication.storeInSession(new WrappedPortletSession(request
                .getPortletSession()));

        Locale locale = request.getLocale();
        newApplication.setLocale(locale);
        // No application URL when running inside a portlet
        newApplication.start(new SessionStartEvent(null, getVaadinService()
                .getDeploymentConfiguration(), new PortletCommunicationManager(
                newApplication)));
        addonContext.fireApplicationStarted(newApplication);

        return newApplication;
    }

    protected VaadinPortletSession createApplication(PortletRequest request)
            throws PortletException {
        VaadinPortletSession application = new VaadinPortletSession();

        try {
            ServletPortletHelper.initDefaultUIProvider(application,
                    getVaadinService());
        } catch (ApplicationClassException e) {
            throw new PortletException(e);
        }

        return application;
    }

    private VaadinSession getExistingApplication(PortletRequest request,
            boolean allowSessionCreation) throws MalformedURLException,
            SessionExpiredException {

        final PortletSession session = request
                .getPortletSession(allowSessionCreation);

        if (session == null) {
            throw new SessionExpiredException();
        }

        VaadinSession application = VaadinSession
                .getForSession(new WrappedPortletSession(session));
        if (application == null) {
            return null;
        }
        if (!application.isRunning()) {
            application.removeFromSession();
            return null;
        }

        return application;
    }

    private void handleServiceException(WrappedPortletRequest request,
            WrappedPortletResponse response, VaadinSession application,
            Throwable e) throws IOException, PortletException {
        // TODO Check that this error handler is working when running inside a
        // portlet

        // if this was an UIDL request, response UIDL back to client
        if (getRequestType(request) == RequestType.UIDL) {
            SystemMessages ci = getVaadinService().getSystemMessages();
            criticalNotification(request, response,
                    ci.getInternalErrorCaption(), ci.getInternalErrorMessage(),
                    null, ci.getInternalErrorURL());
            if (application != null) {
                application.getErrorHandler()
                        .terminalError(new RequestError(e));
            } else {
                throw new PortletException(e);
            }
        } else {
            // Re-throw other exceptions
            throw new PortletException(e);
        }

    }

    @SuppressWarnings("serial")
    public class RequestError implements Terminal.ErrorEvent, Serializable {

        private final Throwable throwable;

        public RequestError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public Throwable getThrowable() {
            return throwable;
        }

    }

    /**
     * Send notification to client's application. Used to notify client of
     * critical errors and session expiration due to long inactivity. Server has
     * no knowledge of what application client refers to.
     * 
     * @param request
     *            the Portlet request instance.
     * @param response
     *            the Portlet response to write to.
     * @param caption
     *            for the notification
     * @param message
     *            for the notification
     * @param details
     *            a detail message to show in addition to the passed message.
     *            Currently shown directly but could be hidden behind a details
     *            drop down.
     * @param url
     *            url to load after message, null for current page
     * @throws IOException
     *             if the writing failed due to input/output error.
     */
    void criticalNotification(WrappedPortletRequest request,
            WrappedPortletResponse response, String caption, String message,
            String details, String url) throws IOException {

        // clients JS app is still running, but server application either
        // no longer exists or it might fail to perform reasonably.
        // send a notification to client's application and link how
        // to "restart" application.

        if (caption != null) {
            caption = "\"" + caption + "\"";
        }
        if (details != null) {
            if (message == null) {
                message = details;
            } else {
                message += "<br/><br/>" + details;
            }
        }
        if (message != null) {
            message = "\"" + message + "\"";
        }
        if (url != null) {
            url = "\"" + url + "\"";
        }

        // Set the response type
        response.setContentType("application/json; charset=UTF-8");
        final OutputStream out = response.getOutputStream();
        final PrintWriter outWriter = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(out, "UTF-8")));
        outWriter.print("for(;;);[{\"changes\":[], \"meta\" : {"
                + "\"appError\": {" + "\"caption\":" + caption + ","
                + "\"message\" : " + message + "," + "\"url\" : " + url
                + "}}, \"resources\": {}, \"locales\":[]}]");
        outWriter.close();
    }

    private static final Logger getLogger() {
        return Logger.getLogger(VaadinPortlet.class.getName());
    }

}