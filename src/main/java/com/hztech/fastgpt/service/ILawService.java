package com.hztech.fastgpt.service;

import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.model.dto.request.LawInfoSearchRequestDTO;
import com.hztech.fastgpt.model.dto.request.LawPageRequestDTO;
import com.hztech.fastgpt.model.dto.request.LawStatisticsRequestDTO;
import com.hztech.fastgpt.model.dto.request.QueryLawRequestDTO;
import com.hztech.fastgpt.model.dto.response.*;
import com.hztech.model.dto.HzPage;
import com.hztech.service.transactionscript.IHzTransactionScriptService;

import java.util.Map;

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

//    HzPage<TempLawPageResponseDTO> tempLawPage(LawPageRequestDTO requestDTO);

    boolean save(LawDO lawDO);

    /**
     * 智能检索
     */
    HzPage<LawInfoSearchResponseDTO> search(LawInfoSearchRequestDTO requestDTO);

    HzPage<LawInfoSearchV2ResponseDTO> searchV2(LawInfoSearchRequestDTO requestDTO);

    /**
     * 法规统计
     */
    Map<String, Long> lawStatistics(LawStatisticsRequestDTO requestDTO);

//    /**
//     * 下载法规文件
//     */
//    ResponseEntity<StreamingResponseBody> downloadFile(String id);

    /**
     * 已修订法规查询
     */
    QueryLawResponseDTO queryLaw(QueryLawRequestDTO requestDTO);

    /**
     * 查询法规修订内容
     */
    ModifiedLawContentResponseDTO getModifiedContent(QueryLawRequestDTO requestDTO);

    LawPageResponseDTO getLawInfo(String outerId);

    LawStatisticsResponseDTO lawStatisticsV2(LawStatisticsRequestDTO requestDTO);

    void initPreviewUrl();
}
