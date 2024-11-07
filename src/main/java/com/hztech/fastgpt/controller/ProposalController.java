package com.hztech.fastgpt.controller;

import com.hztech.fastgpt.model.dto.request.ProposalInfoSearchRequestDTO;
import com.hztech.fastgpt.service.IProposalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 议案建议控制器
 *
 * @author: boboo
 * @Date: 2024/10/14 17:24
 **/
@Slf4j
@Api(tags = {"议案建议控制器控制器"})
@RestController
@RequiredArgsConstructor
public class ProposalController {

    private final IProposalService proposalService;

    @ApiOperation("智能检索")
    @PostMapping("/api/v1/proposal/search")
    public Object search(@RequestBody ProposalInfoSearchRequestDTO requestDTO) {
        return proposalService.search(requestDTO);
    }
}
