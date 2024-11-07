package com.hztech.fastgpt.util;

/**
 * DiffUtils
 *
 * @author: boboo
 * @Date: 2024/10/25 11:26
 **/
public class DiffUtils {

    public static void main(String[] args) {
//        DiffRowGenerator generator = DiffRowGenerator.create()
//                .showInlineDiffs(false)
//                .inlineDiffByWord(false)
//                .build();
//        List<DiffRow> rows = generator.generateDiffRows(
//                Arrays.asList("第四十八条　执法部门及其工作人员违反规定检查、收费、罚款，玩忽职守、徇私舞弊、滥用职权的，根据情节轻重，给予批评教育、行政处分；构成犯罪的，由司法机关依法追究刑事责任。",
//                        "第四十九条　违反本条例规定，损坏航道、航道设施和其他水上交通设施的，应当依法承担民事责任。",
//                        "第五十条　本条例自公布之日起施行。"),
//                Arrays.asList("第四十八条　执法部门及其工作人员违反规定检查、收费、罚款，玩忽职守、徇私舞弊、滥用职权的，根据情节轻重，给予批评教育、行政处分；构成犯罪的，由司法机关依法追究刑事责任。",
//                        "第四十九条　违反本条例规定，损坏航道、航道设施和其他水上交通设施的，应当依法承担民事责任。",
//                        "第五十条　当事人对行政处罚决定不服的，可依法申请行政复议或者直接向人民法院起诉。当事人逾期不申请复议、也不向人民法院起诉、又不履行处罚决定的，由作出处罚决定的机关申请人民法院强制执行。",
//                        "第五十一条　本条例自公布之日起施行。"));
//        System.out.println("|original|new|");
//        System.out.println("|--------|---|");
//        for (DiffRow row : rows) {
//            System.out.println("|" + row.getOldLine() + "|" + row.getNewLine() + "|" + "\t" + row.getTag().name());
//        }

//        System.out.println(com.github.difflib.DiffUtils.diff(Arrays.asList("第四十八条　执法部门及其工作人员违反规定检查、收费、罚款，玩忽职守、徇私舞弊、滥用职权的，根据情节轻重，给予批评教育、行政处分；构成犯罪的，由司法机关依法追究刑事责任。",
//                        "第四十九条　违反本条例规定，损坏航道、航道设施和其他水上交通设施的，应当依法承担民事责任。",
//                        "第五十条　本条例自公布之日起施行。"),
//                Arrays.asList("第四十八条　执法部门及其工作人员违反规定检查、收费、罚款，玩忽职守、徇私舞弊、滥用职权的，根据情节轻重，给予批评教育、行政处分；构成犯罪的，由司法机关依法追究刑事责任。",
//                        "第四十九条　违反本条例规定，损坏航道、航道设施和其他水上交通设施的，应当依法承担民事责任。",
//                        "第五十条　当事人对行政处罚决定不服的，可依法申请行政复议或者直接向人民法院起诉。当事人逾期不申请复议、也不向人民法院起诉、又不履行处罚决定的，由作出处罚决定的机关申请人民法院强制执行。",
//                        "第五十一条　本条例自公布之日起施行。")));

        boolean equals = "  ".equals("　");
        System.out.println(equals);
    }
}
