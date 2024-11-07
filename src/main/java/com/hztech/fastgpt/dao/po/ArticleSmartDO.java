package com.hztech.fastgpt.dao.po;

import cn.org.atool.fluent.mybatis.annotation.FluentMybatis;
import cn.org.atool.fluent.mybatis.annotation.TableField;
import cn.org.atool.fluent.mybatis.annotation.TableId;
import cn.org.atool.fluent.mybatis.base.BaseEntity;
import com.hztech.config.IHzFmBaseMapper;
import com.hztech.fluentmybatis.defaults.IBaseDefaultSetter;
import com.hztech.model.po.IDataObjectWithId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 *
 * @author HZ
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@FluentMybatis(
        table = "article_smart",
        defaults = IBaseDefaultSetter.class,
        superMapper = IHzFmBaseMapper.class,
        mapperBeanPrefix = "",
        suffix = "DO"
)
public class ArticleSmartDO extends BaseEntity implements Serializable,
        IDataObjectWithId<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(
            value = "id",
            auto = false
    )
    @NotNull
    private Long id;

    /**
     * 行政区划代码
     */
    @TableField("code")
    @Length(max = 32)
    @NotNull
    private String code;

    /**
     * 标题
     */
    @TableField("title")
    @Length(max = 256)
    private String title;

    /**
     * 正文
     */
    @TableField("text_content")
    private String textContent;

    /**
     * 富文本
     */
    @TableField("rich_content")
    private String richContent;

    /**
     * 来源
     */
    @TableField("from")
    @Length(max = 256)
    private String from;

    /**
     * 作者
     */
    @TableField("author")
    @Length(max = 64)
    private String author;

    /**
     * 发布时间
     */
    @TableField("pubdate")
    @Length(max = 64)
    private String pubdate;

    /**
     * 文件列表
     */
    @TableField("file_list")
    @Length(max = 512)
    private String fileList;

    /**
     * 备注信息
     */
    @TableField("remark")
    @Length(max = 512)
    private String remark;

    /**
     * 采集来源
     */
    @TableField("source")
    @Length(max = 512)
    private String source;

    /**
     * 原文地址
     */
    @TableField("original")
    @Length(max = 512)
    private String original;

    /**
     * 添加时间
     */
    @TableField("add_time")
    private LocalDateTime addTime;


    @Override
    public final Class<? extends BaseEntity> entityClass() {
        return ArticleSmartDO.class;
    }
}