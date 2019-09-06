package com.github.catstiger.multipart.controller;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.catstiger.multipart.controller.after.AfterUpload;
import com.github.catstiger.multipart.controller.after.AfterUploadFactory;
import com.github.catstiger.multipart.model.FileObject;

@SuppressWarnings("serial")
public class FasterUploadServlet extends AbstractUploadServlet {

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // 如果请求中没有cup参数,则是FasterUpload请求
    AfterUpload afterUpload = AfterUploadFactory.getAfterUpload(request);
    try {
      FileObject fileObject = this.doUploadSingle(request, response);
      afterUpload.success(response, fileObject);
    } catch (Exception e) {
      e.printStackTrace();
      afterUpload.error(response, null, e.getMessage());
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    this.doGet(req, resp);
  }

}
