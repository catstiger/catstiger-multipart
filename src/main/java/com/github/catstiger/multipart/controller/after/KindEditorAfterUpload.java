package com.github.catstiger.multipart.controller.after;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.common.web.controller.BaseController;
import com.github.catstiger.multipart.model.FileObject;

public class KindEditorAfterUpload implements AfterUpload {

  @Override
  public void error(HttpServletResponse response, FileObject fileObject, String errorMessage) {
    new CommonAfterUpload().error(response, fileObject, errorMessage);
  }

  @Override
  public void success(HttpServletResponse response, FileObject fileObject) {
    Map<String, Object> map = new HashMap<>(2);
    map.put("error", 0);
    map.put("url", fileObject.getUrl());
    
    BaseController.render(response, JSON.toJSONString(map), "application/json");
  }

}
