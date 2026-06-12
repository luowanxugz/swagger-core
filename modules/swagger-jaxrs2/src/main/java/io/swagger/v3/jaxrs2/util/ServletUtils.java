package io.swagger.v3.jaxrs2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServletUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletUtils.class);

    public static Map<String, List<String>> getQueryParams(Map<String, String[]> parameterMap) {
        Map<String, List<String>> queryParameters = new LinkedHashMap<>();

        if (parameterMap.size() == 0) {
            return queryParameters;
        }

        for (Map.Entry<String, String[]> parameter : parameterMap.entrySet()) {
            for (String value : parameter.getValue()) {
                try {
                    String key = URLDecoder.decode(parameter.getKey(), StandardCharsets.UTF_8.name());
                    String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                    queryParameters.computeIfAbsent(key, k -> new ArrayList<>()).add(decodedValue);
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("Unable to decode query parameter", e);
                }
            }
        }
        return queryParameters;
    }

    public static Map<String, String> getCookies(Cookie[] cookies) {
        Map<String, String> mapOfCookies = new HashMap<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                mapOfCookies.put(cookie.getName(), cookie.getValue());
            }
        }
        return mapOfCookies;
    }

    public static Map<String, List<String>> getHeaders(HttpServletRequest req) {
        if (req.getHeaderNames() == null) {
            return Collections.emptyMap();
        } else {
            return Collections
                    .list(req.getHeaderNames())
                    .stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            header -> Collections.list(req.getHeaders(header))));
        }
    }

}
