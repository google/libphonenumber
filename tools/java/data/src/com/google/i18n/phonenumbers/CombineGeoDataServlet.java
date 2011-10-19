/*
 * Copyright (C) 2011 The Libphonenumber Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Philippe Liard
 */

package com.google.i18n.phonenumbers;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that invokes the geocoding data combination tool.
 */
public class CombineGeoDataServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/html;charset=UTF-8");
    String input = req.getParameter("geodata");
    resp.getOutputStream().print("<html><head>");
    resp.getOutputStream().print(
        "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head>");
    resp.getOutputStream().print("<body>");
    CombineGeoData combineGeoData = new CombineGeoData(
        new ByteArrayInputStream(input.getBytes()), resp.getOutputStream(), "<br>");
    combineGeoData.run();
    resp.getOutputStream().print("</body></html>");
    resp.getOutputStream().flush();
  }
}
