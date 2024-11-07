package com.hztech.fastgpt.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.frameworkset.orm.annotation.ESId;
import com.frameworkset.orm.annotation.ESIndex;
import lombok.Data;

/**
 * @author boboo
 */
@Data
@ESIndex(name = "proposal_info")
public class ProposalInfo {
    private String secondLevelCategory;
    private String deliveryTime;
    private String delegationOfLeadPerson;
    private String title;
    private String submissionTime;
    private String number;
    private String phoneNumber;
    private String modifyTime;
    private String leadPerson;
    private String organizer;
    private String workUnitAndPosition;
    private String supervisionUnit;
    @ESId
    private String id;
    private String firstLevelCategory;
    private String supporters;
    private String cooperationUnit;

    public static ProposalInfo parse(String source) {
        JSONObject jsonObject = JSONUtil.parseObj(source);
        ProposalInfo proposalInfo = new ProposalInfo();
        proposalInfo.setSecondLevelCategory(jsonObject.getStr("二级分类"));
        proposalInfo.setDeliveryTime(StrUtil.emptyToNull(jsonObject.getStr("交办时间")));
        proposalInfo.setDelegationOfLeadPerson(jsonObject.getStr("领衔人所在代表团"));
        proposalInfo.setTitle(jsonObject.getStr("标题"));
        proposalInfo.setSubmissionTime(StrUtil.emptyToNull(jsonObject.getStr("提交时间")));
        proposalInfo.setNumber(jsonObject.getStr("编号"));
        proposalInfo.setPhoneNumber(jsonObject.getStr("手机号码"));
        proposalInfo.setModifyTime(StrUtil.emptyToNull(jsonObject.getStr("modifyTime")));
        proposalInfo.setLeadPerson(jsonObject.getStr("领衔人"));
        proposalInfo.setOrganizer(jsonObject.getStr("主办单位"));
        proposalInfo.setWorkUnitAndPosition(jsonObject.getStr("工作单位及职务"));
        proposalInfo.setSupervisionUnit(jsonObject.getStr("督办单位"));
        proposalInfo.setId(jsonObject.getStr("id"));
        proposalInfo.setFirstLevelCategory(jsonObject.getStr("一级分类"));
        proposalInfo.setSupporters(jsonObject.getStr("附议人"));
        proposalInfo.setCooperationUnit(jsonObject.getStr("协办单位"));
        return proposalInfo;
    }

    public static JSONObject build(ProposalInfo proposalInfo) {
        JSONObject jsonObject = new JSONObject(true);
        jsonObject.set("编号", proposalInfo.getNumber());
        jsonObject.set("领衔人", proposalInfo.getLeadPerson());
        jsonObject.set("领衔人所在代表团", proposalInfo.getDelegationOfLeadPerson());
        jsonObject.set("手机号码", proposalInfo.getPhoneNumber());
        jsonObject.set("工作单位及职务", proposalInfo.getWorkUnitAndPosition());
        jsonObject.set("附议人", proposalInfo.getSupporters());
        jsonObject.set("标题", proposalInfo.getTitle());
        jsonObject.set("提交时间", proposalInfo.getSubmissionTime());
        jsonObject.set("一级分类", proposalInfo.getFirstLevelCategory());
        jsonObject.set("二级分类", proposalInfo.getSecondLevelCategory());
        jsonObject.set("交办时间", proposalInfo.getDeliveryTime());
        jsonObject.set("主办单位", proposalInfo.getOrganizer());
        jsonObject.set("协办单位", proposalInfo.getCooperationUnit());
        jsonObject.set("督办单位", proposalInfo.getSupervisionUnit());
        jsonObject.set("答复时间", "");
        jsonObject.set("办理结果", "");
        jsonObject.set("评价结果", "");
        jsonObject.set("备注", "");
        jsonObject.set("id", proposalInfo.getId());
        return jsonObject;
    }
}