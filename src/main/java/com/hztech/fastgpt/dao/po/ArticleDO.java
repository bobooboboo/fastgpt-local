package com.hztech.fastgpt.dao.po;

import cn.org.atool.fluent.mybatis.annotation.FluentMybatis;
import cn.org.atool.fluent.mybatis.annotation.TableField;
import cn.org.atool.fluent.mybatis.annotation.TableId;
import cn.org.atool.fluent.mybatis.base.BaseEntity;
import cn.org.atool.fluent.mybatis.base.IEntity;
import com.hztech.config.IHzFmBaseMapper;
import com.hztech.fluentmybatis.defaults.IBaseDefaultSetter;
import com.hztech.model.po.ICreateTime;
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
        table = "article",
        defaults = IBaseDefaultSetter.class,
        superMapper = IHzFmBaseMapper.class,
        mapperBeanPrefix = "",
        suffix = "DO"
)
public class ArticleDO extends BaseEntity implements Serializable,
        IDataObjectWithId<Long>, ICreateTime {

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
    @TableField("region_code")
    @Length(max = 20)
    private String regionCode;

    /**
     * 标题
     */
    @TableField("title")
    @Length(max = 255)
    private String title;

    /**
     * 正文
     */
    @TableField("content")
    private String content;

    /**
     * 来源
     */
    @TableField("from")
    @Length(max = 255)
    private String from;

    /**
     * 发布者
     */
    @TableField("author")
    @Length(max = 255)
    private String author;

    /**
     * 发布时间
     */
    @TableField("pubdate")
    @Length(max = 255)
    private String pubdate;

    /**
     * 数据源
     */
    @TableField("source")
    @Length(max = 512)
    private String source;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;


    @Override
    public final Class<? extends IEntity> entityClass() {
        return ArticleDO.class;
    }
}