/* $HeadURL::                                                                            $
 * $Id$
 *
 * Copyright (c) 2006 by Topaz, Inc.
 * http://topazproject.org
 *
 * Licensed under the Educational Community License version 1.0
 * http://opensource.org/licenses/ecl1.php
 */

package org.plos.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.plos.Constants;
import org.plos.configuration.ConfigurationStore;

import edu.yale.its.tp.cas.client.CASReceipt;

/**
 * A dummy filter for development to manage logging in and out. It simulates both a CAS server and
 * the local CAS filter by intercepting redirects to the CAS server and (in the case of a login
 * request) redirecting to a simple login form instead; on submission it then sets the auth-id and
 * email session attributes appropriately to emulate the CAS filter and redirects back to the
 * desired service URL. For logouts it just redirects to the service URL.
 */
public class DummySSOFilter implements Filter {
  private static final Log log = LogFactory.getLog(DummySSOFilter.class);
  private static final String login  = "login";
  private static final String logout = "logout";

  private String  casUrl;
  private String  ssoUrl;
  private boolean wrap = false;
  private boolean enabled = false;

  /*
   * @see javax.servlet.Filter#init
   */
  public void init(final FilterConfig filterConfig) throws ServletException {
    // get params defined in web.xml
    casUrl = filterConfig.getInitParameter("casUrl");
    ssoUrl = filterConfig.getInitParameter("ssoUrl");
    wrap   = "true".equalsIgnoreCase(filterConfig.getInitParameter("wrapRequest"));

    // look up any overrides in the plos configuration
    Configuration configuration = ConfigurationStore.getInstance().getConfiguration();
    String casBaseUrl = configuration.getString("cas.url.base");
    if (casBaseUrl != null)
      casUrl = casBaseUrl;

    // final defaults
    if (ssoUrl == null)
      ssoUrl = "/dummy/";

    enabled = configuration.getBoolean("dummy.sso.enabled", false);

    if (enabled)
      log.info("dummy sso enabled");
    else
      log.info("dummy sso disabled (use -Ddummy.sso.enabled=true to enable)");
  }

  /*
   * @see javax.servlet.Filter#destroy
   */
  public void destroy() {
  }

  /*
   * @see javax.servlet.Filter#doFilter
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc)
      throws ServletException, IOException {
    if (!enabled) {
     fc.doFilter(request, response);
     return;
    }

    HttpServletRequest  req = (HttpServletRequest)  request;
    HttpServletResponse rsp = (HttpServletResponse) response;

    if (req.getServletPath().startsWith(ssoUrl)) {
      if (req.getMethod().equals("POST"))
        handleLogin(req, rsp);
      else if (req.getServletPath().startsWith(ssoUrl + logout))
        handleLogout(req, rsp);
      else
        sendLoginForm(req, rsp);

      return;
    }

    if (wrap) {
      String authId = (String) req.getSession().getAttribute(Constants.SINGLE_SIGNON_USER_KEY);
      req = wrapRequest(req, authId);
    }

    fc.doFilter(req, new CatchCasRedirResponse(req, rsp));
  }

  private void handleLogin(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String authId = request.getParameter("sso.auth.id");
    String email  = request.getParameter("sso.email");

    if (authId == null) {
      handleLogout(request, response);
      return;
    }

    HttpSession session = ((HttpServletRequest) request).getSession();

    if (log.isDebugEnabled())
      log.debug("logging in as: auth-id='" + authId + "', email='" + email + "', on session " +
                session.getId());

    CASReceipt receipt = new CASReceipt();
    receipt.setPgtIou("foo" + System.currentTimeMillis());

    session.setAttribute(Constants.SINGLE_SIGNON_USER_KEY,  authId);
    session.setAttribute(Constants.SINGLE_SIGNON_RECEIPT,   receipt);
    session.setAttribute(Constants.SINGLE_SIGNON_EMAIL_KEY, email);

    redirToService(request, response);
  }

  private void handleLogout(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    redirToService(request, response);
  }

  private void redirToService(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // get where to redirect back to
    String loc = request.getParameter("service");

    // add any other query paramters to location
    StringBuilder qry = new StringBuilder();
    for (Map.Entry<String, String[]> e :
            ((Map<String, String[]>) request.getParameterMap()).entrySet()) {
      if (!e.getKey().equals("service") && !e.getKey().startsWith("sso.")) {
        for (int idx = 0; idx < e.getValue().length; idx++)
          qry.append(URLEncoder.encode(e.getKey(), "UTF-8")).append("=").
              append(URLEncoder.encode(e.getValue()[idx], "UTF-8")).append("&");
      }
    }

    if (qry.length() > 0) {
      qry.setLength(qry.length() - 1);
      loc += (loc.contains("?") ? "&" : "?") + qry;
    }

    // redirect
    response.sendRedirect(loc);
  }

  private void sendLoginForm(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    byte[] body = createLoginPage(request).getBytes("UTF-8");

    response.setStatus(200);
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
    response.setContentLength(body.length);
    response.getOutputStream().write(body);
  }

  private String createLoginPage(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder(200);
    sb.append("<html>\n").
       append("  <head>\n").
       append("    <title>Dummy-SSO Login</title>\n").
       append("  </head>\n").
       append("  <body>\n").
       append("    <h2>Login</h2>\n").
       append("    <form method=\"POST\" action=\"").append(request.getContextPath()).append(ssoUrl).append(login).append("\">\n").
       append("      <table border=0>\n").
       append("        <tr><td>Username:</td><td><input name=\"sso.auth.id\"/></td></tr>\n").
       append("        <tr><td>Email:   </td><td><input name=\"sso.email\"/></td></tr>\n").
       append("      </table>\n");

    for (Map.Entry<String, String[]> e :
            ((Map<String, String[]>) request.getParameterMap()).entrySet()) {
      for (int idx = 0; idx < e.getValue().length; idx++)
        sb.append("      <input type=\"hidden\" name=\"").append(e.getKey()).append("\" value=\"").append(e.getValue()[idx]).append("\"/>\n");
    }

    sb.append("      <input type=\"submit\" value=\"Log in\" name=\"sso.login\"/>\n").
       append("    </form>\n").
       append("    <p>Notes:</p>\n").
       append("    <ul>\n").
       append("      <li>creates a new account if username doesn't exist</li>\n").
       append("      <li>\"username\" is actually internal account-id (auth-id)</li>\n").
       append("      <li>leave \"username\" empty to logout and become anonymous user</li>\n").
       append("      <li>if email is different than the currently stored one, the profile will be updated</li>\n").
       append("    </ul>\n").
       append("  </body>\n").
       append("</html>\n");

    return sb.toString();
  }

  protected HttpServletRequest wrapRequest(HttpServletRequest request, final String user) {
    final Principal principal = (user == null) ? null : new Principal() {
        public String getName() {
          return user;
        }
      };

    return new HttpServletRequestWrapper(request) {
        public String getRemoteUser() {
          return user;
        }

        public Principal getUserPrincipal() {
          return principal;
        }
      };
  }

  private class CatchCasRedirResponse extends HttpServletResponseWrapper {
    private final HttpServletRequest req;

    public CatchCasRedirResponse(HttpServletRequest req, HttpServletResponse resp) {
      super(resp);
      this.req = req;
    }

    public void sendRedirect(String location) throws IOException {
      if (location.startsWith(casUrl)) {
        String cmd =
            location.substring(casUrl.length()).matches("(?i)[^?]*login.*") ? login : logout;
        int q = location.indexOf("?");
        String query = (q >= 0) ? location.substring(q) : "";
        super.sendRedirect(req.getContextPath() + ssoUrl + cmd + query);
      } else
        super.sendRedirect(location);
    }
  }
}
