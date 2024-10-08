package com.hztech.fastgpt.model.dto.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * HigherLevelLawResponseDTO
 *
 * @author: boboo
 * @Date: 2024/9/27 16:50
 **/
@Data
public class HigherLevelLawResponseDTO {

    @ApiModelProperty("法律法规标题")
    private String title;

    @ApiModelProperty("法律法规内容")
    private String content;

    @ApiModelProperty("法律法规平台")
    private String platform;

    @ApiModelProperty("模型")
    private String model;
}
