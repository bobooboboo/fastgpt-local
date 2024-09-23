package com.hztech.fastgpt.service;

import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.model.dto.request.LawInfoSearchRequestDTO;
import com.hztech.fastgpt.model.dto.request.LawPageRequestDTO;
import com.hztech.fastgpt.model.dto.response.LawInfoSearchResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawInfoSearchV2ResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawPageResponseDTO;
import com.hztech.fastgpt.model.dto.response.TempLawPageResponseDTO;
import com.hztech.model.dto.HzPage;
import com.hztech.service.transactionscript.IHzTransactionScriptService;

/**
 * ILawService 服务接口
 *
 * @author HZ
 */
public interface ILawService extends IHzTransactionScriptService<LawDO, Long> {

    /**
     * 根据法规名称搜索法规
     */
    HzPage<LawPageResponseDTO> lawPage(LawPageRequestDTO requestDTO);

    HzPage<TempLawPageResponseDTO> tempLawPage(LawPageRequestDTO requestDTO);

    boolean save(LawDO lawDO);

    /**
     * 智能检索
     */
    HzPage<LawInfoSearchResponseDTO> search(LawInfoSearchRequestDTO requestDTO);

    HzPage<LawInfoSearchV2ResponseDTO> searchV2(LawInfoSearchRequestDTO requestDTO);
}
