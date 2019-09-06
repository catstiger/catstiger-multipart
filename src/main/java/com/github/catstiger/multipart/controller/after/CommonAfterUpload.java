package com.github.catstiger.multipart.controller.after;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.common.web.controller.BaseController;
import com.github.catstiger.multipart.model.FileObject;
import com.google.common.base.Preconditions;

public class CommonAfterUpload implements AfterUpload {

  @Override
  public void error(HttpServletResponse response, FileObject fileObject, String errorMessage) {
    Map<String, Object> data = new HashMap<>();
    data.put("error", 1);
    data.put("message", errorMessage);
    
    BaseController.render(response, JSON.toJSONString(data), "application/json");
  }

  @Override
  public void success(HttpServletResponse response, FileObject fileObject) {
    Preconditions.checkArgument(fileObject != null, "FileObject must not be null");
    BaseController.render(response, JSON.toJSONString(fileObject), "application/json");
  }

}
