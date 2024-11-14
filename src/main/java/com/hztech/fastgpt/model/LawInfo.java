package com.hztech.fastgpt.model;

import cn.hutool.core.util.StrUtil;
import com.frameworkset.orm.annotation.ESId;
import com.frameworkset.orm.annotation.ESIndex;
import com.frameworkset.orm.annotation.ESMetaHighlight;
import com.hztech.fastgpt.model.enums.EnumLawSource;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.model.enums.EnumLawType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 法律法规数据
 *
 * @author: boboo
 * @Date: 2023/9/25 15:52
 **/
@Data
@ESIndex(name = "law_info")
public class LawInfo implements Serializable {

    /**
     * id
     */
    @ESId
    private Long id;

    /**
     * 外部id
     */
    private String outerId;

    /**
     * 类型
     */
    private EnumLawType type;

    /**
     * 时效性
     */
    private EnumLawStatus status;

    /**
     * 标题
     */
    private String title;

    /**
     * 主体
     */
    private String subject;

    /**
     * 生效日期
     */
    private String effective;

    /**
     * 发布日期
     */
    private String publish;

    /**
     * 章节内容
     */
    private String content;

    /**
     * wps附件地址
     */
    private String docFileUrl;

    /**
     * pdf附件地址
     */
    private String pdfFileUrl;

    /**
     * 预览附件地址
     */
    private String previewUrl;

    /**
     * 第几编
     */
    private Integer part;

    /**
     * 第几章
     */
    private Integer chapter;

    /**
     * 第几节
     */
    private Integer section;

    /**
     * 第几条
     */
    private Integer article;

    private EnumLawSource dataSource;

    /**
     * 文档对应的高亮检索信息
     */
    @ESMetaHighlight
    private Map<String, List<Object>> highlights;

    public String getPublish() {
        return StrUtil.emptyToNull(publish);
    }

    public String getEffective() {
        return StrUtil.emptyToNull(effective);
    }
}
