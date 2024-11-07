package com.hztech.fastgpt.model.dto.request;

import cn.hutool.core.util.StrUtil;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * LawStatisticsRequestDTO
 *
 * @author: boboo
 * @Date: 2024/10/21 9:35
 **/
@Data
public class LawStatisticsRequestDTO {

    @ApiModelProperty("法律性质")
    private List<String> type;

    @ApiModelProperty("时效性")
    private List<EnumLawStatus> status;

    @ApiModelProperty("发布日期开始时间")
    private String publishBegin;

    @ApiModelProperty("发布日期结束时间")
    private String publishEnd;

    @ApiModelProperty("发布主体")
    private String subject;

    /**
     * 发布年度
     */
    private Integer year;

    private String city;

    public String getPublishBegin() {
        return StrUtil.emptyToNull(publishBegin);
    }

    public String getPublishEnd() {
        return StrUtil.emptyToNull(publishEnd);
    }
}
