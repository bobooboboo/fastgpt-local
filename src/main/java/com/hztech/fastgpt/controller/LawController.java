package com.hztech.fastgpt.controller;

import cn.hutool.core.collection.ListUtil;
import com.hztech.fastgpt.model.LawInfo;
import com.hztech.fastgpt.model.dto.request.LawInfoSearchRequestDTO;
import com.hztech.fastgpt.model.dto.request.LawPageRequestDTO;
import com.hztech.fastgpt.model.dto.response.LawInfoSearchResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawInfoSearchV2ResponseDTO;
import com.hztech.fastgpt.model.dto.response.LawPageResponseDTO;
import com.hztech.fastgpt.model.dto.response.TempLawPageResponseDTO;
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

    @GetMapping("/api/v1/law/page")
    public HzResponse<HzPage<TempLawPageResponseDTO>> tempLawPage(LawPageRequestDTO requestDTO) {
        return HzResponse.success(lawService.tempLawPage(requestDTO));
    }

    @ApiIgnore
    @ApiOperation("创建索引")
    @PostMapping("/api/v1/createIndexAndMapping")
    public HzResponse<?> createIndexAndMapping() {
        ClientInterface restClient = bossESStarter.getConfigRestClient("esmapper/createLawInfoIndex.xml");
        String result = restClient.createIndiceMapping("law_info", "createLawInfoIndex");
        return HzResponse.success(result);
    }

    @ApiIgnore
    @ApiOperation("从DB同步数据到ES")
    @GetMapping("/api/v1/db2es")
    public HzResponse<?> db2es(@RequestParam(value = "date", required = false) LocalDateTime date, @RequestParam(value = "minId", required = false) Long minId, @RequestParam(value = "maxId", required = false) Long maxId) {
        List<LawInfo> list = lawContentService.getAll(date, minId, maxId);
        List<List<LawInfo>> lawList = ListUtil.split(list, 10000);
        ClientInterface restClient = bossESStarter.getRestClient();
        lawList.forEach(l -> restClient.addDocuments("law_info", l));
//        String result = restClient.addDocuments("law_info", list);
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
}
