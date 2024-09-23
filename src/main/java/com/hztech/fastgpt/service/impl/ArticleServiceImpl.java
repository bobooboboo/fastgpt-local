package com.hztech.fastgpt.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.fastgpt.ServiceRequests;
import com.hztech.fastgpt.dao.IArticleDao;
import com.hztech.fastgpt.dao.po.ArticleDO;
import com.hztech.fastgpt.dao.wrapper.ArticleQuery;
import com.hztech.fastgpt.entity.PushData;
import com.hztech.fastgpt.service.IArticleService;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import com.hztech.util.HzCollectionUtils;
import com.hztech.util.HzStringUtils;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * IArticleService 服务实现
 *
 * @author HZ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends HzBaseTransactionScriptService<IArticleDao, ArticleDO, Long>
        implements IArticleService {

    private final ServiceRequests serviceRequests;

    /**
     * 用于断句的标点符号
     */
    private static final List<Character> PUNCTUATION = HzCollectionUtils.newArrayList('，', '。', ',');

    @Override
    public Boolean save(Integer from, Integer size) {
        List<ArticleDO> list = ArticleQuery.query()
                .limit(from, size)
                .select.id().title().content().source().end()
                .where.content().ne("").end()
                .orderBy.id().asc().end().to().listEntity();
        if (HzCollectionUtils.isNotEmpty(list)) {
            list.forEach(articleDO -> {
                Map<String, Object> map = serviceRequests.listCollection("66d9561d07e26e2068449b48", 1, 1, null, articleDO.getTitle() + "_" + articleDO.getId().toString());
                JSONObject jsonObject = JSONUtil.parseObj(map);
                Integer total = jsonObject.getByPath("data.total", Integer.class);
                if (total != null && total > 0) {
                    JSONArray jsonArray = jsonObject.getByPath("data.data", JSONArray.class);
                    if (HzCollectionUtils.isNotEmpty(jsonArray)) {
                        JSONObject js = JSONUtil.parseObj(jsonArray.get(0));
                        Integer dataAmount = js.getInt("dataAmount");
                        if (Objects.equals(dataAmount, 0)) {
                            String collectionId = js.getStr("_id");
                            List<PushData> pushDataList = buildPushData(articleDO);
                            serviceRequests.pushData(collectionId, "chunk", null, pushDataList);
                        }
                    }
                } else {
                    List<PushData> pushDataList = buildPushData(articleDO);
                    // 创建文件集合
                    Map<String, Object> resultMap = serviceRequests.createCollection("66d9561d07e26e2068449b48", null, articleDO.getTitle() + "_" + articleDO.getId().toString(), "virtual", null);
                    String collectionId = JSONUtil.parseObj(resultMap).getStr("data");
                    log.info("{}创建文件集合成功，collectionId：{}", articleDO.getId(), collectionId);
                    // 插入新数据
                    serviceRequests.pushData(collectionId, "chunk", null, pushDataList);
                    log.info("插入数据成功");
                }
            });
        }
        log.info("保存新闻执行完毕");
        return true;
    }

    /**
     * 将文本拆分成段落句子
     */
    private static List<String> subSentence(String text, int maxLength) {
        if (text.contains("Evaluation Warning: The document was created with Spire.Doc for JAVA.\r\n")) {
            text = text.replace("Evaluation Warning: The document was created with Spire.Doc for JAVA.\r\n", "");
        }
        int length = text.length();
        if (length <= maxLength) {
            return Collections.singletonList(text);
        }
        List<String> result = new ArrayList<>();
        int currentIndex = 0, cursor = maxLength - 1;
        while (currentIndex < length - 1) {
            String sentence = null;
            for (; cursor > currentIndex; cursor--) {
                if (PUNCTUATION.contains(text.charAt(cursor))) {
                    sentence = text.substring(currentIndex, cursor + 1);
                    currentIndex = cursor + 1;
                    cursor = currentIndex + maxLength - 1;
                    if (cursor >= length) {
                        cursor = length - 1;
                    }
                    break;
                }

            }
            if (sentence == null) {
                sentence = StrUtil.sub(text, currentIndex, currentIndex + maxLength - 1);
                currentIndex += StrUtil.length(sentence);
            }
            if (HzStringUtils.isNotBlank(sentence)) {
                result.add(sentence);
            } else if (StrUtil.length(sentence) > 0) {
                currentIndex += StrUtil.length(sentence);
            }
        }
        return result;
    }

    private List<PushData> buildPushData(ArticleDO articleDO) {
        String articleContent = HtmlUtil.removeHtmlTag(articleDO.getContent(), "img", "strong", "section");
        articleContent = HtmlUtil.removeAllHtmlAttr(articleContent, "td", "table");
        Document document = new Document(new ByteArrayInputStream(articleContent.getBytes()), FileFormat.Html);
        String text = document.getText();
        return subSentence(text, 700).stream().map(content -> {
            PushData pushData = new PushData();
            pushData.setQ(content);
            pushData.setA(articleDO.getSource());
            return pushData;
        }).collect(Collectors.toList());
    }

    public static void main(String[] args) {
//        String content = "<div contentScore=\"1848\"><p><img src=\"http://www.bjrd.gov.cn/xwzx/rdyw/202409/W020240906624707549818.jpg\" width=\"900\" height=\"675\" title=\"市十六届人大教科文卫委员会召开第九次会议2.jpg\" alt=\"市十六届人大教科文卫委员会召开第九次会议2.jpg\" oldsrc=\"W020240906624707549818.jpg\"></p><p><img src=\"http://www.bjrd.gov.cn/xwzx/rdyw/202409/W020240906624707644843.jpg\" width=\"900\" height=\"1200\" title=\"市十六届人大教科文卫委员会召开第九次会议1.jpg\" alt=\"市十六届人大教科文卫委员会召开第九次会议1.jpg\" oldsrc=\"W020240906624707644843.jpg\"></p><p>　　北京人大网讯 9月5日下午，市十六届人大教科文卫委员会召开第九次会议。市人大常委会副主任于军出席会议并讲话，教科文卫委员会主任委员刘玉芳主持会议。教科文卫委员会组成人员、卫生代表小组和监督工作专题调研组部分代表、市卫生健康委有关负责同志、市人大常委会教科文卫办公室全体同志参加会议。<br></p><p>　　教科文卫委员会分党组书记、副主任委员孟繁华带领大家集体学习了市委十三届五次全会精神。会议听取了市卫生健康委关于本市推进医疗服务信息化便民惠民工作情况的汇报和市人大教科文卫委员会对市人民政府关于推进医疗服务信息化便民惠民工作情况报告的意见和建议（讨论稿）和市卫生健康委关于市十六届人大常委会第五次会议对《北京市院前医疗急救服务条例》实施情况审议意见研究处理情况的报告（讨论稿）。</p><p>　　委员代表们认为，医疗服务信息化便民惠民和院前医疗急救与人民群众生活密切相关，本市在推动两项工作时取得了显著成效，建议持续加强医疗服务信息化建设，不断推动院前医疗急救体系均衡发展，满足新时代人民群众对于医疗卫生工作的新期待。</p><p>　　于军同志在讲话中强调，委员会要继续推动学习贯彻党的二十届三中全会精神走深走实，深入推进市委十三届五次全会确定的涉及人大教科文卫领域的主责任务落细落实，接下来要在医疗服务信息化便民惠民监督工作、民办教育促进法实施办法一审以及大运河保护决定执法检查等工作中，统筹谋划、凝聚共识、各方协同，推动新时代首都教科文卫事业高质量发展。（教科文卫办公室）</p></div>\n";
//        content = HtmlUtil.removeHtmlTag(content, "img", "strong");
//        content = HtmlUtil.removeAllHtmlAttr(content, "td", "table");
////        content = HtmlUtil.cleanHtmlTag(content);
//        Document document = new Document(new ByteArrayInputStream(content.getBytes()), FileFormat.Html);
//        String text = document.getText();
//        List<String> strings = subSentence(text, 700);
//        System.out.println(strings);

    }
}
