package com.hztech.fastgpt.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.fastgpt.dao.IHigherLevelLawDao;
import com.hztech.fastgpt.dao.po.HigherLevelLawDO;
import com.hztech.fastgpt.dao.wrapper.HigherLevelLawQuery;
import com.hztech.fastgpt.dao.wrapper.LawQuery;
import com.hztech.fastgpt.mapper.wrapper.HigherLevelLawMapperWrapper;
import com.hztech.fastgpt.mapper.wrapper.LawMapperWrapper;
import com.hztech.fastgpt.model.enums.EnumLawStatus;
import com.hztech.fastgpt.service.IHigherLevelLawService;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import com.hztech.util.HzCollectionUtils;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * IHigherLevelLawService 服务实现
 *
 * @author HZ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HigherLevelLawServiceImpl extends HzBaseTransactionScriptService<IHigherLevelLawDao, HigherLevelLawDO, Long>
        implements IHigherLevelLawService {

    private final HigherLevelLawMapperWrapper mapper;

    private final LawMapperWrapper lawMapper;

    @Override
    public String getHigherLevelLaw(String title) {
        HigherLevelLawQuery higherLevelLawQuery = HigherLevelLawQuery.query().select.content().end().where.title().eq(title).end().limit(1);
        return mapper.findFieldByQuery(higherLevelLawQuery);
    }

    @Override
    public void initHigherLevelLaw() {
        LawQuery lawQuery = lawMapper.query().select.title().end().where.and(q -> q.where.title().like("杭州").or.title().like("浙江").end()).status().eq(EnumLawStatus.EFFECTIVE).title().ne("杭州市限制养犬规定").end();
        List<String> titleList = lawMapper.findFieldListByQuery(lawQuery);
        if (HzCollectionUtils.isNotEmpty(titleList)) {
            List<HigherLevelLawDO> higherLevelLawList = titleList.stream().map(title -> {
                HigherLevelLawDO higherLevelLawDO = new HigherLevelLawDO();
                higherLevelLawDO.setTitle(title);
                higherLevelLawDO.setQuestion(StrUtil.format("{}的上位法是什么，并给出相应的依据", title));
                higherLevelLawDO.setPlatform("百炼");
                higherLevelLawDO.setModel("qwen-max-latest");
                JSONObject jsonObject = new JSONObject();
                JSONObject input = new JSONObject();
                input.set("prompt", higherLevelLawDO.getQuestion());
                jsonObject.set("input", input);
                HttpRequest request = HttpRequest.post("https://dashscope.aliyuncs.com/api/v1/apps/3d11b14e76814c6ca54cedcc4f6215ba/completion").body(jsonObject.toString()).bearerAuth("sk-44a1296ab3114549a25b372e7f07d0fd");
                @Cleanup
                HttpResponse response = request.execute();
                String body = response.body();
                log.info("法规{}请求百炼返回：{}", title, body);
                higherLevelLawDO.setContent(JSONUtil.getByPath(JSONUtil.parse(body), "output.text", ""));

                return higherLevelLawDO;
            }).collect(Collectors.toList());
            mapper.insertBatch(higherLevelLawList);
        }
    }
}
