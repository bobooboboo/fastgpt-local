package com.hztech.fastgpt.dao.po;

import cn.org.atool.fluent.mybatis.annotation.FluentMybatis;
import cn.org.atool.fluent.mybatis.annotation.TableField;
import cn.org.atool.fluent.mybatis.annotation.TableId;
import cn.org.atool.fluent.mybatis.base.BaseEntity;
import com.hztech.config.IHzFmBaseMapper;
import com.hztech.fastgpt.model.enums.EnumLawSource;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.model.enums.EnumLawType;
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
 * 国家法律法规统计信息(root:this)
 *
 * @author HZ
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@FluentMybatis(
        table = "law",
        defaults = IBaseDefaultSetter.class,
        superMapper = IHzFmBaseMapper.class,
        mapperBeanPrefix = "",
        suffix = "DO"
)
public class LawDO extends BaseEntity implements Serializable,
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
     * 法律法规id
     */
    @TableField("outer_id")
    @Length(max = 255)
    @NotNull
    private String outerId;

    /**
     * 标题
     */
    @TableField("title")
    @Length(max = 255)
    @NotNull
    private String title;

    /**
     * 文件类型 EnumLawType [CONSTITUTION=0 宪法; STATUTE=1 法律; ADMINISTRATIVE_REGULATIONS=2 行政法规; SUPERVISION_REGULATIONS=3 监察法规; JUDICIAL_INTERPRETATION=4 司法解释; LOCAL_REGULATIONS=5 地方性法规]
     */
    @TableField("type")
    @NotNull
    private EnumLawType type;

    /**
     * 时效性 EnumLawStatus [NOT_EFFECTIVE=0 尚未生效; EFFECTIVE=1 有效; MODIFIED=2 已修改; REPEALED=3 已废止]
     */
    @TableField("status")
    private EnumLawStatus status;

    /**
     * 主体
     */
    @TableField("subject")
    @Length(max = 255)
    @NotNull
    private String subject;

    /**
     * 生效日期
     */
    @TableField("effective")
    @Length(max = 255)
    private String effective;

    /**
     * 发布日期
     */
    @TableField("publish")
    @Length(max = 255)
    private String publish;

    /**
     * 数据源
     */
    @TableField("data_source")
    @NotNull
    private EnumLawSource dataSource;

    /**
     * doc附件地址
     */
    @TableField("doc_file_url")
    @Length(max = 255)
    private String docFileUrl;

    /**
     * pdf附件地址
     */
    @TableField("pdf_file_url")
    @Length(max = 255)
    private String pdfFileUrl;

    @TableField("original_url")
    @Length(max = 255)
    private String originalUrl;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField("modify_time")
    private LocalDateTime modifyTime;


    @Override
    public final Class<? extends BaseEntity> entityClass() {
        return LawDO.class;
    }
}