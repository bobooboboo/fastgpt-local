package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

import java.util.List;

/**
 * ModifiedLawContentResponseDTO
 *
 * @author: boboo
 * @Date: 2024/10/25 10:26
 **/
@Data
public class ModifiedLawContentResponseDTO {

    private String title;

    private List<ModifiedLawContent> items;

    @Data
    public static class ModifiedLawContent {
        private String oldContent;
        private String newContent;
        private String type;
    }
}
