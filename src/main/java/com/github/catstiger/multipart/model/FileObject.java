package com.github.catstiger.multipart.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 上传文件对象
 * 
 * @author sam
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "file_object")
public class FileObject implements Serializable {
	/**
	 * 主键，访问URL
	 */
	private String url;
	/**
	 * 原文件名
	 */
	private String name;

	/**
	 * 上传人员ID
	 */
	private String creator;

	/**
	 * 上传时间
	 */
	private Date createTime;

	/**
	 * 扩展名
	 */
	private String ext;

	/**
	 * 文件大小
	 */
	private Long size = 0L;

	/**
	 * 文件大小的字符串形式
	 */
	private String sizeStr;

	/**
	 * 文件所属主体的类名
	 */
	private String targetClass;
	/**
	 * 文件所属主体的ID
	 */
	private String targetId;

	public FileObject() {

	}


	@Id
	@Column(length = 100)
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Column(length = 100)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(length = 30)
	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(length = 30)
	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	@Column(name = "size_")
	public Long getSize() {
		return size;
	}

	/**
	 * 设置文件大小（字节数），如果为null，则缺省设置为0
	 */
	public void setSize(Long size) {
		if (size == null) {
			size = 0L;
		}
		this.size = size;
	}

	@Column(length = 20)
	public String getSizeStr() {
		return sizeStr;
	}

	public void setSizeStr(String sizeStr) {
		this.sizeStr = sizeStr;
	}

	@Column(length = 120)
	public String getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(String targetClass) {
		this.targetClass = targetClass;
	}

	@Column(length = 36)
	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileObject other = (FileObject) obj;
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.equals(other.url)) {
			return false;
		}
		return true;
	}

}
