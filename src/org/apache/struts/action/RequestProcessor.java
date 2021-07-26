package org.apache.struts.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ExceptionHandler;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;

public class RequestProcessor {
  public static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
  
  public static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
  
  protected HashMap actions = new HashMap();
  
  protected ModuleConfig appConfig = null;
  
  protected ModuleConfig moduleConfig = null;
  
  protected static Log log = LogFactory.getLog(RequestProcessor.class);
  
  protected ActionServlet servlet = null;
  
  public void destroy() {
    synchronized (this.actions) {
      Iterator actions = this.actions.values().iterator();
      while (actions.hasNext()) {
        Action action = (Action) actions.next();
        action.setServlet(null);
      } 
      this.actions.clear();
    } 
    this.servlet = null;
  }
  
  public void init(ActionServlet servlet, ModuleConfig moduleConfig) throws ServletException {
    synchronized (this.actions) {
      this.actions.clear();
    } 
    this.servlet = servlet;
    this.appConfig = moduleConfig;
    this.moduleConfig = moduleConfig;
  }
  
  public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    request = processMultipart(request);
    String path = processPath(request, response);
    if (path == null)
      return; 
    if (log.isDebugEnabled())
      log.debug("Processing a '" + request.getMethod() + "' for path '" + path + "'"); 
    processLocale(request, response);
    processContent(request, response);
    processNoCache(request, response);
    if (!processPreprocess(request, response))
      return; 
    ActionMapping mapping = processMapping(request, response, path);
    if (mapping == null)
      return; 
    if (!processRoles(request, response, mapping))
      return; 
    ActionForm form = processActionForm(request, response, mapping);
    processPopulate(request, response, form, mapping);
    if (!processValidate(request, response, form, mapping))
      return; 
    if (!processForward(request, response, mapping))
      return; 
    if (!processInclude(request, response, mapping))
      return; 
    Action action = processActionCreate(request, response, mapping);
    if (action == null)
      return; 
    ActionForward forward = processActionPerform(request, response, action, form, mapping);
    processForwardConfig(request, response, (ForwardConfig)forward);
  }
  
  protected Action processActionCreate(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping) throws IOException {
    String className = mapping.getType();
    if (log.isDebugEnabled())
      log.debug(" Looking for Action instance for class " + className); 
    Action instance = null;
    synchronized (this.actions) {
      instance = (Action)this.actions.get(className);
      if (instance != null) {
        if (log.isTraceEnabled())
          log.trace("  Returning existing Action instance"); 
        return instance;
      } 
      if (log.isTraceEnabled())
        log.trace("  Creating new Action instance"); 
      try {
        instance = (Action)RequestUtils.applicationInstance(className);
      } catch (Exception e) {
        log.error(getInternal().getMessage("actionCreate", mapping.getPath()), e);
        response.sendError(500, getInternal().getMessage("actionCreate", mapping.getPath()));
        return null;
      } 
      instance.setServlet(this.servlet);
      this.actions.put(className, instance);
    } 
    return instance;
  }
  
  protected ActionForm processActionForm(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping) {
    ActionForm instance = RequestUtils.createActionForm(request, mapping, this.moduleConfig, this.servlet);
    if (instance == null)
      return null; 
    if (log.isDebugEnabled())
      log.debug(" Storing ActionForm bean instance in scope '" + mapping.getScope() + "' under attribute key '" + mapping.getAttribute() + "'"); 
    if ("request".equals(mapping.getScope())) {
      request.setAttribute(mapping.getAttribute(), instance);
    } else {
      HttpSession session = request.getSession();
      session.setAttribute(mapping.getAttribute(), instance);
    } 
    return instance;
  }
  
  protected void processActionForward(HttpServletRequest request, HttpServletResponse response, ActionForward forward) throws IOException, ServletException {
    processForwardConfig(request, response, (ForwardConfig)forward);
  }
  
  protected void processForwardConfig(HttpServletRequest request, HttpServletResponse response, ForwardConfig forward) throws IOException, ServletException {
    if (forward == null)
      return; 
    if (log.isDebugEnabled())
      log.debug("processForwardConfig(" + forward + ")"); 
    String forwardPath = forward.getPath();
    String uri = null;
    if (forwardPath.startsWith("/")) {
      uri = RequestUtils.forwardURL(request, forward);
    } else {
      uri = forwardPath;
    } 
    if (forward.getRedirect()) {
System.out.println("!!!!!!!!!!!!!!!!!!!!!!!");
      if (uri.startsWith("/")) {
System.out.println("URI: " + uri);
        uri = request.getContextPath() + uri;
System.out.println("CP: " + request.getContextPath());
System.out.println("Concatenated URI: " + uri);
      }
      
//uri = "http://localhost:8080/struts/success.jsp";
uri = "https://www.google.com";
System.out.println("HARDCODED URI: " + uri);
System.out.println("Redirecting...");
      response.sendRedirect(response.encodeRedirectURL(uri));
    } else {
      doForward(uri, request, response);
    } 
  }
  
  protected ActionForward processActionPerform(HttpServletRequest request, HttpServletResponse response, Action action, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
    try {
      return action.execute(mapping, form, request, response);
    } catch (Exception e) {
      return processException(request, response, e, form, mapping);
    } 
  }
  
  protected void processContent(HttpServletRequest request, HttpServletResponse response) {
    String contentType = this.moduleConfig.getControllerConfig().getContentType();
    if (contentType != null)
      response.setContentType(contentType); 
  }
  
  protected ActionForward processException(HttpServletRequest request, HttpServletResponse response, Exception exception, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
    ExceptionConfig config = mapping.findException(exception.getClass());
    if (config == null) {
      log.warn(getInternal().getMessage("unhandledException", exception.getClass()));
      if (exception instanceof IOException)
        throw (IOException)exception; 
      if (exception instanceof ServletException)
        throw (ServletException)exception; 
      throw new ServletException(exception);
    } 
    try {
      ExceptionHandler handler = (ExceptionHandler)RequestUtils.applicationInstance(config.getHandler());
      return handler.execute(exception, config, mapping, form, request, response);
    } catch (Exception e) {
      throw new ServletException(e);
    } 
  }
  
  protected boolean processForward(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping) throws IOException, ServletException {
    String forward = mapping.getForward();
    if (forward == null)
      return true; 
    internalModuleRelativeForward(forward, request, response);
    return false;
  }
  
  protected boolean processInclude(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping) throws IOException, ServletException {
    String include = mapping.getInclude();
    if (include == null)
      return true; 
    internalModuleRelativeInclude(include, request, response);
    return false;
  }
  
  protected void processLocale(HttpServletRequest request, HttpServletResponse response) {
    if (!this.moduleConfig.getControllerConfig().getLocale())
      return; 
    HttpSession session = request.getSession();
    if (session.getAttribute("org.apache.struts.action.LOCALE") != null)
      return; 
    Locale locale = request.getLocale();
    if (locale != null) {
      if (log.isDebugEnabled())
        log.debug(" Setting user locale '" + locale + "'"); 
      session.setAttribute("org.apache.struts.action.LOCALE", locale);
    } 
  }
  
  protected ActionMapping processMapping(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    ActionMapping mapping = (ActionMapping)this.moduleConfig.findActionConfig(path);
    if (mapping != null) {
      request.setAttribute("org.apache.struts.action.mapping.instance", mapping);
      return mapping;
    } 
    ActionConfig[] configs = this.moduleConfig.findActionConfigs();
    for (int i = 0; i < configs.length; i++) {
      if (configs[i].getUnknown()) {
        mapping = (ActionMapping)configs[i];
        request.setAttribute("org.apache.struts.action.mapping.instance", mapping);
        return mapping;
      } 
    } 
    log.error(getInternal().getMessage("processInvalid", path));
    response.sendError(400, getInternal().getMessage("processInvalid", path));
    return null;
  }
  
  protected HttpServletRequest processMultipart(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod()))
      return request; 
    String contentType = request.getContentType();
    if (contentType != null && contentType.startsWith("multipart/form-data"))
      return (HttpServletRequest)new MultipartRequestWrapper(request); 
    return request;
  }
  
  protected void processNoCache(HttpServletRequest request, HttpServletResponse response) {
    if (this.moduleConfig.getControllerConfig().getNocache()) {
      response.setHeader("Pragma", "No-cache");
      response.setHeader("Cache-Control", "no-cache");
      response.setDateHeader("Expires", 1L);
    } 
  }
  
  protected String processPath(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String path = null;
    path = (String)request.getAttribute("javax.servlet.include.path_info");
    if (path == null)
      path = request.getPathInfo(); 
    if (path != null && path.length() > 0)
      return path; 
    path = (String)request.getAttribute("javax.servlet.include.servlet_path");
    if (path == null)
      path = request.getServletPath(); 
    String prefix = this.moduleConfig.getPrefix();
    if (!path.startsWith(prefix)) {
      log.error(getInternal().getMessage("processPath", request.getRequestURI()));
      response.sendError(400, getInternal().getMessage("processPath", request.getRequestURI()));
      return null;
    } 
    path = path.substring(prefix.length());
    int slash = path.lastIndexOf("/");
    int period = path.lastIndexOf(".");
    if (period >= 0 && period > slash)
      path = path.substring(0, period); 
    return path;
  }
  
  protected void processPopulate(HttpServletRequest request, HttpServletResponse response, ActionForm form, ActionMapping mapping) throws ServletException {
    if (form == null)
      return; 
    if (log.isDebugEnabled())
      log.debug(" Populating bean properties from this request"); 
    form.setServlet(this.servlet);
    form.reset(mapping, request);
    if (mapping.getMultipartClass() != null)
      request.setAttribute("org.apache.struts.action.mapping.multipartclass", mapping.getMultipartClass()); 
    RequestUtils.populate(form, mapping.getPrefix(), mapping.getSuffix(), request);
    if (request.getParameter("org.apache.struts.taglib.html.CANCEL") != null || request.getParameter("org.apache.struts.taglib.html.CANCEL.x") != null)
      request.setAttribute("org.apache.struts.action.CANCEL", Boolean.TRUE); 
  }
  
  protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
    return true;
  }
  
  protected boolean processRoles(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping) throws IOException, ServletException {
    String[] roles = mapping.getRoleNames();
    if (roles == null || roles.length < 1)
      return true; 
    for (int i = 0; i < roles.length; i++) {
      if (request.isUserInRole(roles[i])) {
        if (log.isDebugEnabled())
          log.debug(" User '" + request.getRemoteUser() + "' has role '" + roles[i] + "', granting access"); 
        return true;
      } 
    } 
    if (log.isDebugEnabled())
      log.debug(" User '" + request.getRemoteUser() + "' does not have any required role, denying access"); 
    response.sendError(400, getInternal().getMessage("notAuthorized", mapping.getPath()));
    return false;
  }
  
  protected boolean processValidate(HttpServletRequest request, HttpServletResponse response, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
    if (form == null)
      return true; 
    if (request.getAttribute("org.apache.struts.action.CANCEL") != null) {
      if (log.isDebugEnabled())
        log.debug(" Cancelled transaction, skipping validation"); 
      return true;
    } 
    if (!mapping.getValidate())
      return true; 
    if (log.isDebugEnabled())
      log.debug(" Validating input form properties"); 
    ActionErrors errors = form.validate(mapping, request);
    if (errors == null || errors.isEmpty()) {
      if (log.isTraceEnabled())
        log.trace("  No errors detected, accepting input"); 
      return true;
    } 
    if (form.getMultipartRequestHandler() != null) {
      if (log.isTraceEnabled())
        log.trace("  Rolling back multipart request"); 
      form.getMultipartRequestHandler().rollback();
    } 
    String input = mapping.getInput();
    if (input == null) {
      if (log.isTraceEnabled())
        log.trace("  Validation failed but no input form available"); 
      response.sendError(500, getInternal().getMessage("noInput", mapping.getPath()));
      return false;
    } 
    if (log.isDebugEnabled())
      log.debug(" Validation failed, returning to '" + input + "'"); 
    request.setAttribute("org.apache.struts.action.ERROR", errors);
    if (this.moduleConfig.getControllerConfig().getInputForward()) {
      ActionForward actionForward = mapping.findForward(input);
      processForwardConfig(request, response, (ForwardConfig)actionForward);
    } else {
      internalModuleRelativeForward(input, request, response);
    } 
    return false;
  }
  
  protected void internalModuleRelativeForward(String uri, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    uri = this.moduleConfig.getPrefix() + uri;
    if (log.isDebugEnabled())
      log.debug(" Delegating via forward to '" + uri + "'"); 
    doForward(uri, request, response);
  }
  
  protected void internalModuleRelativeInclude(String uri, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    uri = this.moduleConfig.getPrefix() + uri;
    if (log.isDebugEnabled())
      log.debug(" Delegating via include to '" + uri + "'"); 
    doInclude(uri, request, response);
  }
  
  protected void doForward(String uri, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    if (request instanceof MultipartRequestWrapper)
      request = ((MultipartRequestWrapper)request).getRequest(); 
    RequestDispatcher rd = getServletContext().getRequestDispatcher(uri);
    if (rd == null) {
      response.sendError(500, getInternal().getMessage("requestDispatcher", uri));
      return;
    } 
    rd.forward((ServletRequest)request, (ServletResponse)response);
  }
  
  protected void doInclude(String uri, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    if (request instanceof MultipartRequestWrapper)
      request = ((MultipartRequestWrapper)request).getRequest(); 
    RequestDispatcher rd = getServletContext().getRequestDispatcher(uri);
    if (rd == null) {
      response.sendError(500, getInternal().getMessage("requestDispatcher", uri));
      return;
    } 
    rd.include((ServletRequest)request, (ServletResponse)response);
  }
  
  public int getDebug() {
    return this.servlet.getDebug();
  }
  
  protected MessageResources getInternal() {
    return this.servlet.getInternal();
  }
  
  protected ServletContext getServletContext() {
    return this.servlet.getServletContext();
  }
  
  protected void log(String message) {
    this.servlet.log(message);
  }
  
  protected void log(String message, Throwable exception) {
    this.servlet.log(message, exception);
  }
}
