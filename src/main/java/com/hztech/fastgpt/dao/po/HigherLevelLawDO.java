package com.hztech.fastgpt.dao.po;

import cn.org.atool.fluent.mybatis.annotation.FluentMybatis;
import cn.org.atool.fluent.mybatis.annotation.TableField;
import cn.org.atool.fluent.mybatis.annotation.TableId;
import cn.org.atool.fluent.mybatis.base.BaseEntity;
import com.hztech.config.IHzFmBaseMapper;
import com.hztech.fluentmybatis.defaults.IBaseDefaultSetter;
import com.hztech.model.po.base.IBaseDataObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 国家法律法规上位法信息(root:this)
 *
 * @author HZ
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@FluentMybatis(
        table = "higher_level_law",
        defaults = IBaseDefaultSetter.class,
        superMapper = IHzFmBaseMapper.class,
        mapperBeanPrefix = "",
        suffix = "DO"
)
public class HigherLevelLawDO extends BaseEntity implements Serializable,
        IBaseDataObject<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(
            value = "id",
            auto = false
    )
    @NotNull
    private Long id;

    /**
     * 法律法规标题
     */
    @TableField("title")
    @Length(max = 255)
    @NotNull
    private String title;

    /**
     * 向AI提出的问题
     */
    @TableField("question")
    @Length(max = 255)
    @NotNull
    private String question;

    /**
     * AI回复内容
     */
    @TableField("content")
    @Length(max = 4000)
    @NotNull
    private String content;

    /**
     * 大模型平台
     */
    @TableField("platform")
    @Length(max = 255)
    @NotNull
    private String platform;

    /**
     * 大模型名称
     */
    @TableField("model")
    @Length(max = 255)
    @NotNull
    private String model;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @NotNull
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("modify_time")
    @NotNull
    private LocalDateTime modifyTime;


    @Override
    public final Class<? extends BaseEntity> entityClass() {
        return HigherLevelLawDO.class;
    }
}