package com.hztech.fastgpt.model.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 智能检索结果类
 *
 * @author: boboo
 * @Date: 2023/9/27 14:44
 **/
@ApiModel(description = "智能检索结果类")
@Data
public class LawInfoSearchResponseDTO {

    private Long id;

    private String outerId;

    /**
     * 类型
     */
    @ApiModelProperty("类型")
    private String type;

    /**
     * 时效性
     */
    @ApiModelProperty("时效性")
    private String status;

    /**
     * 标题
     */
    @ApiModelProperty("标题")
    private String title;

    /**
     * 生效日期
     */
    @ApiModelProperty("生效日期")
    private String effective;

    /**
     * 发布日期
     */
    @ApiModelProperty("发布日期")
    private String publish;

    /**
     * 章节内容
     */
    @ApiModelProperty("章节内容")
    private String content;

    @ApiModelProperty("制定机关")
    private String subject;

    /**
     * 附件地址
     */
    @ApiModelProperty("附件地址")
    private String docFileUrl;

    /**
     * 附件地址
     */
    @ApiModelProperty("附件地址")
    private String pdfFileUrl;
}
