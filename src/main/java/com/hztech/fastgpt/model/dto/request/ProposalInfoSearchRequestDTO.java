package com.hztech.fastgpt.model.dto.request;

import lombok.Data;

/**
 * ProposalInfoSearchRequestDTO
 *
 * @author: boboo
 * @Date: 2024/10/14 17:32
 **/
@Data
public class ProposalInfoSearchRequestDTO {
    private String leader;
    private String delegation;
    private String title;
    private String firstLevelCategory;
    private String organizer;
    private Object textList;
}
