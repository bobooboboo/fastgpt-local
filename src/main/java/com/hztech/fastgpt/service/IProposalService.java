package com.hztech.fastgpt.service;

import com.hztech.fastgpt.model.dto.request.ProposalInfoSearchRequestDTO;

/**
 * IProposalService
 *
 * @author: boboo
 * @Date: 2024/10/14 17:33
 **/
public interface IProposalService {
    Object search(ProposalInfoSearchRequestDTO requestDTO);
}
