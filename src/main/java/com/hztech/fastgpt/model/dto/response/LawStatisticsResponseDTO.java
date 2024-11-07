package com.hztech.fastgpt.model.dto.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * LawStatisticsResponseDTO
 *
 * @author: boboo
 * @Date: 2024/10/20 14:03
 **/
@Data
public class LawStatisticsResponseDTO {

    @ApiModelProperty("")
    private Integer total;

    @ApiModelProperty("宪法")
    private Long constitutionCount;

    @ApiModelProperty("法律")
    private Long statuteCount;

    @ApiModelProperty("行政法规")
    private Long administrativeRegulationsCount;
//
//    @ApiModelProperty("监察法规")
//    private Long supervisionRegulationsCount;
//
//    @ApiModelProperty("司法解释")
//    private Long judicialInterpretationCount;

    @ApiModelProperty("地方性法规")
    private Long localRegulationsCount;

//    @ApiModelProperty("法律解释")
//    private Long legalInterpretationCount;
//
//    @ApiModelProperty("有关法律问题和重大问题的决定")
//    private Long legalQuestionsCount;
//
//    @ApiModelProperty("修改、废止的决定")
//    private Long modificationAndAbolitionCount;
}
