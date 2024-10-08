package com.hztech.fastgpt.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.org.atool.fluent.mybatis.If;
import com.hztech.fastgpt.ServiceRequests;
import com.hztech.fastgpt.dao.ILawDao;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.dao.wrapper.LawContentQuery;
import com.hztech.fastgpt.dao.wrapper.LawQuery;
import com.hztech.fastgpt.mapper.wrapper.LawContentMapperWrapper;
import com.hztech.fastgpt.mapper.wrapper.LawMapperWrapper;
import com.hztech.fastgpt.model.LawInfo;
import com.hztech.fastgpt.model.dto.request.LawInfoElasticSearchRequestDTO;
import com.hztech.fastgpt.model.dto.request.LawInfoSearchRequestDTO;
import com.hztech.fastgpt.model.dto.request.LawPageRequestDTO;
import com.hztech.fastgpt.model.dto.response.*;
import com.hztech.fastgpt.model.enums.*;
import com.hztech.fastgpt.service.ILawService;
import com.hztech.model.dto.HzPage;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import com.hztech.util.HzCollectionUtils;
import com.hztech.util.HzConvertUtils;
import com.hztech.util.HzStringUtils;
import lombok.RequiredArgsConstructor;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ILawService 服务实现
 *
 * @author HZ
 */
@Service
@RequiredArgsConstructor
public class LawServiceImpl extends HzBaseTransactionScriptService<ILawDao, LawDO, Long>
        implements ILawService {

    private final LawMapperWrapper mapper;

    private final ServiceRequests serviceRequests;

    private final BBossESStarter bossESStarter;

    private final LawContentMapperWrapper lawContentMapperWrapper;

    @Override
    public HzPage<LawPageResponseDTO> lawPage(LawPageRequestDTO requestDTO) {
        LawQuery lawQuery = LawQuery.query().where.title().like(requestDTO.getTitle(), If::notBlank).end();
        return mapper.findPageByQuery(lawQuery, requestDTO, LawPageResponseDTO.class);
    }

    @Override
    public HzPage<TempLawPageResponseDTO> tempLawPage(LawPageRequestDTO requestDTO) {
        HzPage<TempLawPageResponseDTO> page = new HzPage<>();
        Map<String, Object> resultMap = serviceRequests.listCollection("66ed1eb7526336d2393c9d53", requestDTO.getCurrent().intValue(), requestDTO.getSize().intValue(), "", requestDTO.getTitle());
        JSONObject jsonObject = JSONUtil.parseObj(resultMap).getJSONObject("data");
        List<ListCollectionResponseDTO> data = jsonObject.getBeanList("data", ListCollectionResponseDTO.class);
        page.setTotal(jsonObject.getLong("total", 0L));
        page.setRows(data.stream().map(responseDTO -> {
            TempLawPageResponseDTO tempLawPageResponseDTO = new TempLawPageResponseDTO();
            tempLawPageResponseDTO.setTitle(StrUtil.subBefore(responseDTO.getName(), "_", true));
            tempLawPageResponseDTO.setUrl("http://192.168.1.12:3000" + JSONUtil.parseObj(serviceRequests.readFile(responseDTO.get_id())).getByPath("data.value", String.class));
            return tempLawPageResponseDTO;
        }).collect(Collectors.toList()));
        return page;
    }

    @Override
    public boolean save(LawDO lawDO) {
        LawQuery lawQuery = LawQuery.query().where.outerId().eq(lawDO.getOuterId()).end();
        if (!mapper.existsByQuery(lawQuery)) {
            return insertSelective(lawDO);
        }
        return false;
    }

    @Override
    public HzPage<LawInfoSearchResponseDTO> search(LawInfoSearchRequestDTO requestDTO) {
        ClientInterface restClient = bossESStarter.getRestClient();
        String body = buildElasticSearchRequestDTO(requestDTO).toString();
        ESDatas<LawInfo> esDatas = restClient.searchList("/law_info/_search", body, LawInfo.class);
        List<LawInfo> list = CollUtil.emptyIfNull(esDatas.getDatas());
        HzPage<LawInfoSearchResponseDTO> result = new HzPage<>();
        result.setRows(list.stream().map(this::buildLawInfoSearchResponseDTO).collect(Collectors.toList()));
        result.setTotal(HzConvertUtils.toLong(esDatas.getAggregations().get("lawInfoCount").get("value")));
        return result;
    }

    @Override
    public HzPage<LawInfoSearchV2ResponseDTO> searchV2(LawInfoSearchRequestDTO requestDTO) {
        HzPage<LawInfoSearchResponseDTO> page = search(requestDTO);
        HzPage<LawInfoSearchV2ResponseDTO> result = new HzPage<>();
        result.setTotal(page.getTotal());
        Map<String, String> map = new HashMap<>();
        if (requestDTO.getChapter() != null && HzCollectionUtils.isNotEmpty(page.getRows())) {
            List<String> outerIds = page.getRows().stream().map(LawInfoSearchResponseDTO::getOuterId).collect(Collectors.toList());
            LawContentQuery lawContentQuery = lawContentMapperWrapper.query().select.outerId().content().end().where.outerId().in(outerIds).chapter().eq(requestDTO.getChapter()).end();
            List<LawContentResponseDTO> list = lawContentMapperWrapper.findListByQuery(lawContentQuery, LawContentResponseDTO.class);
            map.putAll(list.stream().collect(Collectors.groupingBy(LawContentResponseDTO::getOuterId, Collectors.mapping(LawContentResponseDTO::getContent, Collectors.joining("\n")))));
        }
        Map<String, String> fullContentMap = new HashMap<>();
        if (BooleanUtil.isTrue(requestDTO.getFullContent()) && HzCollectionUtils.isNotEmpty(page.getRows())) {
            List<String> outerIds = page.getRows().stream().map(LawInfoSearchResponseDTO::getOuterId).collect(Collectors.toList());
            LawContentQuery lawContentQuery = lawContentMapperWrapper.query().select.outerId().content().end().where.outerId().in(outerIds).end();
            List<LawContentResponseDTO> list = lawContentMapperWrapper.findListByQuery(lawContentQuery, LawContentResponseDTO.class);
            fullContentMap.putAll(list.stream().collect(Collectors.groupingBy(LawContentResponseDTO::getOuterId, Collectors.mapping(LawContentResponseDTO::getContent, Collectors.joining("\n")))));
        }
        result.setRows(page.getRows().stream().map(responseDTO -> {
            LawInfoSearchV2ResponseDTO dto = new LawInfoSearchV2ResponseDTO();
            dto.setTitle(responseDTO.getTitle());
            if (requestDTO.getChapter() != null && requestDTO.getArticle() == null) {
                dto.setContent(map.get(responseDTO.getOuterId()));
            } else if (requestDTO.getArticle() != null) {
                dto.setContent(responseDTO.getContent());
            }
            if (BooleanUtil.isTrue(requestDTO.getFullContent())) {
                dto.setContent(fullContentMap.get(responseDTO.getOuterId()));
            }
            String fileUrl = StrUtil.blankToDefault(responseDTO.getPdfFileUrl(), responseDTO.getDocFileUrl());
            if (HzStringUtils.isNotBlank(fileUrl)) {
                dto.setFileUrl("http://192.168.1.13:8080" + fileUrl);
            }
            return dto;
        }).collect(Collectors.toList()));
        return result;
    }

    private LawInfoElasticSearchRequestDTO buildElasticSearchRequestDTO(LawInfoSearchRequestDTO requestDTO) {
        LawInfoElasticSearchRequestDTO lawInfoElasticSearchRequestDTO =
                new LawInfoElasticSearchRequestDTO((requestDTO.getCurrent() - 1) * requestDTO.getSize(), requestDTO.getSize());
        if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
            DateTime beginOfYear = DateUtil.parseDateTime(requestDTO.getYear() + "-01-01 00:00:00");
            requestDTO.setPublishBegin(beginOfYear.toString());
            requestDTO.setPublishEnd(DateUtil.endOfYear(beginOfYear).toString());
        }
        if (HzCollectionUtils.isNotEmpty(requestDTO.getType())) {
            LawInfoElasticSearchRequestDTO.Must terms = new LawInfoElasticSearchRequestDTO.Must("terms");
            terms.set("type", requestDTO.getType().stream().map(EnumLawType::getValue).collect(Collectors.toList()));
            lawInfoElasticSearchRequestDTO.must(terms);
        }
        if (HzCollectionUtils.isNotEmpty(requestDTO.getStatus())) {
            LawInfoElasticSearchRequestDTO.Must terms = new LawInfoElasticSearchRequestDTO.Must("terms");
            terms.set("status", requestDTO.getStatus().stream().map(EnumLawStatus::getValue).collect(Collectors.toList()));
            lawInfoElasticSearchRequestDTO.must(terms);
        }
        if (requestDTO.getPart() != null && requestDTO.getPart() > 0) {
            LawInfoElasticSearchRequestDTO.Must term = new LawInfoElasticSearchRequestDTO.Must("term");
            term.set("part", requestDTO.getPart());
            lawInfoElasticSearchRequestDTO.must(term);
        }
        if (requestDTO.getChapter() != null && requestDTO.getChapter() > 0) {
            LawInfoElasticSearchRequestDTO.Must term = new LawInfoElasticSearchRequestDTO.Must("term");
            term.set("chapter", requestDTO.getChapter());
            lawInfoElasticSearchRequestDTO.must(term);
        }
        if (requestDTO.getSection() != null && requestDTO.getSection() > 0) {
            LawInfoElasticSearchRequestDTO.Must term = new LawInfoElasticSearchRequestDTO.Must("term");
            term.set("section", requestDTO.getSection());
            lawInfoElasticSearchRequestDTO.must(term);
        }
        if (requestDTO.getArticle() != null && requestDTO.getArticle() > 0) {
            LawInfoElasticSearchRequestDTO.Must term = new LawInfoElasticSearchRequestDTO.Must("term");
            term.set("article", requestDTO.getArticle());
            lawInfoElasticSearchRequestDTO.must(term);
        }
        if ((HzStringUtils.isNotBlank(requestDTO.getPublishBegin()) && !HzStringUtils.equals("null", requestDTO.getPublishBegin()))
                || (HzStringUtils.isNotBlank(requestDTO.getPublishEnd()) && !HzStringUtils.equals("null", requestDTO.getPublishEnd()))) {
            LawInfoElasticSearchRequestDTO.Must range = new LawInfoElasticSearchRequestDTO.Must("range");
            JSONObject rangeValue = new JSONObject(JSONConfig.create().setIgnoreNullValue(true));
            if (HzStringUtils.isNotBlank(requestDTO.getPublishBegin())) {
                DateTime publishBegin = DateUtil.parse(requestDTO.getPublishBegin());
                rangeValue.set("gte", DateUtil.beginOfDay(publishBegin).toString());
            }
            if (HzStringUtils.isNotBlank(requestDTO.getPublishEnd())) {
                DateTime publishEnd = DateUtil.parse(requestDTO.getPublishEnd());
                rangeValue.set("lte", DateUtil.endOfDay(publishEnd).toString());
            }
            range.set("publish", rangeValue);
            lawInfoElasticSearchRequestDTO.must(range);
        }
        if (HzStringUtils.isNotBlank(requestDTO.getSubject())) {
            LawInfoElasticSearchRequestDTO.Must match = new LawInfoElasticSearchRequestDTO.Must("match_phrase");
            match.set("subject", requestDTO.getSubject());
            lawInfoElasticSearchRequestDTO.must(match);
        }
        if (!StrUtil.isAllBlank(requestDTO.getHighlightPreTag(), requestDTO.getHighlightPostTag())) {
            lawInfoElasticSearchRequestDTO.enableHighlight(requestDTO.getHighlightPreTag(), requestDTO.getHighlightPostTag());
        }
        if (HzStringUtils.isNotBlank(requestDTO.getText())) {
            if (requestDTO.getSearchMode() == null || requestDTO.getSearchMode() == EnumLawSearchMode.INTELLIGENT) {
                if (requestDTO.getMode() == EnumLawMode.STRIP) {
                    LawInfoElasticSearchRequestDTO.Should titleShould = new LawInfoElasticSearchRequestDTO.Should("match");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.set("query", requestDTO.getText());
                    titleShould.set("title", jsonObject);
                    lawInfoElasticSearchRequestDTO.should(titleShould);

                    LawInfoElasticSearchRequestDTO.Should contentShould = new LawInfoElasticSearchRequestDTO.Should("match_phrase");
                    contentShould.set("content", requestDTO.getText());
                    lawInfoElasticSearchRequestDTO.should(contentShould);
                    lawInfoElasticSearchRequestDTO.enableHighlight();
                } else {
                    // 搜索类型
                    if (requestDTO.getSearchType() == EnumLawSearchType.FULL) {
                        LawInfoElasticSearchRequestDTO.Must match = new LawInfoElasticSearchRequestDTO.Must("multi_match");
                        match.set("query", requestDTO.getText());
                        match.set("fields", HzCollectionUtils.newArrayList("title", "content"));
                        lawInfoElasticSearchRequestDTO.must(match);
                    } else if (requestDTO.getSearchType() == EnumLawSearchType.TITLE) {
                        LawInfoElasticSearchRequestDTO.Must match = new LawInfoElasticSearchRequestDTO.Must("match");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.set("query", requestDTO.getText());
                        match.set("title", jsonObject);
                        lawInfoElasticSearchRequestDTO.must(match);
                    } else if (requestDTO.getSearchType() == EnumLawSearchType.CONTENT) {
                        LawInfoElasticSearchRequestDTO.Must match = new LawInfoElasticSearchRequestDTO.Must("match");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.set("query", requestDTO.getText());
                        match.set("content", jsonObject);
                        lawInfoElasticSearchRequestDTO.must(match);
                    }
                }
            } else if (requestDTO.getSearchMode() == EnumLawSearchMode.ACCURATE) {
                if (requestDTO.getMode() == EnumLawMode.STRIP) {
                    LawInfoElasticSearchRequestDTO.Should titleShould = new LawInfoElasticSearchRequestDTO.Should("match_phrase");
                    titleShould.set("title", requestDTO.getText());
                    lawInfoElasticSearchRequestDTO.should(titleShould);

                    LawInfoElasticSearchRequestDTO.Should contentShould = new LawInfoElasticSearchRequestDTO.Should("match_phrase");
                    contentShould.set("content", requestDTO.getText());
                    lawInfoElasticSearchRequestDTO.should(contentShould);
                    lawInfoElasticSearchRequestDTO.enableHighlight();
                } else {
                    LawInfoElasticSearchRequestDTO.Must match = new LawInfoElasticSearchRequestDTO.Must("match_phrase");
                    match.set("title", requestDTO.getText());
                    lawInfoElasticSearchRequestDTO.must(match);
                }
            }
        }
        Object textList = requestDTO.getTextList();
        if (ObjectUtil.isNotEmpty(textList)) {
            // 领域搜索
            List<String> list = new ArrayList<>();
            if (textList instanceof String) {
                list = JSONUtil.parseArray(textList).toList(String.class);
            } else if (textList instanceof List) {
                list = Convert.toList(String.class, textList);
            }
            for (String text : list) {
                if (requestDTO.getSearchMode() == null || requestDTO.getSearchMode() == EnumLawSearchMode.INTELLIGENT) {
                    LawInfoElasticSearchRequestDTO.Should titleShould = new LawInfoElasticSearchRequestDTO.Should("match");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.set("query", text);
                    titleShould.set("title", jsonObject);
                    lawInfoElasticSearchRequestDTO.should(titleShould);
                } else if (requestDTO.getSearchMode() == EnumLawSearchMode.ACCURATE) {
                    LawInfoElasticSearchRequestDTO.Should titleShould = new LawInfoElasticSearchRequestDTO.Should("match_phrase");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.set("query", text);
                    titleShould.set("title", jsonObject);
                    lawInfoElasticSearchRequestDTO.should(titleShould);
                }
            }
            requestDTO.setSize(list.size() * 2L);
        }
        // 数据源
        if (HzCollectionUtils.isNotEmpty(requestDTO.getDataSource())) {
            LawInfoElasticSearchRequestDTO.Must terms = new LawInfoElasticSearchRequestDTO.Must("terms");
            terms.set("dataSource", requestDTO.getDataSource().stream().map(EnumLawSource::getValue).collect(Collectors.toList()));
            lawInfoElasticSearchRequestDTO.must(terms);
        }
        // 排序
        if (requestDTO.getSortType() == EnumLawSortType.PUBLISH_ASC) {
            lawInfoElasticSearchRequestDTO.sort("publish", "asc");
        } else if (requestDTO.getSortType() == EnumLawSortType.PUBLISH_DESC) {
            lawInfoElasticSearchRequestDTO.sort("publish", "desc");
        } else if (requestDTO.getSortType() == EnumLawSortType.IMPLEMENTATION_ASC) {
            lawInfoElasticSearchRequestDTO.sort("effective", "asc");
        } else if (requestDTO.getSortType() == EnumLawSortType.IMPLEMENTATION_DESC) {
            lawInfoElasticSearchRequestDTO.sort("effective", "desc");
        }
        return lawInfoElasticSearchRequestDTO;
    }

    /**
     * 构建检索响应对象
     */
    private LawInfoSearchResponseDTO buildLawInfoSearchResponseDTO(LawInfo lawInfo) {
        LawInfoSearchResponseDTO responseDTO = new LawInfoSearchResponseDTO();
        responseDTO.setOuterId(lawInfo.getOuterId());
        responseDTO.setType(lawInfo.getType().getDesc());
        responseDTO.setStatus(lawInfo.getStatus().getDesc());
        responseDTO.setTitle(lawInfo.getTitle());
        responseDTO.setEffective(lawInfo.getEffective());
        responseDTO.setPublish(lawInfo.getPublish());
        if (HzCollectionUtils.isNotEmpty(lawInfo.getHighlights())) {
            responseDTO.setContent(HzStringUtils.toStringOrNull(CollUtil.getFirst(lawInfo.getHighlights().get("content"))));
        } else {
            responseDTO.setContent(lawInfo.getContent());
        }
        responseDTO.setDocFileUrl(lawInfo.getDocFileUrl());
        responseDTO.setPdfFileUrl(lawInfo.getPdfFileUrl());
        return responseDTO;
    }
}
