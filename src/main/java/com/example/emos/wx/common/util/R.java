package com.example.emos.wx.common.util;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class R extends HashMap<String, Object> {
    public R () {
        put("code", HttpStatus.SC_OK);
        put("msg", "success");
    }
    public static R error() {
        return error(HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE, "未知异常，请联系管理员");
    }

    public static R error (String msg) {
        return error(HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE, msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok (String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok (Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R ok () {
        return new R();
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

}
