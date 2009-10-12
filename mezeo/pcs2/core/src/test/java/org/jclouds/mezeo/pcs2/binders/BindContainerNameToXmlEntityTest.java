/**
 *
 * Copyright (C) 2009 Global Cloud Specialists, Inc. <info@globalcloudspecialists.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 */
package org.jclouds.mezeo.pcs2.binders;

import static org.testng.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;

import org.jclouds.http.HttpRequest;
import org.testng.annotations.Test;

/**
 * Tests behavior of {@code BindContainerNameToXmlEntity}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "pcs2.BindContainerNameToXmlEntityTest")
public class BindContainerNameToXmlEntityTest {

   public void test() {
      BindContainerNameToXmlEntity binder = new BindContainerNameToXmlEntity();
      HttpRequest request = new HttpRequest("GET", URI.create("http://localhost"));
      binder.bindToRequest(request, "foo");
      assertEquals(request.getEntity(), "<container><name>foo</name></container>");
      assertEquals(request.getFirstHeaderOrNull(HttpHeaders.CONTENT_LENGTH),
               "<container><name>foo</name></container>".getBytes().length + "");
      assertEquals(request.getFirstHeaderOrNull(HttpHeaders.CONTENT_TYPE),
               "application/vnd.csp.container-info+xml");

   }
}