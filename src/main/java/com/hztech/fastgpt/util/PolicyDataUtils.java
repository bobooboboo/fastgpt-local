package com.hztech.fastgpt.util;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.mapper.wrapper.LawContentMapperWrapper;
import com.hztech.fastgpt.model.dto.response.LawDetailResponseDTO;
import com.hztech.fastgpt.model.enums.EnumLawSource;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.service.ILawContentService;
import com.hztech.fastgpt.service.ILawService;
import com.hztech.util.HzCollectionUtils;
import com.hztech.util.HzSpringUtils;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import lombok.Cleanup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PolicyDataUtils
 *
 * @author: boboo
 * @Date: 2024/10/10 11:52
 **/
@Slf4j
public class PolicyDataUtils {

    @Data
    static
    class SearchResponseDTO {
        private CatMap catMap;
    }

    @Data
    static
    class CatMap {
        private CatItem gongwen;
        private CatItem bumenfile;
        private CatItem otherfile;
        private CatItem gongbao;
    }

    @Data
    static
    class CatItem {
        /**
         * 数据总数量
         */
        private Integer totalCount;

        /**
         * 数据列表
         */
        private List<CatItemDetail> listVO;
    }

    @Data
    static
    class CatItemDetail {
        private String id;

        /**
         * 标题
         */
        private String title;

        /**
         * 详情url
         */
        private String url;

        /**
         * 成文时间
         */
        private LocalDateTime ptime;

        /**
         * 发布日期
         */
        private LocalDateTime pubtime;

        /**
         * 发布组织
         */
        private String puborg;
    }

    private static final String BASIC_DATA_URL = "https://sousuo.www.gov.cn/search-gov/data?t=zhengcelibrary&timetype=timeqb&mintime=&maxtime=&sort=score&sortType=1&searchfield=title&subchildtype=&puborg=&pcodeYear=&pcodeNum=&filetype=&p={}&n={}&inpro=&dup=&orpro=&type=gwyzcwjk";

    public static void fetchRemoteData() {
        for (int startPage = 1; startPage < 50; startPage++) {
            @Cleanup
            HttpResponse response = HttpRequest.get(StrUtil.format(BASIC_DATA_URL, 1, 1)).execute();
            String body = response.body();
            CatMap catMap = JSONUtil.parseObj(body).getByPath("searchVO.catMap", CatMap.class);
            if (!BooleanUtil.or(handleCatItem(catMap.getBumenfile()), handleCatItem(catMap.getGongbao()), handleCatItem(catMap.getGongwen()), handleCatItem(catMap.getOtherfile()))) {
                break;
            }
        }
    }

    private static boolean handleCatItem(CatItem catItem) {
        if (HzCollectionUtils.isNotEmpty(catItem.getListVO())) {
            for (CatItemDetail catItemDetail : catItem.getListVO()) {
                List<LawDetailResponseDTO> list = read(catItemDetail.getTitle(), catItemDetail.getUrl());
                if (HzCollectionUtils.isNotEmpty(list)) {
                    // 保存入库
                    LawDO lawStatisticsDO = new LawDO();
//                    lawStatisticsDO.setType(basicData.getLegislationType());
                    lawStatisticsDO.setOuterId(catItemDetail.getId());
                    lawStatisticsDO.setTitle(catItemDetail.getTitle());
                    lawStatisticsDO.setSubject(catItemDetail.getPuborg());
//                    lawStatisticsDO.setEffective(basicData.getExpiry());
                    lawStatisticsDO.setPublish(LocalDateTimeUtil.formatNormal(catItemDetail.getPubtime()));
                    lawStatisticsDO.setDataSource(EnumLawSource.STATE_COUNCIL_POLICY_DOCUMENT_LIBRARY);
//                    lawStatisticsDO.setDocFileUrl(docFileUrl);
//                    lawStatisticsDO.setPdfFileUrl(pdfFileUrl);
                    lawStatisticsDO.setStatus(EnumLawStatus.EFFECTIVE);
                    lawStatisticsDO.setOriginalUrl(catItemDetail.getUrl());

                    boolean saved = HzSpringUtils.getBean(ILawService.class).save(lawStatisticsDO);
                    if (saved) {
                        List<LawContentDO> lawContentList = list.stream().map(lawDetailResponseDTO -> {
                            LawContentDO lawElasticSearchDO = new LawContentDO();
                            lawElasticSearchDO.setPart(lawDetailResponseDTO.getPart());
                            lawElasticSearchDO.setChapter(lawDetailResponseDTO.getChapter());
                            lawElasticSearchDO.setSection(lawDetailResponseDTO.getSection());
                            lawElasticSearchDO.setArticle(lawDetailResponseDTO.getArticle());
                            lawElasticSearchDO.setContentType(lawDetailResponseDTO.getContentType());
//                            lawElasticSearchDO.setDocFileUrl(docFileUrl);
//                            lawElasticSearchDO.setPdfFileUrl(pdfFileUrl);
                            lawElasticSearchDO.setOuterId(catItemDetail.getId());
//                            lawElasticSearchDO.setType(catItemDetail.getLegislationType());
                            lawElasticSearchDO.setStatus(EnumLawStatus.EFFECTIVE);
                            lawElasticSearchDO.setTitle(catItemDetail.getTitle());
                            lawElasticSearchDO.setSubject(lawStatisticsDO.getSubject());
                            lawElasticSearchDO.setEffective(lawStatisticsDO.getEffective());
                            lawElasticSearchDO.setPublish(lawStatisticsDO.getPublish());
                            lawElasticSearchDO.setContent(lawDetailResponseDTO.getContent());
                            lawElasticSearchDO.setDataSource(EnumLawSource.STATE_COUNCIL_POLICY_DOCUMENT_LIBRARY);
                            return lawElasticSearchDO;
                        }).collect(Collectors.toList());
                        HzSpringUtils.getBean(LawContentMapperWrapper.class).insertBatch(lawContentList);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static List<LawDetailResponseDTO> read(String title, String url) {
        String html = HttpUtil.get(url);
        Element element = Jsoup.parse(html).getElementById("UCAP-CONTENT");
        if (element != null) {
            Document document = new Document();
            document.loadFromStream(new ByteArrayInputStream(element.html().getBytes()), FileFormat.Html);
            List<LawDetailResponseDTO> list = LawDataUtils.readFromDocx(document);
            log.info("{}读取到内容：{}", title, JSONUtil.toJsonStr(list));
            return list;
        }
        return Collections.emptyList();
    }

    public static void main(String[] args) {
        fetchRemoteData();
    }
}
