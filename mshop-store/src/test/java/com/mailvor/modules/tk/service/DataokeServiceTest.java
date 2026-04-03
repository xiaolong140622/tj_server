package com.mailvor.modules.tk.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mailvor.modules.tk.param.RankingListParam;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataokeServiceTest {

    @Test
    void rankingListShouldReturnEmptyDataWhenUpstreamObjectHasNoDataArray() {
        TestableDataokeService service = new TestableDataokeService();
        service.setResponse("{\"message\":\"appkey不存在..\"}");

        JSONObject result = service.rankingList(new RankingListParam());

        assertNotNull(result);
        assertEquals("appkey不存在..", result.getString("message"));
        assertTrue(result.getJSONArray("data").isEmpty());
    }

    @Test
    void rankingListShouldReturnEmptyDataWhenUpstreamReturnsArrayPayload() {
        TestableDataokeService service = new TestableDataokeService();
        service.setResponse("[{\"message\":\"appkey不存在..\"}]");

        JSONObject result = service.rankingList(new RankingListParam());

        assertNotNull(result);
        assertTrue(result.getJSONArray("data").isEmpty());
        assertTrue(result.get("raw") instanceof JSONArray);
    }

    @Test
    void rankingListShouldKeepNormalArrayPayload() {
        TestableDataokeService service = new TestableDataokeService();
        service.setResponse("{\"data\":[{\"title\":\"普通商品\"}]}");

        JSONObject result = service.rankingList(new RankingListParam());

        assertEquals(1, result.getJSONArray("data").size());
    }

    private static class TestableDataokeService extends DataokeService {
        private String response;

        void setResponse(String response) {
            this.response = response;
        }

        @Override
        protected String getData(String url, String version, TreeMap<String, Object> paraMap) {
            return response;
        }
    }
}

