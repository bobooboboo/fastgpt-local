package com.hztech.fastgpt.model.dto.request;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.hztech.fastgpt.model.enums.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 智能检索请求类
 *
 * @author: boboo
 * @Date: 2023/9/27 14:23
 **/
@Data
public class LawInfoSearchRequestDTO {

    @ApiModelProperty(value = "检索内容")
    private String text;

    /**
     * 检索内容列表
     */
    private Object textList;

    @ApiModelProperty("搜索类型 0：全文；1：标题；2：内容")
    private EnumLawSearchType searchType;

    @ApiModelProperty("1:全文检索 2:条文检索")
    private EnumLawMode mode;

    @ApiModelProperty("文件类型 [0:宪法,1:法律,2:行政法规,3:地方性法规,4:部门规章,5:地方政府规章,6:规范性文件,7:其他]")
    private List<EnumLawType> type;

    @ApiModelProperty("时效性 [0:未生效,1:有效,2:已修改,3:已废止]")
    private List<EnumLawStatus> status;

    @ApiModelProperty("发布日期开始时间")
    private String publishBegin;

    @ApiModelProperty("发布日期结束时间")
    private String publishEnd;

    @ApiModelProperty("发布主体")
    private String subject;

    @ApiModelProperty("检索类型 0:智能 1:精确")
    private EnumLawSearchMode searchMode;

    @ApiModelProperty("排序 0:相关度 1:发布时间升序 2:发布时间降序 3:实施时间升序 4:实施时间降序")
    private EnumLawSortType sortType;

    @ApiModelProperty("当前页数")
    private Long current;

    @ApiModelProperty("每页数据量")
    private Long size;

    @ApiModelProperty("高亮开始标签")
    private String highlightPreTag;

    @ApiModelProperty("高亮结束标签")
    private String highlightPostTag;

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
    /**
     * 是否查询全文
     */
    private Boolean fullContent;
    /**
     * 发布年度
     */
    private Integer year;

    /**
     * 数据源
     */
    private List<EnumLawSource> dataSource;

    private String city;

    public String getPublishBegin() {
        return StrUtil.emptyToNull(publishBegin);
    }

    public String getPublishEnd() {
        return StrUtil.emptyToNull(publishEnd);
    }

    public Integer getArticle() {
        return ObjectUtil.equal(article, 0) ? null : article;
    }

    public Integer getSection() {
        return ObjectUtil.equal(section, 0) ? null : section;
    }

    public Integer getChapter() {
        return ObjectUtil.equal(chapter, 0) ? null : chapter;
    }

    public Integer getPart() {
        return ObjectUtil.equal(part, 0) ? null : part;
    }
}
