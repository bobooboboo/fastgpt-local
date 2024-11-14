package com.hztech.fastgpt.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.org.atool.fluent.mybatis.If;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.hztech.fastgpt.ServiceRequests;
import com.hztech.fastgpt.dao.ILawDao;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.dao.wrapper.LawContentQuery;
import com.hztech.fastgpt.dao.wrapper.LawContentUpdate;
import com.hztech.fastgpt.dao.wrapper.LawQuery;
import com.hztech.fastgpt.dao.wrapper.LawUpdate;
import com.hztech.fastgpt.mapper.wrapper.LawContentMapperWrapper;
import com.hztech.fastgpt.mapper.wrapper.LawMapperWrapper;
import com.hztech.fastgpt.model.CommonConstants;
import com.hztech.fastgpt.model.LawInfo;
import com.hztech.fastgpt.model.dto.request.*;
import com.hztech.fastgpt.model.dto.response.*;
import com.hztech.fastgpt.model.enums.*;
import com.hztech.fastgpt.service.ILawService;
import com.hztech.model.SpireConfig;
import com.hztech.model.dto.HzPage;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import com.hztech.util.HzCollectionUtils;
import com.hztech.util.HzConvertUtils;
import com.hztech.util.HzStreamUtils;
import com.hztech.util.HzStringUtils;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.documents.XHTMLValidationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ILawService 服务实现
 *
 * @author HZ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawServiceImpl extends HzBaseTransactionScriptService<ILawDao, LawDO, Long>
        implements ILawService {

    private final LawMapperWrapper mapper;

    private final ServiceRequests serviceRequests;

    private final BBossESStarter bossESStarter;

    private final LawContentMapperWrapper lawContentMapperWrapper;

    private final List<EnumLawType> defaultLawTypeList = HzCollectionUtils.newArrayList(EnumLawType.CONSTITUTION, EnumLawType.STATUTE,
            EnumLawType.ADMINISTRATIVE_REGULATIONS, EnumLawType.SUPERVISION_REGULATIONS, EnumLawType.JUDICIAL_INTERPRETATION, EnumLawType.LOCAL_REGULATIONS);

    private final List<EnumLawStatus> modifiedLawStatusList = HzCollectionUtils.newArrayList(EnumLawStatus.MODIFIED, EnumLawStatus.EFFECTIVE);

    private static final Pattern PASS_PATTERN = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日).*(通过|公布|发布)");

    private static final Pattern MODIFIED_PATTERN = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日).*(修订|修正)");

    private static final List<EnumLawSource> LAW_SOURCE_LIST = HzCollectionUtils.newArrayList(EnumLawSource.NATIONAL_LAWS_AND_REGULATIONS_DATABASE,
            EnumLawSource.HANGZHOU_MUNICIPAL_PEOPLE_GOVERNMENT_PORTAL_WEBSITE_LOCAL_REGULATIONS, EnumLawSource.HANGZHOU_MUNICIPAL_PEOPLE_GOVERNMENT_PORTAL_GOVERNMENT_REGULATIONS_DATABASE);

    private static final List<EnumLawSource> LOCAL_REGULATIONS_LAW_SOURCE_LIST = HzCollectionUtils.newArrayList(EnumLawSource.NATIONAL_LAWS_AND_REGULATIONS_DATABASE,
            EnumLawSource.HANGZHOU_MUNICIPAL_PEOPLE_GOVERNMENT_PORTAL_WEBSITE_LOCAL_REGULATIONS);

//    private final ThreadPoolExecutor threadPoolExecutor;

    @Override
    public HzPage<LawPageResponseDTO> lawPage(LawPageRequestDTO requestDTO) {
        LawQuery lawQuery = LawQuery.query().where.title().like(requestDTO.getTitle(), If::notBlank).end();
        return mapper.findPageByQuery(lawQuery, requestDTO, LawPageResponseDTO.class);
    }

//    @Override
//    public HzPage<TempLawPageResponseDTO> tempLawPage(LawPageRequestDTO requestDTO) {
//        HzPage<TempLawPageResponseDTO> page = new HzPage<>();
//        Map<String, Object> resultMap = serviceRequests.listCollection("66ed1eb7526336d2393c9d53", requestDTO.getCurrent().intValue(), requestDTO.getSize().intValue(), "", requestDTO.getTitle());
//        JSONObject jsonObject = JSONUtil.parseObj(resultMap).getJSONObject("data");
//        List<ListCollectionResponseDTO> data = jsonObject.getBeanList("data", ListCollectionResponseDTO.class);
//        page.setTotal(jsonObject.getLong("total", 0L));
//        page.setRows(data.stream().map(responseDTO -> {
//            TempLawPageResponseDTO tempLawPageResponseDTO = new TempLawPageResponseDTO();
//            tempLawPageResponseDTO.setTitle(StrUtil.subBefore(responseDTO.getName(), "_", true));
//            tempLawPageResponseDTO.setUrl("http://192.168.1.12:3000" + JSONUtil.parseObj(serviceRequests.readFile(responseDTO.get_id())).getByPath("data.value", String.class));
//            return tempLawPageResponseDTO;
//        }).collect(Collectors.toList()));
//        return page;
//    }

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
//        if (HzStringUtils.isBlank(requestDTO.getText()) &&
//                ObjectUtil.isEmpty(requestDTO.getTextList()) &&
//                HzStringUtils.isBlank(requestDTO.getCity()) &&
//                HzCollectionUtils.isEmpty(requestDTO.getStatus()) &&
//                HzCollectionUtils.isEmpty(requestDTO.getType())) {
//            return HzPage.empty();
//        }
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
            if (requestDTO.getChapter() != null && (requestDTO.getArticle() == null || requestDTO.getArticle() == 0)) {
                dto.setContent(map.get(responseDTO.getOuterId()));
            } else if (requestDTO.getArticle() != null) {
                dto.setContent(responseDTO.getContent());
            }
            if (BooleanUtil.isTrue(requestDTO.getFullContent())) {
                dto.setContent(fullContentMap.get(responseDTO.getOuterId()));
            }
//            String fileUrl = StrUtil.blankToDefault(responseDTO.getPdfFileUrl(), responseDTO.getDocFileUrl());
            // PDF预览
            String fileUrl = StrUtil.blankToDefault(responseDTO.getPreviewUrl(), StrUtil.blankToDefault(responseDTO.getPdfFileUrl(), responseDTO.getDocFileUrl()));
            if (HzStringUtils.isNotBlank(fileUrl)) {
                dto.setFileUrl("http://hztyjdaiservice.2dmeeting.cn:13808" + fileUrl);
//                dto.setFileUrl("http://192.168.1.13:8080/api/v1/law/downloadFile?outerId=" + responseDTO.getOuterId());
//                dto.setFileUrl("https://hztyjdgateway.2dmee/ting.cn:3150/chat/pb/c/v1/law/downloadFile?outerId=" + responseDTO.getOuterId());
//                dto.setFileUrl("https://hztyjdgateway.2dmeeting.cn:3150/chat/pb/c/v1/law/downloadFile?outerId=" + responseDTO.getOuterId());
            }
            dto.setPublish(responseDTO.getPublish());
            dto.setSubject(responseDTO.getSubject());
            return dto;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Long> lawStatistics(LawStatisticsRequestDTO requestDTO) {
        LawQuery lawQuery = mapper.query()
                .select.type().status().title().end()
                .where
                .type().in(requestDTO.getType(), If::notEmpty)
//                .type().in(CollUtil.defaultIfEmpty(lawTypes, defaultLawTypeList))
//                .status().in(requestDTO.getStatus(), If::notEmpty)
                .subject().like(requestDTO.getSubject(), If::notBlank)
                .dataSource().in(LOCAL_REGULATIONS_LAW_SOURCE_LIST).end();
        if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
            DateTime beginOfYear = DateUtil.parseDateTime(requestDTO.getYear() + "-01-01 00:00:00");
            requestDTO.setPublishBegin(beginOfYear.toString());
            requestDTO.setPublishEnd(DateUtil.endOfYear(beginOfYear).toString());
        }
        if ((HzStringUtils.isNotBlank(requestDTO.getPublishBegin()) && !HzStringUtils.equals("null", requestDTO.getPublishBegin()))
                || (HzStringUtils.isNotBlank(requestDTO.getPublishEnd()) && !HzStringUtils.equals("null", requestDTO.getPublishEnd()))) {
            if (HzStringUtils.isNotBlank(requestDTO.getPublishBegin())) {
                DateTime publishBegin = DateUtil.parse(requestDTO.getPublishBegin());
                lawQuery = lawQuery.where.publish().ge(publishBegin).end();
            }
            if (HzStringUtils.isNotBlank(requestDTO.getPublishEnd())) {
                DateTime publishEnd = DateUtil.parse(requestDTO.getPublishEnd());
                lawQuery = lawQuery.where.publish().le(publishEnd).end();
            }
        }
        if (HzStringUtils.isNotBlank(requestDTO.getCity())) {
            lawQuery = lawQuery.where.subject().startWith(requestDTO.getCity().replace("市", "")).end();
        }

        List<LawDO> list = mapper.findListByQuery(lawQuery);
        Map<EnumLawType, Long> map = list.stream().collect(Collectors.groupingBy(LawDO::getType, Collectors.counting()));
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<EnumLawType, Long> entry : map.entrySet()) {
            EnumLawType enumLawType = entry.getKey();
            if (defaultLawTypeList.contains(enumLawType)) {
                result.put(enumLawType.getDesc(), map.get(enumLawType));
            } else {
                if (result.containsKey("相关文件")) {
                    result.put("相关文件", result.get("相关文件") + map.get(enumLawType));
                } else {
                    result.put("相关文件", map.get(enumLawType));
                }
            }
        }
        if (HzCollectionUtils.isNotEmpty(requestDTO.getStatus())) {
            Map<EnumLawStatus, Long> lawStatusMap = list.stream().collect(Collectors.groupingBy(LawDO::getStatus, Collectors.counting()));
            for (EnumLawStatus lawStatus : requestDTO.getStatus()) {
                result.put(lawStatus.getDesc(), lawStatusMap.getOrDefault(lawStatus, 0L));
            }
        }
        return result;
    }

    @Override
    public QueryLawResponseDTO queryLaw(QueryLawRequestDTO requestDTO) {
        QueryLawResponseDTO responseDTO = new QueryLawResponseDTO();
        if (requestDTO.getStatus() == EnumLawStatus.MODIFIED) {
            LawQuery lawQuery = mapper.query().select.title().min.publish("`minPublish`").end()
                    .where.status().in(modifiedLawStatusList)
                    .and(q -> q.where.title().startWith(requestDTO.getKeyword(), If::notBlank).or.subject().startWith(requestDTO.getKeyword(), If::notBlank).end())
                    .dataSource().in(LAW_SOURCE_LIST).end()
                    .groupBy.title().end().having.count.status().gt(1).end();
            if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
                lawQuery = lawQuery.where.publish().between(DateUtil.parseDateTime(requestDTO.getYear() + "-01-01 00:00:00"), DateUtil.parseDateTime(requestDTO.getYear() + "-12-31 23:59:59")).end();
            }
            // 修订过的法规名称
            List<ModifiedLawResponseDTO> list = mapper.findListByQuery(lawQuery, ModifiedLawResponseDTO.class);
            if (HzCollectionUtils.isNotEmpty(list)) {
                LawQuery query = mapper.query().where.status().eq(EnumLawStatus.MODIFIED).and(q -> q.where.title().startWith(requestDTO.getKeyword(), If::notBlank).or.subject().startWith(requestDTO.getKeyword(), If::notBlank).end()).dataSource().in(LAW_SOURCE_LIST).end();
                responseDTO.setTotal(mapper.countByQuery(query));
                Collections.shuffle(list);
                List<QueryLawResponseDTO.QueryLawInfo> queryLawInfoList = CollUtil.sub(list, 0, 3).stream().map(law -> {
                    QueryLawResponseDTO.QueryLawInfo queryLawInfo = new QueryLawResponseDTO.QueryLawInfo();
                    queryLawInfo.setTitle(law.getTitle());
                    LawContentQuery lawContentQuery = lawContentMapperWrapper.query().select.content().end().where.title().eq(law.getTitle()).end();
                    if (HzStringUtils.isNotBlank(law.getMinPublish())) {
                        lawContentQuery = lawContentQuery.where.publish().ge(law.getMinPublish()).end();
                    } else {
                        lawContentQuery = lawContentQuery.where.publish().notNull().and.publish().ne("").end();
                    }
                    if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
                        lawContentQuery = lawContentQuery.where.publish().between(DateUtil.parseDateTime(requestDTO.getYear() + "-01-01 00:00:00"), DateUtil.parseDateTime(requestDTO.getYear() + "-12-31 23:59:59")).end();
                    }
                    List<String> contents = lawContentMapperWrapper.findFieldListByQuery(lawContentQuery);
                    String content = String.join("\n", contents);
                    queryLawInfo.setLawInfo(ReUtil.getGroup0(MODIFIED_PATTERN, content));
                    return queryLawInfo;
                }).collect(Collectors.toList());
                responseDTO.setLawInfos(queryLawInfoList);
            } else {
                responseDTO.setLawInfos(Collections.emptyList());
                responseDTO.setTotal(0);
            }
        } else {
            if (requestDTO.getStatus() == null) {
                requestDTO.setStatus(EnumLawStatus.EFFECTIVE);
            }
            LawQuery lawQuery = mapper.query().select.outerId().title().end()
                    .where.status().eq(requestDTO.getStatus())
                    .and(q -> q.where.title().like(requestDTO.getKeyword(), If::notBlank).or.subject().like(requestDTO.getKeyword(), If::notBlank).end())
                    .dataSource().in(LAW_SOURCE_LIST).end();
            if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
                lawQuery = lawQuery.where.publish().between(DateUtil.parseDateTime(requestDTO.getYear() + "-01-01 00:00:00"), DateUtil.parseDateTime(requestDTO.getYear() + "-12-31 23:59:59")).end();
            }
            List<LawDO> list = mapper.findListByQuery(lawQuery);
            if (HzCollectionUtils.isNotEmpty(list)) {
                Collections.shuffle(list);
                List<QueryLawResponseDTO.QueryLawInfo> queryLawInfoList = CollUtil.sub(list, 0, 3).stream().map(law -> {
                    QueryLawResponseDTO.QueryLawInfo queryLawInfo = new QueryLawResponseDTO.QueryLawInfo();
                    queryLawInfo.setTitle(law.getTitle());
                    LawContentQuery lawContentQuery = lawContentMapperWrapper.query().select.content().end()
                            .where.outerId().eq(law.getOuterId()).contentType().in(HzCollectionUtils.newArrayList(EnumLawContentType.FOREWORD, EnumLawContentType.ARTICLE, EnumLawContentType.OTHER)).end();
                    List<String> contents = lawContentMapperWrapper.findFieldListByQuery(lawContentQuery);
                    String content = String.join("\n", contents);
                    if (requestDTO.getStatus() == EnumLawStatus.NOT_EFFECTIVE || requestDTO.getStatus() == EnumLawStatus.EFFECTIVE) {
                        // 尚未生效或有效
                        queryLawInfo.setLawInfo(ReUtil.getGroup0(PASS_PATTERN, content));
                    }
                    return queryLawInfo;
                }).collect(Collectors.toList());
                responseDTO.setLawInfos(queryLawInfoList);
                responseDTO.setTotal(list.size());
            } else {
                responseDTO.setLawInfos(Collections.emptyList());
                responseDTO.setTotal(0);
            }
        }
        return responseDTO;
    }

    private final DiffRowGenerator generator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(false)
            .ignoreWhiteSpaces(true)
            .oldTag(f -> "")
            .newTag(f -> "")
            .build();

    @Override
    public ModifiedLawContentResponseDTO getModifiedContent(QueryLawRequestDTO requestDTO) {
        LawQuery lawQuery = mapper.query().select.title().min.publish("`minPublish`").end()
                .where.status().in(modifiedLawStatusList).title().like(requestDTO.getKeyword())
                .dataSource().in(LAW_SOURCE_LIST).end()
                .groupBy.title().end().having.count.status().gt(1).end();
        if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
            lawQuery = lawQuery.where.publish().between(DateUtil.parseDateTime(requestDTO.getYear() + "-01-01 00:00:00"), DateUtil.parseDateTime(requestDTO.getYear() + "-12-31 23:59:59")).end();
        }
        // 修订过的法规名称
        ModifiedLawResponseDTO modifiedLawResponseDTO = mapper.findTopByQuery(lawQuery, ModifiedLawResponseDTO.class);
        if (modifiedLawResponseDTO != null) {
            ModifiedLawContentResponseDTO responseDTO = new ModifiedLawContentResponseDTO();
            LawContentQuery lawContentQuery = lawContentMapperWrapper.query().select.outerId().content().end().where.title().eq(modifiedLawResponseDTO.getTitle()).contentType().eq(EnumLawContentType.ARTICLE).end().orderBy.publish().desc().id().asc().end();
            if (HzStringUtils.isNotBlank(modifiedLawResponseDTO.getMinPublish())) {
                lawContentQuery = lawContentQuery.where.publish().ge(modifiedLawResponseDTO.getMinPublish()).end();
            } else {
                lawContentQuery = lawContentQuery.where.publish().notNull().and.publish().ne("").end();
            }
            if (requestDTO.getYear() != null && requestDTO.getYear() > 0) {
                lawContentQuery = lawContentQuery.where.publish().le(DateUtil.parseDateTime(requestDTO.getYear() + "-12-31 23:59:59")).end();
            }
            List<LawContentDO> list = lawContentMapperWrapper.findListByQuery(lawContentQuery);
            List<String> outerIds = list.stream().map(LawContentDO::getOuterId).distinct().collect(Collectors.toList());
            Map<String, List<String>> map = HzStreamUtils.groupBy(list, LawContentDO::getOuterId, Collectors.mapping(x -> {
                String content = x.getContent().replaceAll("\u200b", "").replaceAll(" {2}", "　");
                while (content.endsWith("\n")) {
                    content = content.substring(0, content.length() - 1);
                }
                return content;
            }, Collectors.toList()));
            responseDTO.setTitle(modifiedLawResponseDTO.getTitle());
            List<DiffRow> diffRows = generator.generateDiffRows(map.get(outerIds.get(1)), map.get(outerIds.get(0)));
            List<ModifiedLawContentResponseDTO.ModifiedLawContent> lawContentList = diffRows.stream()
                    .filter(diffRow -> !diffRow.getTag().equals(DiffRow.Tag.EQUAL) && !HzStringUtils.equals(diffRow.getNewLine(), diffRow.getOldLine())).map(diffRow -> {
                        ModifiedLawContentResponseDTO.ModifiedLawContent modifiedLawContent = new ModifiedLawContentResponseDTO.ModifiedLawContent();
                        modifiedLawContent.setOldContent(diffRow.getOldLine());
                        modifiedLawContent.setNewContent(diffRow.getNewLine());
                        modifiedLawContent.setType(diffRow.getTag().name());
                        return modifiedLawContent;
                    }).collect(Collectors.toList());
            responseDTO.setItems(lawContentList);
            return responseDTO;
        }
        return null;
    }

    @Override
    public LawPageResponseDTO getLawInfo(String outerId) {
        LawQuery lawQuery = mapper.query().select.title().docFileUrl().pdfFileUrl().end().where.outerId().like(outerId).end();
        return mapper.findTopByQuery(lawQuery, LawPageResponseDTO.class);
    }

    @Override
    public LawStatisticsResponseDTO lawStatisticsV2(LawStatisticsRequestDTO requestDTO) {
        Map<String, Long> map = lawStatistics(requestDTO);
        LawStatisticsResponseDTO responseDTO = new LawStatisticsResponseDTO();
        if (HzCollectionUtils.isNotEmpty(map)) {
            String result = map.entrySet().stream().map(entry -> entry.getKey() + entry.getValue() + "部").collect(Collectors.joining("，"));
            responseDTO.setHasData(true);
            responseDTO.setContent(StrUtil.format("共收录{}", result));
        } else {
            responseDTO.setHasData(false);
            responseDTO.setContent(StrUtil.format("{}暂无{}法规的相关信息。", StrUtil.emptyIfNull(requestDTO.getCity()),
                    CollUtil.emptyIfNull(requestDTO.getStatus()).stream().map(EnumLawStatus::getDesc).collect(Collectors.joining("、"))));
        }
        return responseDTO;
    }

    @Override
    public void initPreviewUrl() {
        com.spire.license.LicenseProvider.setLicenseKey(SpireConfig.KEY);
        List<LawDO> lawList = mapper.query().select.id().outerId().title().docFileUrl().end()
                .where.dataSource().notIn(HzCollectionUtils.newArrayList(EnumLawSource.MEASURES_FOR_THE_ESTABLISHMENT_OF_LOCAL_REGULATIONS_IN_HANGZHOU,
                        EnumLawSource.HANGZHOU_MUNICIPAL_PEOPLE_GOVERNMENT_PORTAL_WEBSITE_LOCAL_REGULATIONS))
                .previewUrl().isNull().end().to().listEntity();
        for (LawDO law : lawList) {
//            threadPoolExecutor.submit(() -> {
            try {
                String docFileUrl = law.getDocFileUrl();
                FileFormat fileFormat = docFileUrl.endsWith(".docx") ? FileFormat.Docx : docFileUrl.endsWith(".doc") ? FileFormat.Doc : FileFormat.Html;
                Document document = new Document(new ByteArrayInputStream(HttpUtil.downloadBytes("http://192.168.1.13:8080" + docFileUrl)), fileFormat, XHTMLValidationType.None);
                String fileName = StrUtil.format("/fstore/preview/《{}》（{}）.pdf", law.getTitle(), law.getId());
                document.saveToFile(fileName.replaceAll("/fstore", "/Users/boboo").replaceAll("preview", "preview2"), FileFormat.PDF);
                LawContentUpdate update = lawContentMapperWrapper.updater().where.outerId().eq(law.getOuterId()).end().set.previewUrl().is(fileName).end();
                lawContentMapperWrapper.updateByUpdater(update);
                LawUpdate lawUpdate = mapper.updater().where.id().eq(law.getId()).end().set.previewUrl().is(fileName).end();
                mapper.updateByUpdater(lawUpdate);
                log.info("保存预览文件到：{}", fileName);
            } catch (Exception e) {
                log.error("{}异常：{}", law.getTitle(), ExceptionUtil.stacktraceToString(e, 5000));
            }
//            });
        }
    }

    private LawInfoElasticSearchRequestDTO buildElasticSearchRequestDTO(LawInfoSearchRequestDTO requestDTO) {
        LawInfoElasticSearchRequestDTO lawInfoElasticSearchRequestDTO;
        if (requestDTO.getCurrent() == null || requestDTO.getSize() == null) {
            lawInfoElasticSearchRequestDTO = new LawInfoElasticSearchRequestDTO();
        } else {
            lawInfoElasticSearchRequestDTO = new LawInfoElasticSearchRequestDTO((requestDTO.getCurrent() - 1) * requestDTO.getSize(), requestDTO.getSize());
        }
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
                String array = ReUtil.get(CommonConstants.JSON_ARRAY_PATTERN, (String) textList, 0);
                if (HzStringUtils.isNotBlank(array)) {
                    list = JSONUtil.parseArray(array).toList(String.class);
                }
            } else if (textList instanceof List) {
                list = Convert.toList(String.class, textList);
            }
            for (int i = 0; i < list.size(); i++) {
                String text = list.get(i);
                if (requestDTO.getSearchMode() == null || requestDTO.getSearchMode() == EnumLawSearchMode.INTELLIGENT) {
                    LawInfoElasticSearchRequestDTO.Should titleShould = new LawInfoElasticSearchRequestDTO.Should("match");
                    JSONObject jsonObject = new JSONObject();
                    if (i == 0) {
                        jsonObject.set("boost", 5);
                    }
                    jsonObject.set("query", text);
                    titleShould.set("title", jsonObject);
                    lawInfoElasticSearchRequestDTO.should(titleShould);
                } else if (requestDTO.getSearchMode() == EnumLawSearchMode.ACCURATE) {
                    LawInfoElasticSearchRequestDTO.Should titleShould = new LawInfoElasticSearchRequestDTO.Should("match_phrase");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.set("query", text);
                    if (i == 0) {
                        jsonObject.set("boost", 5);
                    }
                    jsonObject.set("query", text);
                    titleShould.set("title", jsonObject);
                    lawInfoElasticSearchRequestDTO.should(titleShould);
//                    LawInfoElasticSearchRequestDTO.Should contentShould = new LawInfoElasticSearchRequestDTO.Should("match_phrase");
//                    JSONObject json = new JSONObject();
//                    json.set("query", text);
//                    contentShould.set("content", json);
//                    lawInfoElasticSearchRequestDTO.should(contentShould);
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
        if (HzStringUtils.isNotBlank(requestDTO.getCity())) {
            LawInfoElasticSearchRequestDTO.Must match = new LawInfoElasticSearchRequestDTO.Must("match_phrase");
            match.set("subject", requestDTO.getCity().replace("市", ""));
            lawInfoElasticSearchRequestDTO.must(match);
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
        responseDTO.setId(lawInfo.getId());
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
        responseDTO.setPreviewUrl(lawInfo.getPreviewUrl());
        responseDTO.setSubject(lawInfo.getSubject());
        return responseDTO;
    }
}
