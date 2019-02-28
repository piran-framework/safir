package com.piranframework.safir.test;

import com.piranframework.safir.api.Action;
import com.piranframework.safir.api.ActionCategory;

import java.util.List;

/**
 * @author Isa Hekmatizadeh
 */
@ActionCategory("testCat")
public class TestCat {
  @Action("testAct")
  public void hello() {
  }
}
