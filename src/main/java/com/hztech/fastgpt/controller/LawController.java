package com.hztech.fastgpt.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.hztech.fastgpt.model.LawInfo;
import com.hztech.fastgpt.model.dto.request.*;
import com.hztech.fastgpt.model.dto.response.*;
import com.hztech.fastgpt.service.IHigherLevelLawService;
import com.hztech.fastgpt.service.ILawContentService;
import com.hztech.fastgpt.service.ILawService;
import com.hztech.fastgpt.util.LawDataUtils;
import com.hztech.model.dto.HzPage;
import com.hztech.model.dto.HzResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 国家法律法规库控制器
 *
 * @author: boboo
 * @Date: 2024/2/4 11:11
 **/
@Slf4j
@Api(tags = {"国家法律法规库控制器"})
@RestController
@RequiredArgsConstructor
public class LawController {

    private final ILawService lawService;

    private final ILawContentService lawContentService;

    private final BBossESStarter bossESStarter;

    private final IHigherLevelLawService higherLevelLawService;

    @Async
    @ApiIgnore
    @ApiOperation("获取远程立法数据到DB")
    @GetMapping("/api/v1/fetchRemoteData")
    public void fetchRemoteData(@RequestParam("page") Integer page, @RequestParam("stop") Integer stop) {
        LawDataUtils.fetchData(page, stop);
    }

    @ApiOperation("法规分页接口")
    @GetMapping("/api/v2/law/page")
    public HzResponse<HzPage<LawPageResponseDTO>> lawPage(LawPageRequestDTO requestDTO) {
        return HzResponse.success(lawService.lawPage(requestDTO));
    }


    @ApiIgnore
    @ApiOperation("创建索引")
    @PostMapping("/api/v1/createIndexAndMapping")
    public HzResponse<?> createIndexAndMapping() {
        return createIndexAndMapping(new CreateIndexAndMappingRequestDTO("createLawInfoIndex.xml", "law_info", "createLawInfoIndex"));
    }

    @ApiIgnore
    @ApiOperation("创建索引")
    @PostMapping("/api/v2/createIndexAndMapping")
    public HzResponse<?> createIndexAndMapping(@RequestBody CreateIndexAndMappingRequestDTO requestDTO) {
        ClientInterface restClient = bossESStarter.getConfigRestClient("esmapper/" + requestDTO.getConfigFileName());
        String result = restClient.createIndiceMapping(requestDTO.getIndexName(), requestDTO.getIndexMapping());
        return HzResponse.success(result);
    }

//    @GetMapping("/api/v1/proposal/dataset2es")
//    public HzResponse<Void> proposalDataset2es() {
//        ServiceRequests serviceRequests = HzSpringUtils.getBean(ServiceRequests.class);
//        for (int i = 1; i <= 4; i++) {
//            Map<String, Object> map = serviceRequests.listData("670680ed04a986474376fb75" , i, 30, null);
//            List<ListDataResponseDTO> list = JSONUtil.parseObj(map).getJSONObject("data").getBeanList("data" , ListDataResponseDTO.class);
//            List<ProposalInfo> proposalInfoList = list.stream().map(x -> ProposalInfo.parse(x.getA())).collect(Collectors.toList());
//            ClientInterface restClient = bossESStarter.getRestClient();
//            restClient.addDocuments("proposal_info" , proposalInfoList);
//        }
//        return HzResponse.success();
//    }

    @ApiIgnore
    @ApiOperation("从DB同步数据到ES")
    @GetMapping("/api/v1/db2es")
    public HzResponse<?> db2es(@RequestParam(value = "date", required = false) LocalDateTime date,
                               @RequestParam(value = "minId", required = false) Long minId,
                               @RequestParam(value = "maxId", required = false) Long maxId) {
        List<LawInfo> list = lawContentService.getAll(date, minId, maxId);
        List<List<LawInfo>> lawList = ListUtil.split(list, 10000);
        ClientInterface restClient = bossESStarter.getRestClient();
        lawList.forEach(l -> restClient.addDocuments("law_info", l));
        return HzResponse.success();
    }

    @ApiOperation("智能检索")
    @PostMapping("/api/v1/search")
    public HzResponse<HzPage<LawInfoSearchResponseDTO>> search(@RequestBody LawInfoSearchRequestDTO requestDTO) {
        return HzResponse.success(lawService.search(requestDTO));
    }

    @ApiOperation("智能检索")
    @PostMapping("/api/v2/search")
    public HzResponse<HzPage<LawInfoSearchV2ResponseDTO>> searchV2(@RequestBody LawInfoSearchRequestDTO requestDTO) {
        HzPage<LawInfoSearchV2ResponseDTO> page = lawService.searchV2(requestDTO);
        return HzResponse.success(page);
    }

    @ApiOperation("查询法律法规上位法")
    @GetMapping("/api/v1/getHigherLevelLaw")
    public HzResponse<String> getHigherLevelLaw(@RequestParam("title") String title) {
        return HzResponse.success(higherLevelLawService.getHigherLevelLaw(title));
    }

    @ApiOperation("查询收录的法律法规上位法的标题")
    @GetMapping("/api/v1/getHigherLevelLawTitle")
    public HzResponse<List<String>> getHigherLevelLawTitle() {
        return HzResponse.success(higherLevelLawService.getHigherLevelLawTitle());
    }

    @ApiOperation("法律法规统计")
    @PostMapping("/api/v1/law/statistics")
    public HzResponse<Map<String, Long>> lawStatistics(@RequestBody LawStatisticsRequestDTO requestDTO) {
        return HzResponse.success(lawService.lawStatistics(requestDTO));
    }

    @ApiOperation("法律法规统计")
    @PostMapping("/api/v2/law/statistics")
    public HzResponse<LawStatisticsResponseDTO> lawStatisticsV2(@RequestBody LawStatisticsRequestDTO requestDTO) {
        return HzResponse.success(lawService.lawStatisticsV2(requestDTO));
    }

    @ApiOperation("下载法规文件")
    @GetMapping("/api/v1/law/getLawInfo")
    public HzResponse<LawPageResponseDTO> getLawInfo(@RequestParam("outerId") String outerId) {
        if (outerId.endsWith("=")) {
            outerId = StrUtil.replace(outerId, "=", "%3d");
        }
        return HzResponse.success(lawService.getLawInfo(outerId));
    }


//    @GetMapping("/api/v1/test")
//    public void test() {
//        LawDataUtils.articleSmart();
//    }

    @ApiOperation("法律法规检索（年份、时效性、制定机关）")
    @PostMapping("/api/v1/law/queryLaw")
    public HzResponse<QueryLawResponseDTO> queryLaw(@RequestBody QueryLawRequestDTO requestDTO) {
        return HzResponse.success(lawService.queryLaw(requestDTO));
    }

    @ApiOperation("法规修订内容查询")
    @PostMapping("/api/v1/law/getModifiedContent")
    public HzResponse<ModifiedLawContentResponseDTO> getModifiedContent(@RequestBody QueryLawRequestDTO requestDTO) {
        return HzResponse.success(lawService.getModifiedContent(requestDTO));
    }

    @GetMapping("/api/v1/rebuildLawContent")
    public HzResponse<Void> rebuildLawContent() {
        LawDataUtils.rebuildLawContent();
        return HzResponse.success();
    }

//    @GetMapping("/api/v1/initPreviewUrl")
//    public HzResponse<Void> initPreviewUrl() {
//        lawService.initPreviewUrl();
//        return HzResponse.success();
//    }

    //    @PostMapping("/api/v1/initHigherLevelLaw")
//    public HzResponse<Void> initHigherLevelLaw() {
//        higherLevelLawService.initHigherLevelLaw();
//        return HzResponse.success();
//    }
//
//    @GetMapping("/api/v1/temp")
//    public HzResponse<Void> temp() {
//        LawDataUtils.temp1();
//        LawDataUtils.temp2();
//        return HzResponse.success();
//    }

//    @GetMapping("/api/v1/test")
//    public HzResponse<String> test() {
//        LawContentMapperWrapper mapper = HzSpringUtils.getBean(LawContentMapperWrapper.class);
//        LawContentQuery query = LawContentQuery.query().where.outerId().eq("NDAyOGFiY2M2MTI3Nzc5MzAxNjEyN2Y3NDVjMTM3YzM%3D").end();
//        ClientInterface restClient = bossESStarter.getRestClient();
//        List<LawInfo> list = mapper.findListByQuery(query, LawInfo.class);
//        String result = restClient.addDocuments("law_info", list);
//        return HzResponse.success(result);
//    }
}
