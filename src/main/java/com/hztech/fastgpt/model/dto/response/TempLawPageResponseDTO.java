package com.hztech.fastgpt.model.dto.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * TempLawPageResponseDTO
 *
 * @author: boboo
 * @Date: 2024/9/20 13:47
 **/
@Data
public class TempLawPageResponseDTO {

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("附件地址")
    private String url;
}
