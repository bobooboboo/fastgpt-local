package com.hztech.fastgpt.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateIndexAndMappingRequestDTO
 *
 * @author: boboo
 * @Date: 2024/10/14 14:05
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIndexAndMappingRequestDTO {
    private String configFileName;

    private String indexName;

    private String indexMapping;
}
