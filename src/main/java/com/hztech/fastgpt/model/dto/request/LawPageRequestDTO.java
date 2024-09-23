package com.hztech.fastgpt.model.dto.request;

import com.hztech.model.dto.HzPageParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * LawPageRequestDTO
 *
 * @author: boboo
 * @Date: 2024/9/19 16:28
 **/
@EqualsAndHashCode(callSuper = false)
@Data
public class LawPageRequestDTO extends HzPageParam {

    @ApiModelProperty("法规标题")
    private String title;
}
