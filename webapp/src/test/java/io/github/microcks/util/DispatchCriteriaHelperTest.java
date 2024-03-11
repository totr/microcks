/*
 * Copyright The Microcks Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.microcks.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * This is a test case for DispatchCriteriaHelper class.
 * @author laurent
 */
public class DispatchCriteriaHelperTest {

   @Test
   public void testExtractParamsFromURI() {
      // Check with parameters.
      String requestPath = "/v2/pet/findByStatus?user_key=998bac0775b1d5f588e0a6ca7c11b852&status=available";

      // Dispatch string params are sorted.
      String dispatchCriteria = DispatchCriteriaHelper.extractParamsFromURI(requestPath);
      assertEquals("user_key && status", dispatchCriteria);
   }

   @Test
   public void testExtractPartsFromURIs() {
      // Prepare a bunch of uri paths.
      List<String> resourcePaths = new ArrayList<>();
      resourcePaths.add("/v2/pet/findByDate/2017");
      resourcePaths.add("/v2/pet/findByDate/2016/12");
      resourcePaths.add("/v2/pet/findByDate/2016/12/20");

      // Dispatch parts in natural order.
      // Should be deduced to something like "/v2/pet/findByDate/{part1}/{part2}/{part3}"
      String dispatchCriteria = DispatchCriteriaHelper.extractPartsFromURIs(resourcePaths);
      assertEquals("part1 && part2 && part3", dispatchCriteria);

      // 2nd case: variable part is not terminating.
      resourcePaths = new ArrayList<>();
      resourcePaths.add("/v2/pet/1/tattoos");
      resourcePaths.add("/v2/pet/23/tattoos");

      // Should be deduced to something like "/v2/pet/{part1}/tattoos"
      dispatchCriteria = DispatchCriteriaHelper.extractPartsFromURIs(resourcePaths);
      assertEquals("part1", dispatchCriteria);
   }

   @Test
   public void testExtractPartsFromURIPattern() {
      // Check with single URI pattern.
      String operationName = "/deployment/byComponent/{component}/{version}?{{param}}";

      String dispatchCriteria = DispatchCriteriaHelper.extractPartsFromURIPattern(operationName);
      assertEquals("component && version", dispatchCriteria);

      // Check with swagger/postman syntax.
      operationName = "/deployment/byComponent/:component/:version?{{param}}";

      dispatchCriteria = DispatchCriteriaHelper.extractPartsFromURIPattern(operationName);
      assertEquals("component && version", dispatchCriteria);

      // Check with extra path containing special character.
      operationName = "/deployment/byComponent/{component}/{version}/$count";

      dispatchCriteria = DispatchCriteriaHelper.extractPartsFromURIPattern(operationName);
      assertEquals("component && version", dispatchCriteria);
   }

   @ParameterizedTest
   @CsvSource({
         // Check with parts sorted in natural order.
         "/deployment/byComponent/myComp/1.2, /deployment/byComponent/{component}/{version}",
         // Check with parts expressed using swagger/postman syntax.
         "/deployment/byComponent/myComp/1.2, /deployment/byComponent/:component/:version",
         // Check with parts expressed using swagger/postman syntax.
         "/deployment/byComponent/myComp/1.2/$count, /deployment/byComponent/:component/:version/$count", })
   public void testExtractFromURIPattern(String requestPath, String operationName) {

      final String dispatcherRule = "component && version";
      final String expectedCriteria = "/component=myComp/version=1.2";

      // Dispatch string parts are sorted.
      String dispatchCriteria = DispatchCriteriaHelper.extractFromURIPattern(dispatcherRule, operationName,
            requestPath);
      assertEquals(expectedCriteria, dispatchCriteria);
   }

   @Test
   public void testExtractFromURIPattern2() {
      String resourcePath = "/pet/2";
      String operationName = "/pet/:petId";
      String paramRule = "petId";

      String dispatchCriteria = DispatchCriteriaHelper.extractFromURIPattern(paramRule, operationName, resourcePath);
      assertEquals("/petId=2", dispatchCriteria);

      resourcePath = "/order/123456";
      operationName = "/order/:id";
      paramRule = "id";

      dispatchCriteria = DispatchCriteriaHelper.extractFromURIPattern(paramRule, operationName, resourcePath);
      assertEquals("/id=123456", dispatchCriteria);
   }

   @Test
   public void testExtractFromURIPatternUnsorted() {
      // Check with parts not sorted in natural order.
      String requestPath = "/deployment/byComponent/1.2/myComp";
      String operationName = "/deployment/byComponent/{version}/{component}";
      String paramRule = "version && component";

      // Dispatch string parts are sorted.
      String dispatchCriteria = DispatchCriteriaHelper.extractFromURIPattern(paramRule, operationName, requestPath);
      assertEquals("/component=myComp/version=1.2", dispatchCriteria);
   }

   @Test
   public void testExtractFromURIPatternWithExtension() {
      // Check with parts sorted in natural order.
      String requestPath = "/deployment/byComponent/myComp/1.2.json";
      String operationName = "/deployment/byComponent/{component}/{version}.json";
      String paramRule = "component && version";

      // Dispatch string parts are sorted.
      String dispatchCriteria = DispatchCriteriaHelper.extractFromURIPattern(paramRule, operationName, requestPath);
      assertEquals("/component=myComp/version=1.2", dispatchCriteria);
   }

   @ParameterizedTest
   @CsvSource({ "component, /component=myComp", "version, /version=1.2",
         "component && version, /component=myComp/version=1.2", "component ?? apiKey, /component=myComp" })
   public void testExtractFromURIPatternWithCustomRule(String paramRule, String expectedCriteria) {
      // Check with parts sorted in natural order.
      String requestPath = "/deployment/byComponent/myComp/1.2";
      String operationName = "/deployment/byComponent/{component}/{version}";

      // Dispatch string parts are sorted.
      String dispatchCriteria = DispatchCriteriaHelper.extractFromURIPattern(paramRule, operationName, requestPath);
      assertEquals(expectedCriteria, dispatchCriteria);
   }

   @Test
   public void testExtractFromURIParams() {
      // Check with parameters in no particular order.
      String requestPath = "/v2/pet/findByDate/2017/01/04?user_key=998bac0775b1d5f588e0a6ca7c11b852&status=available";

      // Only 1 parameter should be taken into account according to rules.
      String dispatchCriteria = DispatchCriteriaHelper.extractFromURIParams("user_key", requestPath);
      assertEquals("?user_key=998bac0775b1d5f588e0a6ca7c11b852", dispatchCriteria);

      // 2 parameters should be considered and sorted according to rules.
      dispatchCriteria = DispatchCriteriaHelper.extractFromURIParams("user_key && status", requestPath);
      assertEquals("?status=available?user_key=998bac0775b1d5f588e0a6ca7c11b852", dispatchCriteria);
   }

   @Test
   public void testBuildFromPartsMap() {
      Multimap<String, String> partsMap = ArrayListMultimap.create();
      partsMap.put("year", "2018");
      partsMap.put("month", "05");
      partsMap.put("year-summary", "true");
      partsMap.put("half-year", "true");

      // Dispatch string parts are sorted.
      String dispatchCriteria = DispatchCriteriaHelper.buildFromPartsMap("month && year", partsMap);
      assertEquals("/month=05/year=2018", dispatchCriteria);

      // Only 1 parameter should be taken into account according to rules.
      dispatchCriteria = DispatchCriteriaHelper.buildFromPartsMap("year", partsMap);
      assertEquals("/year=2018", dispatchCriteria);

      // 2 parameters should be taken into account according to rules with no inclusion of year.
      dispatchCriteria = DispatchCriteriaHelper.buildFromPartsMap("month && year-summary", partsMap);
      assertEquals("/month=05/year-summary=true", dispatchCriteria);
   }

   @Test
   public void testBuildFromParamsMap() {
      Multimap<String, String> paramsMap = ArrayListMultimap.create();
      paramsMap.put("page", "1");
      paramsMap.put("limit", "20");
      paramsMap.put("limitation", "20");
      paramsMap.put("status", "available");

      // Only 1 parameter should be taken into account according to rules.
      String dispatchCriteria = DispatchCriteriaHelper.buildFromParamsMap("page", paramsMap);
      assertEquals("?page=1", dispatchCriteria);

      // 2 parameters should be considered and sorted according to rules.
      dispatchCriteria = DispatchCriteriaHelper.buildFromParamsMap("page && limit", paramsMap);
      assertEquals("?limit=20?page=1", dispatchCriteria);

      // 2 parameters should be considered and sorted according to rules with no inclusion of limit.
      dispatchCriteria = DispatchCriteriaHelper.buildFromParamsMap("page && limitation", paramsMap);
      assertEquals("?limitation=20?page=1", dispatchCriteria);
   }

   @Test
   public void testBuildFromParamsArrayMap() {
      Multimap<String, String> paramsMap = ArrayListMultimap.create();
      paramsMap.put("page", "1");
      paramsMap.put("limit", "20");
      paramsMap.put("limitation", "20");
      paramsMap.put("status", "available");
      paramsMap.put("status", "busy");

      // 2 parameters should be taken into account, one with two values according to rules.
      String dispatchCriteria = DispatchCriteriaHelper.buildFromParamsMap("page && status", paramsMap);
      assertEquals("?page=1?status=available?status=busy", dispatchCriteria);

      // 1 parameter with two values should be taken into account according to rules
      dispatchCriteria = DispatchCriteriaHelper.buildFromParamsMap("status", paramsMap);
      assertEquals("?status=available?status=busy", dispatchCriteria);
   }

   @Test
   public void extractCommonSuffix() {
      final var uris = List.of("/ab/def", "/cde/def");
      final var result = DispatchCriteriaHelper.extractCommonSuffix(uris);
      assertEquals("/def", result);
   }
}
