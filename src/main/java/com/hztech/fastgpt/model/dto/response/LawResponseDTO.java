package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

import java.util.List;

/**
 * LawResponseDTO
 *
 * @author: boboo
 * @Date: 2024/2/19 15:28
 **/
@Data
public class LawResponseDTO {

    private List<LawDetailResponseDTO> details;

    private String pdfFileUrl;

    private String docFileUrl;
}
