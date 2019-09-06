package com.github.catstiger.multipart.controller.after;

import javax.servlet.http.HttpServletResponse;

import com.github.catstiger.multipart.model.FileObject;

/**
 * 文件上传后，要向客户端返回数据，不同的客户端要求的数据格式不同，因此需要分别处理。
 * {@link AfterUpload}的每一个实现类都对应一个客户端的规范。
 * @author samlee
 *
 */
public interface AfterUpload {
  /**
   * 当文件上传失败的时候调用
   * @param fileObject 上传文件对象
   * @param errorMessage 错误信息
   * @return 符合某个客户端规范的数据
   */
  void error(HttpServletResponse response, FileObject fileObject, String errorMessage);
  
  /**
   * 当文件上传成功的时候调用
   * @param fileObject 上传文件对象
   * @return 符合某个客户端规范的数据
   */
  void success(HttpServletResponse response, FileObject fileObject);
}
