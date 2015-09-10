/*
 * Copyright (c) 2015 TextGlass
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.textglass;

import org.textglass.json.JsonFile;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;

public class ReferenceTest {

  private JsonFile loadJson(String path) throws Exception {
    InputStream in = ReferenceTest.class.getResourceAsStream(path);

    if (in == null) {
        throw new FileNotFoundException("Jar resource not found: " + path);
    }

    return new JsonFile(in);
  }

  @Test
  public void doReferenceATest() throws Exception {
    String p = "/textglass/reference/domains/a/patterns.json";
    String a = "/textglass/reference/domains/a/attributes.json";
    String t = "/textglass/reference/domains/a/test.json";

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(p), null, loadJson(a), null);

    test(client, loadJson(t));
  }

  @Test
  public void doReferenceBTest() throws Exception {
    String p = "/textglass/reference/domains/b/patterns.json";
    String a = "/textglass/reference/domains/b/attributes.json";
    String t = "/textglass/reference/domains/b/test.json";

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(p), null, loadJson(a), null);

    test(client, loadJson(t));
  }

  @Test
  public void doReferenceCTest() throws Exception {
    String p = "/textglass/reference/domains/c/patterns.json";
    String t = "/textglass/reference/domains/c/test.json";

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(p), null, null, null);

    test(client, loadJson(t));
  }

  @Test
  public void doReferenceDTest() throws Exception {
    String p = "/textglass/reference/domains/d/patterns.json";
    String t = "/textglass/reference/domains/d/test.json";

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(p), null, null, null);

    test(client, loadJson(t));
  }

  @Test
  public void doReferenceETest() throws Exception {
    String p = "/textglass/reference/domains/e/patterns.json";
    String pp = "/textglass/reference/domains/e/patterns_patch.json";
    String a = "/textglass/reference/domains/e/attributes.json";
    String ap = "/textglass/reference/domains/e/attributes_patch.json";
    String t = "/textglass/reference/domains/e/test.json";

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(p), loadJson(pp), loadJson(a), loadJson(ap));

    test(client, loadJson(t));
  }

  @Test
  public void doReferenceFTest() throws Exception {
    String p = "/textglass/reference/domains/f/patterns.json";
    String a = "/textglass/reference/domains/f/attributes.json";
    String t = "/textglass/reference/domains/f/test.json";

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(p), null, loadJson(a), null);

    test(client, loadJson(t));
  }

  private boolean test(TextGlassClient client, JsonFile tests) throws Exception {

    //PARSE TEST FILE JSON

    if(!tests.getType().equals("test")) {
      throw new Exception("Unknown test type: " + tests.getType());
    }

    if(!tests.getDomain().equals(client.getDomain())) {
      throw new Exception("Domains do not match: " + client.getDomain() + " != " + tests.getDomain());
    }

    if(!tests.getDomainVersion().equals(client.getDomainVersion())) {
      throw new Exception("DomainVersions do not match: " + client.getDomainVersion() + " != " + tests.getDomainVersion());
    }

    Util.log("Loading test: " + tests.getDomain() + ", version: " + tests.getDomainVersion(), 1);

    int testCount = 0;
    int passCount = 0;

    long start, time;

    //ITERATE THRU THE TESTS

    start = System.nanoTime();
    
    if(JsonFile.get(tests.getJsonNode(), "tests").isArray()) {
        for(int i = 0; i < tests.getJsonNode().get("tests").size(); i++) {
          JsonNode test = tests.getJsonNode().get("tests").get(i);

          if(JsonFile.get(test, "input").isNull()) {
            throw new Exception("Bad test input found, position: " + testCount);
          }

          String input = test.get("input").asText();
          String resultPatternId = null;

          if(!JsonFile.get(test, "resultPatternId").isNull()) {
            resultPatternId = JsonFile.get(test, "resultPatternId").asText();
          }

          //PERFORM CLASSIFICATION

          Map<String, String> result = client.classify(input);
          String patternId = null;

          if(result != null) {
            patternId = result.get("patternId");
          }

          //CHECK IF IT PASSES

          boolean pass = false;
          int failedAttributes = 0;

          if(resultPatternId == null) {
            if(patternId == null) {
              pass = true;
            }
          } else if(resultPatternId.equals(patternId)) {
            pass = true;
          }

          if(pass && JsonFile.get(test, "resultAttributes").isObject()) {
            for(Iterator<String> j = test.get("resultAttributes").getFieldNames(); j.hasNext();) {
              String key = j.next();
              String expectedValue = test.get("resultAttributes").get(key).asText();
              String value = null;

              if(result != null) {
                value = result.get(key);
              }

              if(!expectedValue.equals(value)) {
                Util.log("FAILED, expected attribute for " + key + ": " + expectedValue + ", found: " + value, 2);
                failedAttributes++;
              }
            }
          }

          //PRINT RESULTS

          if(pass && failedAttributes == 0) {
            passCount++;
            Util.log("Passed, expected patternId: " + resultPatternId, 2);
          } else if(!pass) {
            Util.log("FAILED, expected patternId: " + resultPatternId + ", found: " + patternId, 2);
          }

          testCount++;
        }
      }

    time = System.nanoTime() - start;

    Util.log("Test passed " + passCount + " out of " + testCount + ". " + (testCount == passCount ? "PASS" : "FAIL"), 0);
    Util.log("Test time: " + getTime(time), 0);

    return testCount != passCount;
  }

  public static String getTime(long ns)
  {
      return ns / (1000 * 1000 * 1000) + "s " +
          ns / (1000 * 1000) % 1000 + "ms " +
          ns / 1000 % 1000 + "." + ns % 1000 + "us";
  }
}
