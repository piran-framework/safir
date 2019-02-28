/*
 *  Copyright (c) 2018 Isa Hekmatizadeh.
 *
 *  This file is part of Safir.
 *
 *  Safir is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Safir is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Safir.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.piranframework.darbaan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Isa Hekmatizadeh
 */
public class MappingTest {
  private ObjectMapper mapper = new ObjectMapper();

  @Before
  public void init(){
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
  }

  @Test
  public void transformingTest() throws IOException {
    Map<String,Object> payload = new HashMap<>();
    payload.put("testField1","Hasan");
    payload.put("testField2","Hasan2");
    payload.put("testField3",3);
    byte[] bytes = mapper.writeValueAsBytes(payload);
    TestModel testModel = mapper.readValue(bytes, TestModel.class);
    Assert.assertEquals(testModel.testField1,"Hasan");
    Assert.assertEquals(testModel.testField2,"Hasan2");
    Assert.assertEquals(testModel.testField3,3);
  }

  public static class TestModel {
    private String testField1;
    private String testField2;
    private int testField3;

    public String getTestField1() {
      return testField1;
    }

    public TestModel setTestField1(String testField1) {
      this.testField1 = testField1;
      return this;
    }

    public String getTestField2() {
      return testField2;
    }

    public TestModel setTestField2(String testField2) {
      this.testField2 = testField2;
      return this;
    }

    public int getTestField3() {
      return testField3;
    }

    public TestModel setTestField3(int testField3) {
      this.testField3 = testField3;
      return this;
    }
  }
}
