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
public class LawInfoSearchV2ResponseDTO {

    /**
     * 标题
     */
    @ApiModelProperty("标题")
    private String title;

    /**
     * 章节内容
     */
    @ApiModelProperty("章节内容")
    private String content;

    /**
     * 附件地址，PDF优先
     */
    private String fileUrl;

}
