package com.github.catstiger.multipart.controller;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.Page;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.web.controller.BaseController;
import com.github.catstiger.multipart.model.FileObject;
import com.github.catstiger.multipart.service.FileObjectService;

@Controller
@RequestMapping("/fileaccess")
public class FileAccessController extends BaseController {
  @Autowired
  private FileObjectService fileObjectService;

  @Autowired
  private JdbcTemplateProxy jdbcTemplate;

  /**
   * 查询FileObject对象
   */
  @RequestMapping("/index")
  @ResponseBody
  public void index(@RequestParam("targetClass") String targetClass, @RequestParam("targetId") String targetId,
      @RequestParam(required = false, value = "needTargetId", defaultValue = "false") Boolean needTargetId) {

    if (StringUtils.isBlank(targetId) && needTargetId) {
      logger.debug("TargetId is required.");
      renderJson(JSON.toJSONString(page())); // 没有targetId,
      return;
    }

    Page page = page();
    SQLReady sql = new SQLRequest(FileObject.class, true).select().where()
        .and("target_class=?", StringUtils.isNotBlank(targetClass), new Object[] {targetClass})
        .and("target_id=?", StringUtils.isNotBlank(targetId), new Object[] {targetId})
        .orderBy("name", SQLReady.ASC);
    
    List<FileObject> rows = jdbcTemplate.query(sql.limitSql(page.getStart(), page.getLimit()), new BeanPropertyRowMapper<FileObject>(FileObject.class),
        sql.getArgs());
    Long total = jdbcTemplate.queryTotal(sql);

    page.setRows(rows);
    page.setTotal(total);

    renderJson(JSON.toJSONString(page));
  }

  /**
   * 关联文件和所属对象
   */
  @RequestMapping("/adjoin_entity")
  @ResponseBody
  public Map<String, Object> adjoinEntity(@RequestParam("urls") String urls, @RequestParam("targetId") String targetId) {
    try {
      fileObjectService.saveFiles(urls, targetId);
      return forExt("操作成功！", true);
    } catch (Exception e) {
      e.printStackTrace();
      return forExt(e.getMessage());
    }

  }

  /**
   * 删除一个文件及其对应的FileObject对象
   */
  @RequestMapping("/remove")
  @ResponseBody
  public Map<String, Object> remove(@RequestParam("url") String url) {
    try {
      fileObjectService.remove(url);
      //因执行到此行报file_transfer_snapshot表不存在,且暂时用不到此表,故注释掉此行
      //jdbcTemplate.update("delete from file_transfer_snapshot where url=?", url);
      return forExt("文件已经成功删除！", true);
    } catch (Exception e) {
      e.printStackTrace();
      return forExt("文件删除失败！");
    }
  }

  /**
   * 根据URL，render文件信息
   */
  @RequestMapping("/load")
  @ResponseBody
  public Map<String, Object> load(@RequestParam("url") String url) {
    FileObject fileObject = fileObjectService.get(url);
    if (fileObject == null) {
      fileObject = new FileObject();
      fileObject.setUrl(url);
      fileObject.setName("点击查看图片");
    }
    return forExt(fileObject, true);
  }
}
