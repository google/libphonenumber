package com.google.i18n.phonenumbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombineGeoDataServletTest {

  private CombineGeoDataServlet servlet;
  private Map<String, Object> sessionAttributes;
  private Map<String, Object> requestAttributes;

  @Before
  public void setUp() {
    servlet = new CombineGeoDataServlet();
    sessionAttributes = new HashMap<>();
    requestAttributes = new HashMap<>();
  }

  @Test
  public void testGetOrGenerateCsrfToken_NewSession() {
    HttpServletRequest mockRequest = createMockRequest();
    
    String token = servlet.getOrGenerateCsrfToken(mockRequest);
    
    assertNotNull(token);
    assertEquals(token, sessionAttributes.get("csrf_token"));
  }

  @Test
  public void testGetOrGenerateCsrfToken_ExistingSession() {
    String existingToken = UUID.randomUUID().toString();
    sessionAttributes.put("csrf_token", existingToken);
    HttpServletRequest mockRequest = createMockRequest();
    
    String token = servlet.getOrGenerateCsrfToken(mockRequest);
    
    assertEquals(existingToken, token);
  }

  @Test
  public void testIsTokenValid_NoSessionToken() {
    HttpServletRequest mockRequest = createMockRequest();
    // No token in session
    
    assertFalse(servlet.isTokenValid(mockRequest));
  }

  @Test
  public void testIsTokenValid_ValidToken() {
    String token = UUID.randomUUID().toString();
    sessionAttributes.put("csrf_token", token);
    HttpServletRequest mockRequest = createMockRequest(token);
    
    assertTrue(servlet.isTokenValid(mockRequest));
  }

  private HttpServletRequest createMockRequest() {
    return createMockRequest(null);
  }

  private HttpServletRequest createMockRequest(String requestToken) {
    return (HttpServletRequest) Proxy.newProxyInstance(
        HttpServletRequest.class.getClassLoader(),
        new Class[] { HttpServletRequest.class },
        (proxy, method, args) -> {
          if (method.getName().equals("getSession")) {
            return createMockSession();
          } else if (method.getName().equals("setAttribute")) {
            requestAttributes.put((String) args[0], args[1]);
            return null;
          } else if (method.getName().equals("getAttribute")) {
            return requestAttributes.get(args[0]);
          } else if (method.getName().equals("getParameter")) {
            if ("csrf_token".equals(args[0])) {
              return requestToken;
            }
            return null;
          }
          return null;
        });
  }

  private HttpSession createMockSession() {
    return (HttpSession) Proxy.newProxyInstance(
        HttpSession.class.getClassLoader(),
        new Class[] { HttpSession.class },
        (proxy, method, args) -> {
          if (method.getName().equals("getAttribute")) {
            return sessionAttributes.get(args[0]);
          } else if (method.getName().equals("setAttribute")) {
            sessionAttributes.put((String) args[0], args[1]);
            return null;
          }
          return null;
        });
  }
}
