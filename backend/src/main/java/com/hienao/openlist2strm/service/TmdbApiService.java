/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hienao.openlist2strm.constant.AppConstants;
import com.hienao.openlist2strm.dto.tmdb.TmdbMovieDetail;
import com.hienao.openlist2strm.dto.tmdb.TmdbSearchResponse;
import com.hienao.openlist2strm.dto.tmdb.TmdbTvDetail;
import com.hienao.openlist2strm.exception.BusinessException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * TMDB API 服务 - Quarkus CDI 版本
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class TmdbApiService {

  @Inject
  ObjectMapper objectMapper;

  @Inject
  SystemConfigService systemConfigService;

  private HttpClient createHttpClient() {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String proxyHost = (String) tmdbConfig.get("proxyHost");
    String proxyPortStr = (String) tmdbConfig.get("proxyPort");
    Integer timeout = (Integer) tmdbConfig.getOrDefault("timeout", 30);

    HttpClient.Builder builder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeout))
        .followRedirects(HttpClient.Redirect.NORMAL);

    if (proxyHost != null && !proxyHost.trim().isEmpty()
        && proxyPortStr != null && !proxyPortStr.trim().isEmpty()) {
      try {
        int proxyPort = Integer.parseInt(proxyPortStr.trim());
        builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost.trim(), proxyPort)));
        Log.info("TMDB API 使用代理: " + proxyHost.trim() + ":" + proxyPort);
      } catch (NumberFormatException e) {
        Log.warnf("代理端口配置无效: " + proxyPortStr + ", 将不使用代理");
      }
    }

    return builder.build();
  }

  private void logRequestDetails(String method, String url, Map<String, String> params) {
    Log.infof("TMDB API 请求 - 方法: " + method + ", URL: " + url);
    if (params != null && !params.isEmpty()) {
      Log.info("TMDB API 请求参数: " + params);
    }
  }

  private void logResponseDetails(String method, int statusCode, String responseBody) {
    Log.infof("TMDB API 响应 - 方法: " + method + ", 状态码: " + statusCode);
    if (responseBody != null) {
      String logBody = responseBody.length() > 1000
          ? responseBody.substring(0, 1000) + "... (truncated)"
          : responseBody;
      Log.debug("TMDB API 响应体: " + logBody);
    }
  }

  private void logErrorDetails(String method, String url, Exception e, String responseBody) {
    Log.errorf("TMDB API 请求失败 - 方法: " + method + ", URL: " + url + ", 错误: " + e.getMessage());
    if (responseBody != null) {
      Log.error("TMDB API 错误响应体: " + responseBody);
    }
  }

  private void logEmptySearchResult(String method, String query, String year, int resultCount) {
    Log.warn("TMDB 搜索结果为空 - 方法: " + method + ", 查询: '" + query
        + "', 年份: " + (year != null ? year : "未指定") + ", 结果数量: " + resultCount);
  }

  @SuppressWarnings("unchecked")
  public TmdbSearchResponse searchMovies(String query, String year) {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String apiKey = (String) tmdbConfig.get("apiKey");

    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new BusinessException("TMDB API Key 未配置");
    }

    HttpClient httpClient = createHttpClient();
    String responseBody = null;
    String url = null;

    try {
      String baseUrl = (String) tmdbConfig.getOrDefault("baseUrl", "https://api.themoviedb.org/3");
      String language = (String) tmdbConfig.getOrDefault("language", "zh-CN");

      StringBuilder urlBuilder = new StringBuilder(baseUrl)
          .append("/search/movie?api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
          .append("&language=").append(URLEncoder.encode(language, StandardCharsets.UTF_8))
          .append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));

      if (year != null && !year.trim().isEmpty()) {
        urlBuilder.append("&year=").append(URLEncoder.encode(year, StandardCharsets.UTF_8));
      }

      url = urlBuilder.toString();

      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("query", query);
      requestParams.put("language", language);
      if (year != null && !year.trim().isEmpty()) {
        requestParams.put("year", year);
      }
      logRequestDetails("GET", url, requestParams);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      responseBody = response.body();

      logResponseDetails("GET", response.statusCode(), responseBody);

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("TMDB API 请求失败，状态码: " + response.statusCode());
      }

      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("TMDB API 返回空响应");
      }

      TmdbSearchResponse searchResponse = objectMapper.readValue(responseBody, TmdbSearchResponse.class);
      int resultCount = searchResponse.getResults() != null ? searchResponse.getResults().size() : 0;

      if (resultCount == 0) {
        logEmptySearchResult("searchMovies", query, year, resultCount);
      } else {
        Log.info("搜索电影 '" + query + "' 找到 " + resultCount + " 个结果");
      }

      return searchResponse;

    } catch (Exception e) {
      logErrorDetails("GET", url, e, responseBody);
      throw new BusinessException("搜索电影失败: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public TmdbSearchResponse searchTvShows(String query, String year) {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String apiKey = (String) tmdbConfig.get("apiKey");

    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new BusinessException("TMDB API Key 未配置");
    }

    HttpClient httpClient = createHttpClient();
    String responseBody = null;
    String url = null;

    try {
      String baseUrl = (String) tmdbConfig.getOrDefault("baseUrl", "https://api.themoviedb.org/3");
      String language = (String) tmdbConfig.getOrDefault("language", "zh-CN");

      StringBuilder urlBuilder = new StringBuilder(baseUrl)
          .append("/search/tv?api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
          .append("&language=").append(URLEncoder.encode(language, StandardCharsets.UTF_8))
          .append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));

      if (year != null && !year.trim().isEmpty()) {
        urlBuilder.append("&first_air_date_year=").append(URLEncoder.encode(year, StandardCharsets.UTF_8));
      }

      url = urlBuilder.toString();

      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("query", query);
      requestParams.put("language", language);
      if (year != null && !year.trim().isEmpty()) {
        requestParams.put("first_air_date_year", year);
      }
      logRequestDetails("GET", url, requestParams);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      responseBody = response.body();

      logResponseDetails("GET", response.statusCode(), responseBody);

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("TMDB API 请求失败，状态码: " + response.statusCode());
      }

      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("TMDB API 返回空响应");
      }

      TmdbSearchResponse searchResponse = objectMapper.readValue(responseBody, TmdbSearchResponse.class);
      int resultCount = searchResponse.getResults() != null ? searchResponse.getResults().size() : 0;

      if (resultCount == 0) {
        logEmptySearchResult("searchTvShows", query, year, resultCount);
      } else {
        Log.info("搜索电视剧 '" + query + "' 找到 " + resultCount + " 个结果");
      }

      return searchResponse;

    } catch (Exception e) {
      logErrorDetails("GET", url, e, responseBody);
      throw new BusinessException("搜索电视剧失败: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public TmdbMovieDetail getMovieDetail(Integer movieId) {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String apiKey = (String) tmdbConfig.get("apiKey");

    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new BusinessException("TMDB API Key 未配置");
    }

    HttpClient httpClient = createHttpClient();
    String responseBody = null;
    String url = null;

    try {
      String baseUrl = (String) tmdbConfig.getOrDefault("baseUrl", "https://api.themoviedb.org/3");
      String language = (String) tmdbConfig.getOrDefault("language", "zh-CN");

      url = baseUrl + "/movie/" + movieId
          + "?api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
          + "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8);

      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("movieId", String.valueOf(movieId));
      requestParams.put("language", language);
      logRequestDetails("GET", url, requestParams);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      responseBody = response.body();

      logResponseDetails("GET", response.statusCode(), responseBody);

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("TMDB API 请求失败，状态码: " + response.statusCode());
      }

      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("TMDB API 返回空响应");
      }

      TmdbMovieDetail movieDetail = objectMapper.readValue(responseBody, TmdbMovieDetail.class);
      Log.info("获取电影详情成功: " + movieDetail.getTitle() + " (" + movieDetail.getId() + ")");

      return movieDetail;

    } catch (Exception e) {
      logErrorDetails("GET", url, e, responseBody);
      throw new BusinessException("获取电影详情失败: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public TmdbTvDetail getTvDetail(Integer tvId) {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String apiKey = (String) tmdbConfig.get("apiKey");

    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new BusinessException("TMDB API Key 未配置");
    }

    HttpClient httpClient = createHttpClient();
    String responseBody = null;
    String url = null;

    try {
      String baseUrl = (String) tmdbConfig.getOrDefault("baseUrl", "https://api.themoviedb.org/3");
      String language = (String) tmdbConfig.getOrDefault("language", "zh-CN");

      url = baseUrl + "/tv/" + tvId
          + "?api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
          + "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8);

      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("tvId", String.valueOf(tvId));
      requestParams.put("language", language);
      logRequestDetails("GET", url, requestParams);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      responseBody = response.body();

      logResponseDetails("GET", response.statusCode(), responseBody);

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("TMDB API 请求失败，状态码: " + response.statusCode());
      }

      if (responseBody == null || responseBody.isEmpty()) {
        throw new BusinessException("TMDB API 返回空响应");
      }

      TmdbTvDetail tvDetail = objectMapper.readValue(responseBody, TmdbTvDetail.class);
      Log.info("获取电视剧详情成功: " + tvDetail.getName() + " (" + tvDetail.getId() + ")");

      return tvDetail;

    } catch (Exception e) {
      logErrorDetails("GET", url, e, responseBody);
      throw new BusinessException("获取电视剧详情失败: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public String buildImageUrl(String imagePath, String size) {
    if (imagePath == null || imagePath.trim().isEmpty()) {
      return null;
    }

    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String imageBaseUrl = (String) tmdbConfig.getOrDefault("imageBaseUrl", "https://image.tmdb.org/t/p");

    if (!imagePath.startsWith("/")) {
      imagePath = "/" + imagePath;
    }

    return imageBaseUrl + "/" + size + imagePath;
  }

  public String buildPosterUrl(String posterPath) {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String posterSize = (String) tmdbConfig.getOrDefault("posterSize", "w500");
    return buildImageUrl(posterPath, posterSize);
  }

  public String buildBackdropUrl(String backdropPath) {
    Map<String, Object> tmdbConfig = systemConfigService.getTmdbConfig();
    String backdropSize = (String) tmdbConfig.getOrDefault("backdropSize", "w1280");
    return buildImageUrl(backdropPath, backdropSize);
  }

  public boolean validateApiKey(String apiKey) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      return false;
    }

    try {
      HttpClient httpClient = createHttpClient();
      String url = "https://api.themoviedb.org/3/configuration?api_key="
          + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("User-Agent", AppConstants.USER_AGENT)
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() >= 200 && response.statusCode() < 300;

    } catch (Exception e) {
      Log.warn("验证TMDB API Key失败: " + e.getMessage());
      return false;
    }
  }
}
