package com.github.catstiger.multipart.controller.after;

import javax.servlet.http.HttpServletRequest;

public final class AfterUploadFactory {

  public static AfterUpload getAfterUpload(HttpServletRequest request) {
    AfterUpload afterUpload;
    if (request.getParameter("ke") != null || request.getParameter("k") != null) { // Kindeditor
      afterUpload = new KindEditorAfterUpload();
    } else if (request.getParameter("la") != null) { // Layui
      afterUpload = new LayuiAfterUpload();
    } else if (request.getParameter("uni") != null) { //uni-app
      afterUpload = new UniAppAfterUpload();
    } else {
      afterUpload = new CommonAfterUpload();
    }

    return afterUpload;
  }
  
  private AfterUploadFactory() {
    // Do nothing
  }
}
