package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

/**
 * ListDataResponseDTO
 *
 * @author: boboo
 * @Date: 2024/10/14 14:16
 **/
@Data
public class ListDataResponseDTO {
    private String _id;
    private String datasetId;
    private String collectionId;
    private String q;
    private String a;
    private Integer chunkIndex;
}
