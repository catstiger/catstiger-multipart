package com.github.catstiger.multipart.controller;

import java.io.IOException;
import java.util.Date;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.github.catstiger.common.util.ContentTypes;
import com.github.catstiger.common.util.Exceptions;
import com.github.catstiger.common.util.FileHelper;
import com.github.catstiger.common.web.WebUtil;
import com.github.catstiger.multipart.model.FileObject;
import com.github.catstiger.multipart.service.FileObjectService;
import com.github.catstiger.multipart.service.FileService;

/**
 * 查看和下载上传文件的Servlet
 * 
 * @author catstiger@gmail.com
 * 
 */
public class FileAccessServlet extends HttpServlet {
  private static final long serialVersionUID = 7581802655256306989L;

  private static Logger logger = LoggerFactory.getLogger(FileAccessServlet.class);

  private FileService fileService;
  private WebApplicationContext ctx;
  private FileObjectService fileObjectService;

  // private ImageHelper imageHelper;

  /**
   * @see GenericServlet#init(ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    // Spring
    ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
    // 从Spring中取得FileService实例
    if (ctx != null) {
      fileService = ctx.getBean(FileService.class);
      fileObjectService = ctx.getBean(FileObjectService.class);
    }

    if (fileService == null) {
      logger.error("No bean of type '{}' found.", FileService.class.getName());
      throw Exceptions.unchecked("No FileService implementation found.");
    }

    if (fileObjectService == null) {
      throw Exceptions.unchecked("No FileAuthService implementation found.");
    }
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String uri = getUri(req);
    setup(req, resp, uri); // 设置响应头
    fileService.read(uri, resp.getOutputStream());
  }

  /**
   * 返回请求URL的扩展名
   * 
   */
  protected String getUri(HttpServletRequest req) {
    String uri = req.getRequestURI();
    if (StringUtils.hasText(uri)) {
      if (uri.indexOf("?") > 0) {
        uri = uri.substring(0, uri.indexOf("?"));
      }
      if (uri.startsWith(req.getContextPath())) {
        uri = org.apache.commons.lang3.StringUtils.replace(uri, req.getContextPath(), "");
      }
      //logger.debug("URI:{}", uri);
      return uri;
    }
    return "";
  }

  /**
   * 判断文件是否是图片
   * 
   */
  public static boolean isPic(String filename) {
    return FileHelper.isPic(filename);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    doGet(req, resp);
  }

  /**
   * 设置HttpServletResponse header信息
   * 
   * @param uri 请求的URL，用于判断是否是图片
   */
  private void setup(HttpServletRequest req, HttpServletResponse resp, String uri) {
    if (FileHelper.isPic(uri)) {
      resp.addHeader("Cache-Control", "max-age=31536000");
      resp.addHeader("Expires", new DateTime(new Date()).plusYears(2).toDate().toString());
    } else {
      resp.addHeader("Cache-Control", "0");
      resp.addHeader("Expires", "0");
    }
   
    FileObject fileObject = fileObjectService.get(uri);

    // 设置Content Type
    String contentType = ContentTypes.get(StringUtils.getFilenameExtension(getUri(req)));
    resp.setContentType(contentType);

    // 文件下载（除图片和swf文件之外，其他文件都强制下载）
    if (!FileHelper.isPic(uri) && !FileHelper.isSwf(uri)) {
      resp.setContentType(ContentTypes.get("type")); // 下载ContentType
      String filename = StringUtils.getFilename(uri);
      if (fileObject != null) {
        filename = fileObject.getName();
      }
      WebUtil.setFileDownloadHeader(resp, filename);
    }
    // 设置ContentLength
    resp.setContentLength(Long.valueOf(fileService.getContentLength(uri)).intValue());
  }

}
