package com.hztech.fastgpt.model.dto.response;

import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.model.enums.EnumLawType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * LawPageResponseDTO
 *
 * @author: boboo
 * @Date: 2024/9/19 16:23
 **/
@Data
public class LawPageResponseDTO {

    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("doc文件链接")
    private String docFileUrl;

    @ApiModelProperty("pdf文件链接")
    private String pdfFileUrl;

    @ApiModelProperty("生效日期")
    private String effective;

    @ApiModelProperty("发布日期")
    private String publish;

    @ApiModelProperty("文件类型 EnumLawType [CONSTITUTION=0 宪法; STATUTE=1 法律; ADMINISTRATIVE_REGULATIONS=2 行政法规; SUPERVISION_REGULATIONS=3 监察法规; JUDICIAL_INTERPRETATION=4 司法解释; LOCAL_REGULATIONS=5 地方性法规]")
    private EnumLawType type;

    @ApiModelProperty("时效性 EnumLawStatus [NOT_EFFECTIVE=0 尚未生效; EFFECTIVE=1 有效; MODIFIED=2 已修改; REPEALED=3 已废止]")
    private EnumLawStatus status;
}
