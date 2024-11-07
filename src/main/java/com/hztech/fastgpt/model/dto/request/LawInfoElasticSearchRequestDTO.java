package com.hztech.fastgpt.model.dto.request;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hztech.util.HzCollectionUtils;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ES查询对象
 *
 * @author: boboo
 * @Date: 2023/10/8 10:40
 **/
@Data
public class LawInfoElasticSearchRequestDTO {
    private Query query;

    private List<Sort> sort;

    private Long from;

    private Long size;

    private Highlight highlight;

    public LawInfoElasticSearchRequestDTO() {
        this.query = new Query();
        query.bool = new Bool();
        query.bool.must = new ArrayList<>();
        query.bool.should = new ArrayList<>();
        this.sort = new ArrayList<>();
    }

    public LawInfoElasticSearchRequestDTO(Long from, Long size) {
        this.query = new Query();
        query.bool = new Bool();
        query.bool.must = new ArrayList<>();
        query.bool.should = new ArrayList<>();
        this.sort = new ArrayList<>();
        this.from = from;
        this.size = size;
    }

    @Data
    public static class Query {
        private Bool bool;

        protected JSONObject build() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("bool" , bool.build());
            return jsonObject;
        }
    }

    @Data
    public static class Bool {
        private List<Must> must;

        private List<Should> should;

        protected JSONObject build() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("must" , must.stream().map(Must::build).collect(Collectors.toList()));
            if (HzCollectionUtils.isNotEmpty(should)) {
                jsonObject.set("should" , should.stream().map(Should::build).collect(Collectors.toList()));
                jsonObject.set("minimum_should_match" , "1");
            }
            return jsonObject;
        }
    }

    @Data
    public static class Should {
        private JSONObject jsonObject;

        private JSONObject value;

        public Should(String key) {
            this.jsonObject = new JSONObject();
            this.value = new JSONObject();
            this.jsonObject.set(key, value);
        }

        public void set(String key, Object value) {
            this.value.set(key, value);
        }

        protected JSONObject build() {
            return jsonObject;
        }
    }

    @Data
    public static class Must {
        private JSONObject jsonObject;
        private JSONObject value;

        public Must(String key) {
            this.jsonObject = new JSONObject();
            this.value = new JSONObject();
            this.jsonObject.set(key, value);
        }

        public void set(String key, Object value) {
            this.value.set(key, value);
        }

        protected JSONObject build() {
            return jsonObject;
        }
    }

    @Getter
    public static class Sort {
        private final Map<String, Map<String, String>> map;

        public Sort(Map<String, Map<String, String>> map) {
            this.map = map;
        }
    }

    public void must(Must must) {
        query.bool.must.add(must);
    }

    public void should(Should should) {
        query.bool.should.add(should);
    }

    public void sort(String field, String operation) {
        Map<String, Map<String, String>> map = new HashMap<>(4);
        Map<String, String> innerMap = new HashMap<>(4);
        innerMap.put("order" , operation);
        map.put(field, innerMap);
        this.sort.add(new Sort(map));
    }

    @Data
    public static class Highlight {
        private List<String> pre_tags = HzCollectionUtils.newArrayList("<span color='red'>");

        private List<String> post_tags = HzCollectionUtils.newArrayList("</span>");

        private JSONObject fields;
    }

    public void enableHighlight() {
        this.highlight = new Highlight();
        highlight.fields = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("type" , "plain");
        highlight.fields.set("content" , jsonObject);
    }

    public void enableHighlight(String preTags, String postTags) {
        this.highlight = new Highlight();
        highlight.pre_tags = HzCollectionUtils.newArrayList(preTags);
        highlight.post_tags = HzCollectionUtils.newArrayList(postTags);
        highlight.fields = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("type" , "plain");
        highlight.fields.set("content" , jsonObject);
    }

    private final JSONObject aggs = JSONUtil.parseObj("{\"lawInfoCount\":{\"cardinality\":{\"field\":\"outerId\"}}}");

    private final JSONObject collapse = JSONUtil.parseObj("{\"field\":\"outerId\"}");

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("query" , query.build());
        if (HzCollectionUtils.isNotEmpty(sort)) {
            jsonObject.set("sort" , sort.stream().map(Sort::getMap).collect(Collectors.toList()));
        }
        if (from != null) {
            jsonObject.set("from" , from);
        }
        if (size != null) {
            jsonObject.set("size" , size);
        }
        jsonObject.set("aggs" , aggs);
        jsonObject.set("collapse" , collapse);
        if (highlight != null) {
            jsonObject.set("highlight" , highlight);
        }
        return jsonObject.toString();
    }
}
