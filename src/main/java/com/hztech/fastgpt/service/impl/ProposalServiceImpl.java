package com.hztech.fastgpt.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.fastgpt.model.CommonConstants;
import com.hztech.fastgpt.model.ProposalInfo;
import com.hztech.fastgpt.model.dto.request.ProposalInfoSearchRequestDTO;
import com.hztech.fastgpt.service.IProposalService;
import com.hztech.util.HzCollectionUtils;
import com.hztech.util.HzStringUtils;
import lombok.RequiredArgsConstructor;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProposalServiceImpl
 *
 * @author: boboo
 * @Date: 2024/10/14 17:33
 **/
@Service
@RequiredArgsConstructor
public class ProposalServiceImpl implements IProposalService {

    private final BBossESStarter bossESStarter;

    @Override
    public Object search(ProposalInfoSearchRequestDTO requestDTO) {
        ClientInterface restClient = bossESStarter.getRestClient();
        String sql = "SELECT * FROM proposal_info";
        String condition = buildQueryCondition(requestDTO);
        if (HzStringUtils.isNotBlank(condition)) {
            sql = sql + condition;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("query", sql);
        List<ProposalInfo> list = restClient.sql(ProposalInfo.class, jsonObject.toString());
        JSONObject result = new JSONObject();
        if (HzCollectionUtils.isNotEmpty(list)) {
            result.set("code", 100);
            result.set("data", list.stream().map(ProposalInfo::build).collect(Collectors.toList()));
        } else {
            result.set("code", 200);
            result.set("data", Collections.emptyList());
        }
        return result;
    }

    private String buildQueryCondition(ProposalInfoSearchRequestDTO requestDTO) {
        StrBuilder builder = StrBuilder.create();
        buildQueryCondition(builder, requestDTO.getLeader(), "leadPerson");
        buildQueryCondition(builder, requestDTO.getDelegation(), "delegationOfLeadPerson");
        buildQueryCondition(builder, requestDTO.getTitle(), "title");
        buildQueryCondition(builder, requestDTO.getFirstLevelCategory(), "firstLevelCategory");
        buildQueryCondition(builder, requestDTO.getOrganizer(), "organizer");
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
            buildQueryCondition(builder, list, "title");
        }
        return builder.toString();
    }

    private void buildQueryCondition(StrBuilder builder, String conditionValue, String field) {
        if (HzStringUtils.isNotBlank(conditionValue)) {
            if (builder.isEmpty()) {
                builder.append(" WHERE ");
            }
            if (!" WHERE ".equals(builder.toString())) {
                builder.append(" AND ");
            }
            builder.append("MATCH(").append(field).append(", '").append(conditionValue).append("')");
        }
    }

    private void buildQueryCondition(StrBuilder builder, List<String> conditionValues, String field) {
        if (HzCollectionUtils.isNotEmpty(conditionValues)) {
            if (builder.isEmpty()) {
                builder.append(" WHERE ");
            }
            if (!" WHERE ".equals(builder.toString())) {
                builder.append(" AND ");
            }
            builder.append("(");
            for (int i = 0, size = conditionValues.size(); i < size; i++) {
                String conditionValue = conditionValues.get(i);
                builder.append("MATCH(").append(field).append(", '").append(conditionValue).append("')");
                if (i != size - 1) {
                    builder.append(" OR ");
                }
            }
            builder.append(")");
        }
    }

}
