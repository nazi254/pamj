/*
 * $HeadURL$
 * $Id$
 *
 * Copyright (c) 2006-2011 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
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
 * limitations under the License. |
 */
package org.ambraproject.util;

import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.UserProfile;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.ambraproject.configuration.ConfigurationStore;

import java.io.Serializable;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test class for the URIGenerator
 */
public class URIGeneratorTest {

  public URIGeneratorTest() {
    System.setProperty(ConfigurationStore.SYSTEM_OBJECT_ID_PREFIX, "test:doi/0.0/");
  }

  @DataProvider(name = "objects")
  public Object[][] getObjects() {
    return new Object[][]{
      { new Annotation(null, AnnotationType.REPLY, null), URIGenerator.getPrefix() + "reply/" },
      { new UserProfile(), URIGenerator.getPrefix() + "profile/" }
    };
  }

  @Test(dataProvider = "objects")
  public void testURIs(Object object, String expectedPrefix) {
    Serializable id = URIGenerator.generate(object);
    
    assertNotNull(id, "generated null id");
    assertFalse(id.toString().isEmpty(), "returned empty id");

    assertTrue(id.toString().startsWith(expectedPrefix), "Generated id didn't start with correct prefix; expected: "
      + expectedPrefix + " but found " + id);
  }
}
