package com.github.catstiger.multipart.controller.after;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.github.catstiger.multipart.model.FileObject;

public class UniAppAfterUpload implements AfterUpload {

  @Override
  public void error(HttpServletResponse response, FileObject fileObject, String errorMessage) {
    //Do nothing.
  }

  @Override
  public void success(HttpServletResponse response, FileObject fileObject) {
    try {
      response.getWriter().write(fileObject.getUrl());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
