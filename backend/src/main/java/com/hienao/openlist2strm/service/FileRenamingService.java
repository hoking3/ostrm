package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.media.MediaInfo;
import com.hienao.openlist2strm.dto.tmdb.TmdbMovieDetail;
import com.hienao.openlist2strm.dto.tmdb.TmdbSearchResponse;
import com.hienao.openlist2strm.dto.tmdb.TmdbSeasonDetail;
import com.hienao.openlist2strm.dto.tmdb.TmdbTvDetail;
import com.hienao.openlist2strm.util.MediaFileParser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRenamingService {

  private final TmdbApiService tmdbApiService;
  private final SystemConfigService systemConfigService;

  public Map<String, Object> matchFileToTmdb(String fileName, String directoryPath) {
    Map<String, Object> result = new HashMap<>();
    result.put("fileName", fileName);
    result.put("matched", false);

    try {
      log.info("开始匹配文件到TMDB: {}", fileName);

      Map<String, Object> regexConfig = systemConfigService.getScrapingRegexConfig();
      @SuppressWarnings("unchecked")
      List<String> movieRegexps =
          (List<String>) regexConfig.getOrDefault("movieRegexps", Collections.emptyList());
      @SuppressWarnings("unchecked")
      List<String> tvDirRegexps =
          (List<String>) regexConfig.getOrDefault("tvDirRegexps", Collections.emptyList());
      @SuppressWarnings("unchecked")
      List<String> tvFileRegexps =
          (List<String>) regexConfig.getOrDefault("tvFileRegexps", Collections.emptyList());

      MediaInfo mediaInfo =
          MediaFileParser.parse(fileName, directoryPath, movieRegexps, tvDirRegexps, tvFileRegexps);
      log.debug("解析媒体信息: {}", mediaInfo);

      if (mediaInfo.getConfidence() < 70) {
        log.info("文件名解析置信度过低，继续尝试搜索");
      }

      Map<String, Object> matchResult;
      
      if (mediaInfo.isTvShow() || mediaInfo.getSeason() != null || mediaInfo.getEpisode() != null) {
        matchResult = matchTvShow(mediaInfo, fileName);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
        log.info("电视剧搜索失败，尝试电影搜索: {}", fileName);
        matchResult = matchMovie(mediaInfo, fileName);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
      } else if (mediaInfo.isMovie()) {
        matchResult = matchMovie(mediaInfo, fileName);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
        log.info("电影搜索失败，尝试电视剧搜索: {}", fileName);
        matchResult = matchTvShow(mediaInfo, fileName);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
      } else {
        log.info("未明确类型，先尝试电视剧搜索: {}", fileName);
        matchResult = matchTvShow(mediaInfo, fileName);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
        log.info("电视剧搜索失败，尝试电影搜索: {}", fileName);
        matchResult = matchMovie(mediaInfo, fileName);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
      }

      result.put("error", "未找到匹配的影视内容");
      return result;

    } catch (Exception e) {
      log.error("匹配文件到TMDB失败: {}", fileName, e);
      result.put("error", e.getMessage());
      return result;
    }
  }

  private Map<String, Object> matchMovie(MediaInfo mediaInfo, String fileName) {
    Map<String, Object> result = new HashMap<>();
    result.put("fileName", fileName);
    result.put("type", "movie");

    try {
      TmdbSearchResponse searchResponse =
          tmdbApiService.searchMovies(mediaInfo.getSearchQuery(), mediaInfo.getYear());

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        log.info("使用完整搜索查询未找到结果，尝试简化搜索: {}", fileName);
        String simpleQuery = extractSimpleTitle(mediaInfo.getSearchQuery());
        if (!simpleQuery.equals(mediaInfo.getSearchQuery())) {
          searchResponse = tmdbApiService.searchMovies(simpleQuery, null);
        }
      }

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        result.put("error", "未找到匹配的电影");
        result.put("matched", false);
        return result;
      }

      TmdbSearchResponse.TmdbSearchResult bestMatch = selectBestMovieMatch(searchResponse.getResults(), mediaInfo);

      if (bestMatch == null) {
        bestMatch = searchResponse.getResults().get(0);
        log.info("使用默认匹配结果: {}", bestMatch.getTitle());
      }

      TmdbMovieDetail movieDetail = tmdbApiService.getMovieDetail(bestMatch.getId());

      String newFileName = generateMovieFileName(movieDetail, fileName);

      result.put("matched", true);
      result.put("tmdbId", movieDetail.getId());
      result.put("title", movieDetail.getTitle());
      result.put("originalTitle", movieDetail.getOriginalTitle());
      result.put("year", movieDetail.getReleaseDate() != null && movieDetail.getReleaseDate().length() >= 4
          ? movieDetail.getReleaseDate().substring(0, 4)
          : null);
      result.put("overview", movieDetail.getOverview());
      result.put("posterPath", movieDetail.getPosterPath());
      result.put("posterUrl", tmdbApiService.buildPosterUrl(movieDetail.getPosterPath()));
      result.put("backdropPath", movieDetail.getBackdropPath());
      result.put("backdropUrl", tmdbApiService.buildBackdropUrl(movieDetail.getBackdropPath()));
      result.put("voteAverage", movieDetail.getVoteAverage());
      result.put("newFileName", newFileName);
      result.put("searchResults", searchResponse.getResults());

      return result;

    } catch (Exception e) {
      log.error("匹配电影失败: {}", fileName, e);
      result.put("error", e.getMessage());
      result.put("matched", false);
      return result;
    }
  }

  private Map<String, Object> matchTvShow(MediaInfo mediaInfo, String fileName) {
    Map<String, Object> result = new HashMap<>();
    result.put("fileName", fileName);
    result.put("type", "tv");

    try {
      TmdbSearchResponse searchResponse =
          tmdbApiService.searchTvShows(mediaInfo.getSearchQuery(), mediaInfo.getYear());

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        log.info("使用完整搜索查询未找到结果，尝试简化搜索: {}", fileName);
        String simpleQuery = extractSimpleTitle(mediaInfo.getSearchQuery());
        if (!simpleQuery.equals(mediaInfo.getSearchQuery())) {
          searchResponse = tmdbApiService.searchTvShows(simpleQuery, null);
        }
      }

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        result.put("error", "未找到匹配的电视剧");
        result.put("matched", false);
        return result;
      }

      TmdbSearchResponse.TmdbSearchResult bestMatch = selectBestTvMatch(searchResponse.getResults(), mediaInfo);

      if (bestMatch == null) {
        bestMatch = searchResponse.getResults().get(0);
        log.info("使用默认匹配结果: {}", bestMatch.getName());
      }

      TmdbTvDetail tvDetail = tmdbApiService.getTvDetail(bestMatch.getId());

      String newFileName = generateTvShowFileName(tvDetail, mediaInfo, fileName);

      result.put("matched", true);
      result.put("tmdbId", tvDetail.getId());
      result.put("title", tvDetail.getName());
      result.put("originalTitle", tvDetail.getOriginalName());
      result.put("year", tvDetail.getFirstAirDate() != null && tvDetail.getFirstAirDate().length() >= 4
          ? tvDetail.getFirstAirDate().substring(0, 4)
          : null);
      result.put("overview", tvDetail.getOverview());
      result.put("posterPath", tvDetail.getPosterPath());
      result.put("posterUrl", tmdbApiService.buildPosterUrl(tvDetail.getPosterPath()));
      result.put("backdropPath", tvDetail.getBackdropPath());
      result.put("backdropUrl", tmdbApiService.buildBackdropUrl(tvDetail.getBackdropPath()));
      result.put("voteAverage", tvDetail.getVoteAverage());
      result.put("season", mediaInfo.getSeason());
      result.put("episode", mediaInfo.getEpisode());
      result.put("newFileName", newFileName);
      result.put("searchResults", searchResponse.getResults());

      return result;

    } catch (Exception e) {
      log.error("匹配电视剧失败: {}", fileName, e);
      result.put("error", e.getMessage());
      result.put("matched", false);
      return result;
    }
  }

  private String extractSimpleTitle(String title) {
    if (title == null || title.isEmpty()) {
      return title;
    }
    
    String simpleTitle = title;
    simpleTitle = simpleTitle.replaceAll("[\\[\\]()\\-._]", " ");
    simpleTitle = simpleTitle.replaceAll("\\s+", " ");
    simpleTitle = simpleTitle.trim();
    
    return simpleTitle;
  }

  public String generateMovieFileName(TmdbMovieDetail movieDetail, String originalFileName) {
    String extension = getFileExtension(originalFileName);
    String title = sanitizeFileName(movieDetail.getTitle());
    String year = movieDetail.getReleaseDate() != null && movieDetail.getReleaseDate().length() >= 4
        ? movieDetail.getReleaseDate().substring(0, 4)
        : "";

    if (!year.isEmpty()) {
      return title + " (" + year + ")" + extension;
    } else {
      return title + extension;
    }
  }

  public String generateTvShowFileName(TmdbTvDetail tvDetail, MediaInfo mediaInfo, String originalFileName) {
    return generateTvShowFileName(tvDetail, mediaInfo, originalFileName, true);
  }

  public String generateTvShowFileName(TmdbTvDetail tvDetail, MediaInfo mediaInfo, String originalFileName, boolean useDotSeparator) {
    String extension = getFileExtension(originalFileName);
    String title = sanitizeFileName(tvDetail.getName());
    
    Integer season = mediaInfo.getSeason();
    Integer episode = mediaInfo.getEpisode();
    
    if (season == null) {
      season = 1;
    }
    
    String seasonStr = String.format("S%02d", season);
    String episodeStr = episode != null ? String.format("E%02d", episode) : "";

    StringBuilder sb = new StringBuilder();
    sb.append(title);

    if (!seasonStr.isEmpty() || !episodeStr.isEmpty()) {
      if (useDotSeparator) {
        sb.append(".");
      } else {
        sb.append(" - ");
      }
      sb.append(seasonStr);
      if (!episodeStr.isEmpty()) {
        sb.append(episodeStr);
      }
    }

    sb.append(extension);
    return sb.toString();
  }

  public List<Map<String, Object>> batchMatchFilesToTmdb(List<Map<String, String>> files) {
    List<Map<String, Object>> results = new ArrayList<>();

    for (Map<String, String> file : files) {
      String fileName = file.get("fileName");
      String directoryPath = file.get("directoryPath");
      Map<String, Object> result = matchFileToTmdb(fileName, directoryPath);
      result.put("path", file.get("path"));
      results.add(result);
    }

    return results;
  }

  public List<Map<String, Object>> batchMatchFilesToTmdbWithConfig(
      List<Map<String, String>> files, 
      String searchTitle, 
      Integer searchYear) {
    List<Map<String, Object>> results = new ArrayList<>();
    
    try {
      log.info("开始批量匹配文件到TMDB: searchTitle={}, searchYear={}, fileCount={}", 
          searchTitle, searchYear, files.size());
      
      String yearStr = searchYear != null ? String.valueOf(searchYear) : null;
      TmdbSearchResponse searchResponse = tmdbApiService.searchTvShows(searchTitle, yearStr);
      
      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        log.warn("未找到匹配的电视剧，尝试电影搜索: {}", searchTitle);
        searchResponse = tmdbApiService.searchMovies(searchTitle, yearStr);
        
        if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
          log.error("未找到匹配的影视内容: {}", searchTitle);
          for (Map<String, String> file : files) {
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.get("fileName"));
            result.put("matched", false);
            result.put("error", "未找到匹配的影视内容");
            result.put("path", file.get("path"));
            results.add(result);
          }
          return results;
        }
        
        TmdbSearchResponse.TmdbSearchResult bestMovieMatch = searchResponse.getResults().get(0);
        TmdbMovieDetail movieDetail = tmdbApiService.getMovieDetail(bestMovieMatch.getId());
        
        for (Map<String, String> file : files) {
          String fileName = file.get("fileName");
          Map<String, Object> result = new HashMap<>();
          result.put("fileName", fileName);
          result.put("type", "movie");
          result.put("matched", true);
          result.put("tmdbId", movieDetail.getId());
          result.put("title", movieDetail.getTitle());
          result.put("originalTitle", movieDetail.getOriginalTitle());
          result.put("year", movieDetail.getReleaseDate() != null && movieDetail.getReleaseDate().length() >= 4
              ? movieDetail.getReleaseDate().substring(0, 4)
              : null);
          result.put("overview", movieDetail.getOverview());
          result.put("posterPath", movieDetail.getPosterPath());
          result.put("posterUrl", tmdbApiService.buildPosterUrl(movieDetail.getPosterPath()));
          result.put("backdropPath", movieDetail.getBackdropPath());
          result.put("backdropUrl", tmdbApiService.buildBackdropUrl(movieDetail.getBackdropPath()));
          result.put("voteAverage", movieDetail.getVoteAverage());
          result.put("newFileName", generateMovieFileName(movieDetail, fileName));
          result.put("searchResults", searchResponse.getResults());
          result.put("path", file.get("path"));
          result.put("apply", true);
          results.add(result);
        }
        return results;
      }
      
      TmdbSearchResponse.TmdbSearchResult bestTvMatch = searchResponse.getResults().get(0);
      TmdbTvDetail tvDetail = tmdbApiService.getTvDetail(bestTvMatch.getId());
      
      log.info("成功获取剧集信息: {} (TMDB ID: {})", tvDetail.getName(), tvDetail.getId());
      
      List<TmdbSeasonDetail.Episode> allEpisodes = getAllEpisodes(tvDetail);
      log.info("获取到所有集数信息，共 {} 集", allEpisodes.size());
      
      results = smartMatchAllFiles(files, allEpisodes, tvDetail, searchResponse);
      
    } catch (Exception e) {
      log.error("批量匹配失败", e);
      for (Map<String, String> file : files) {
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.get("fileName"));
        result.put("matched", false);
        result.put("error", e.getMessage());
        result.put("path", file.get("path"));
        results.add(result);
      }
    }
    
    return results;
  }
  
  private List<Map<String, Object>> smartMatchAllFiles(
      List<Map<String, String>> files,
      List<TmdbSeasonDetail.Episode> allEpisodes,
      TmdbTvDetail tvDetail,
      TmdbSearchResponse searchResponse) {
    
    List<Map<String, Object>> results = new ArrayList<>();
    Set<String> matchedEpisodeKeys = new HashSet<>();
    List<Map<String, Object>> unmatchedResults = new ArrayList<>();
    
    for (Map<String, String> file : files) {
      String fileName = file.get("fileName");
      String filePath = file.get("path");
      
      log.info("第一轮匹配（高置信度）: {}", fileName);
      
      Map<String, Object> result = new HashMap<>();
      result.put("fileName", fileName);
      result.put("type", "tv");
      result.put("path", filePath);
      
      try {
        EpisodeMatch bestMatch = findBestEpisodeMatch(fileName, allEpisodes, matchedEpisodeKeys, 70.0);
        
        if (bestMatch != null && bestMatch.episode != null) {
          String episodeKey = bestMatch.episode.getSeasonNumber() + "_" + bestMatch.episode.getEpisodeNumber();
          matchedEpisodeKeys.add(episodeKey);
          
          Integer season = bestMatch.episode.getSeasonNumber() != null ? bestMatch.episode.getSeasonNumber() : 1;
          Integer episode = bestMatch.episode.getEpisodeNumber();
          
          log.info("高置信度匹配成功: {} -> S{}E{} (得分: {})", fileName, season, episode, bestMatch.score);
          
          MediaInfo mediaInfo = new MediaInfo();
          mediaInfo.setSeason(season);
          mediaInfo.setEpisode(episode);
          
          String newFileName = generateTvShowFileName(tvDetail, mediaInfo, fileName);
          
          result.put("matched", true);
          result.put("tmdbId", tvDetail.getId());
          result.put("title", tvDetail.getName());
          result.put("originalTitle", tvDetail.getOriginalName());
          result.put("year", tvDetail.getFirstAirDate() != null && tvDetail.getFirstAirDate().length() >= 4
              ? tvDetail.getFirstAirDate().substring(0, 4)
              : null);
          result.put("overview", tvDetail.getOverview());
          result.put("posterPath", tvDetail.getPosterPath());
          result.put("posterUrl", tmdbApiService.buildPosterUrl(tvDetail.getPosterPath()));
          result.put("backdropPath", tvDetail.getBackdropPath());
          result.put("backdropUrl", tmdbApiService.buildBackdropUrl(tvDetail.getBackdropPath()));
          result.put("voteAverage", tvDetail.getVoteAverage());
          result.put("season", season);
          result.put("episode", episode);
          result.put("newFileName", newFileName);
          result.put("searchResults", searchResponse.getResults());
          result.put("apply", true);
          result.put("matchScore", bestMatch.score);
          
          results.add(result);
        } else {
          unmatchedResults.add(result);
        }
        
      } catch (Exception e) {
        log.error("第一轮匹配失败: {}", fileName, e);
        result.put("matched", false);
        result.put("error", e.getMessage());
        unmatchedResults.add(result);
      }
    }
    
    int episodeCounter = 1;
    for (Map<String, Object> result : unmatchedResults) {
      String fileName = (String) result.get("fileName");
      String filePath = (String) result.get("path");
      
      log.info("第二轮匹配（低置信度/提取）: {}", fileName);
      
      try {
        EpisodeMatch bestMatch = findBestEpisodeMatch(fileName, allEpisodes, matchedEpisodeKeys, 40.0);
        
        Integer season = 1;
        Integer episode = null;
        Double matchScore = null;
        
        if (bestMatch != null && bestMatch.episode != null) {
          String episodeKey = bestMatch.episode.getSeasonNumber() + "_" + bestMatch.episode.getEpisodeNumber();
          matchedEpisodeKeys.add(episodeKey);
          
          season = bestMatch.episode.getSeasonNumber() != null ? bestMatch.episode.getSeasonNumber() : 1;
          episode = bestMatch.episode.getEpisodeNumber();
          matchScore = bestMatch.score;
          
          log.info("低置信度匹配成功: {} -> S{}E{} (得分: {})", fileName, season, episode, matchScore);
        } else {
          Map<String, Integer> extracted = extractSeasonAndEpisodeFromFileName(fileName);
          if (extracted.containsKey("season")) {
            season = extracted.get("season");
          }
          if (extracted.containsKey("episode")) {
            episode = extracted.get("episode");
            log.info("从文件名提取到: {} -> S{}E{}", fileName, season, episode);
          } else {
            episode = episodeCounter;
            log.info("无法提取，自动分配: {} -> S{}E{}", fileName, season, episode);
          }
        }
        
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setSeason(season);
        mediaInfo.setEpisode(episode);
        
        String newFileName = generateTvShowFileName(tvDetail, mediaInfo, fileName);
        
        result.put("matched", true);
        result.put("tmdbId", tvDetail.getId());
        result.put("title", tvDetail.getName());
        result.put("originalTitle", tvDetail.getOriginalName());
        result.put("year", tvDetail.getFirstAirDate() != null && tvDetail.getFirstAirDate().length() >= 4
            ? tvDetail.getFirstAirDate().substring(0, 4)
            : null);
        result.put("overview", tvDetail.getOverview());
        result.put("posterPath", tvDetail.getPosterPath());
        result.put("posterUrl", tmdbApiService.buildPosterUrl(tvDetail.getPosterPath()));
        result.put("backdropPath", tvDetail.getBackdropPath());
        result.put("backdropUrl", tmdbApiService.buildBackdropUrl(tvDetail.getBackdropPath()));
        result.put("voteAverage", tvDetail.getVoteAverage());
        result.put("season", season);
        result.put("episode", episode);
        result.put("newFileName", newFileName);
        result.put("searchResults", searchResponse.getResults());
        result.put("apply", true);
        if (matchScore != null) {
          result.put("matchScore", matchScore);
        }
        
        episodeCounter++;
        
      } catch (Exception e) {
        log.error("第二轮匹配失败: {}", fileName, e);
        result.put("matched", false);
        result.put("error", e.getMessage());
      }
      
      results.add(result);
    }
    
    return results;
  }
  
  private EpisodeMatch findBestEpisodeMatch(
      String fileName,
      List<TmdbSeasonDetail.Episode> allEpisodes,
      Set<String> matchedEpisodeKeys,
      double minScore) {
    
    if (allEpisodes == null || allEpisodes.isEmpty()) {
      return null;
    }
    
    String nameWithoutExt = removeFileExtension(fileName);
    String fileDate = extractDateFromFileName(fileName);
    
    TmdbSeasonDetail.Episode bestEpisode = null;
    double bestScore = 0;
    
    for (TmdbSeasonDetail.Episode episode : allEpisodes) {
      String episodeKey = episode.getSeasonNumber() + "_" + episode.getEpisodeNumber();
      
      if (matchedEpisodeKeys.contains(episodeKey)) {
        continue;
      }
      
      double score = calculateMatchScore(nameWithoutExt, fileDate, episode);
      
      if (score > bestScore) {
        bestScore = score;
        bestEpisode = episode;
      }
    }
    
    if (bestEpisode != null && bestScore >= minScore) {
      return new EpisodeMatch(bestEpisode, bestScore);
    }
    
    return null;
  }
  
  private static class EpisodeMatch {
    TmdbSeasonDetail.Episode episode;
    double score;
    
    EpisodeMatch(TmdbSeasonDetail.Episode episode, double score) {
      this.episode = episode;
      this.score = score;
    }
  }

  public Map<String, Object> matchFileToTmdbWithConfig(
      String fileName, 
      String directoryPath, 
      String searchTitle, 
      Integer searchYear) {
    return matchFileToTmdbWithConfig(fileName, directoryPath, searchTitle, searchYear, null);
  }
  
  public Map<String, Object> matchFileToTmdbWithConfig(
      String fileName, 
      String directoryPath, 
      String searchTitle, 
      Integer searchYear,
      Integer episodeCounter) {
    Map<String, Object> result = new HashMap<>();
    result.put("fileName", fileName);
    result.put("matched", false);

    try {
      log.info("开始匹配文件到TMDB(带搜索配置): fileName={}, searchTitle={}, searchYear={}, episodeCounter={}", 
          fileName, searchTitle, searchYear, episodeCounter);

      Map<String, Object> regexConfig = systemConfigService.getScrapingRegexConfig();
      @SuppressWarnings("unchecked")
      List<String> movieRegexps =
          (List<String>) regexConfig.getOrDefault("movieRegexps", Collections.emptyList());
      @SuppressWarnings("unchecked")
      List<String> tvDirRegexps =
          (List<String>) regexConfig.getOrDefault("tvDirRegexps", Collections.emptyList());
      @SuppressWarnings("unchecked")
      List<String> tvFileRegexps =
          (List<String>) regexConfig.getOrDefault("tvFileRegexps", Collections.emptyList());

      MediaInfo mediaInfo =
          MediaFileParser.parse(fileName, directoryPath, movieRegexps, tvDirRegexps, tvFileRegexps);
      log.debug("解析媒体信息: {}", mediaInfo);

      Map<String, Object> matchResult;
      
      if (mediaInfo.isTvShow() || mediaInfo.getSeason() != null || mediaInfo.getEpisode() != null) {
        matchResult = matchTvShowWithConfig(mediaInfo, fileName, searchTitle, searchYear, episodeCounter);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
        log.info("电视剧搜索失败，尝试电影搜索(带配置): {}", fileName);
        matchResult = matchMovieWithConfig(mediaInfo, fileName, searchTitle, searchYear);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
      } else if (mediaInfo.isMovie()) {
        matchResult = matchMovieWithConfig(mediaInfo, fileName, searchTitle, searchYear);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
        log.info("电影搜索失败，尝试电视剧搜索(带配置): {}", fileName);
        matchResult = matchTvShowWithConfig(mediaInfo, fileName, searchTitle, searchYear, episodeCounter);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
      } else {
        log.info("未明确类型，先尝试电视剧搜索(带配置): {}", fileName);
        matchResult = matchTvShowWithConfig(mediaInfo, fileName, searchTitle, searchYear, episodeCounter);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
        log.info("电视剧搜索失败，尝试电影搜索(带配置): {}", fileName);
        matchResult = matchMovieWithConfig(mediaInfo, fileName, searchTitle, searchYear);
        if ((Boolean) matchResult.getOrDefault("matched", false)) {
          return matchResult;
        }
      }

      result.put("error", "未找到匹配的影视内容");
      return result;

    } catch (Exception e) {
      log.error("匹配文件到TMDB失败(带搜索配置): {}", fileName, e);
      result.put("error", e.getMessage());
      return result;
    }
  }

  private Map<String, Object> matchMovieWithConfig(MediaInfo mediaInfo, String fileName, String searchTitle, Integer searchYear) {
    Map<String, Object> result = new HashMap<>();
    result.put("fileName", fileName);
    result.put("type", "movie");

    try {
      String query = searchTitle != null ? searchTitle : mediaInfo.getSearchQuery();
      String yearStr = searchYear != null ? String.valueOf(searchYear) : mediaInfo.getYear();
      
      TmdbSearchResponse searchResponse = tmdbApiService.searchMovies(query, yearStr);

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        log.info("使用完整搜索查询未找到结果，尝试简化搜索: {}", fileName);
        String simpleQuery = extractSimpleTitle(query);
        if (!simpleQuery.equals(query)) {
          searchResponse = tmdbApiService.searchMovies(simpleQuery, null);
        }
      }

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        result.put("error", "未找到匹配的电影");
        result.put("matched", false);
        return result;
      }

      TmdbSearchResponse.TmdbSearchResult bestMatch = selectBestMovieMatch(searchResponse.getResults(), mediaInfo);

      if (bestMatch == null) {
        bestMatch = searchResponse.getResults().get(0);
        log.info("使用默认匹配结果: {}", bestMatch.getTitle());
      }

      TmdbMovieDetail movieDetail = tmdbApiService.getMovieDetail(bestMatch.getId());

      String newFileName = generateMovieFileName(movieDetail, fileName);

      result.put("matched", true);
      result.put("tmdbId", movieDetail.getId());
      result.put("title", movieDetail.getTitle());
      result.put("originalTitle", movieDetail.getOriginalTitle());
      result.put("year", movieDetail.getReleaseDate() != null && movieDetail.getReleaseDate().length() >= 4
          ? movieDetail.getReleaseDate().substring(0, 4)
          : null);
      result.put("overview", movieDetail.getOverview());
      result.put("posterPath", movieDetail.getPosterPath());
      result.put("posterUrl", tmdbApiService.buildPosterUrl(movieDetail.getPosterPath()));
      result.put("backdropPath", movieDetail.getBackdropPath());
      result.put("backdropUrl", tmdbApiService.buildBackdropUrl(movieDetail.getBackdropPath()));
      result.put("voteAverage", movieDetail.getVoteAverage());
      result.put("newFileName", newFileName);
      result.put("searchResults", searchResponse.getResults());

      return result;

    } catch (Exception e) {
      log.error("匹配电影失败(带配置): {}", fileName, e);
      result.put("error", e.getMessage());
      result.put("matched", false);
      return result;
    }
  }

  private Map<String, Object> matchTvShowWithConfig(MediaInfo mediaInfo, String fileName, String searchTitle, Integer searchYear) {
    return matchTvShowEnhanced(mediaInfo, fileName, searchTitle, null);
  }
  
  private Map<String, Object> matchTvShowWithConfig(MediaInfo mediaInfo, String fileName, String searchTitle, Integer searchYear, Integer episodeCounter) {
    return matchTvShowEnhanced(mediaInfo, fileName, searchTitle, episodeCounter);
  }

  public Map<String, Object> searchTmdb(String query, String type, String year) {
    Map<String, Object> result = new HashMap<>();

    try {
      if ("movie".equals(type)) {
        TmdbSearchResponse searchResponse = tmdbApiService.searchMovies(query, year);
        result.put("type", "movie");
        result.put("results", searchResponse.getResults());
      } else if ("tv".equals(type)) {
        TmdbSearchResponse searchResponse = tmdbApiService.searchTvShows(query, year);
        result.put("type", "tv");
        result.put("results", searchResponse.getResults());
      } else {
        TmdbSearchResponse movieResponse = tmdbApiService.searchMovies(query, year);
        TmdbSearchResponse tvResponse = tmdbApiService.searchTvShows(query, year);
        result.put("type", "all");
        result.put("movieResults", movieResponse.getResults());
        result.put("tvResults", tvResponse.getResults());
      }

      result.put("success", true);
      return result;

    } catch (Exception e) {
      log.error("搜索TMDB失败: query={}, type={}", query, type, e);
      result.put("success", false);
      result.put("error", e.getMessage());
      return result;
    }
  }

  public Map<String, Object> getMovieByIdAndGenerateName(Integer tmdbId, String originalFileName) {
    Map<String, Object> result = new HashMap<>();

    try {
      TmdbMovieDetail movieDetail = tmdbApiService.getMovieDetail(tmdbId);
      String newFileName = generateMovieFileName(movieDetail, originalFileName);

      result.put("matched", true);
      result.put("tmdbId", movieDetail.getId());
      result.put("title", movieDetail.getTitle());
      result.put("originalTitle", movieDetail.getOriginalTitle());
      result.put("year", movieDetail.getReleaseDate() != null
          ? new SimpleDateFormat("yyyy").format(movieDetail.getReleaseDate())
          : null);
      result.put("posterUrl", tmdbApiService.buildPosterUrl(movieDetail.getPosterPath()));
      result.put("posterPath", movieDetail.getPosterPath());
      result.put("backdropPath", movieDetail.getBackdropPath());
      result.put("overview", movieDetail.getOverview());
      result.put("voteAverage", movieDetail.getVoteAverage());
      result.put("newFileName", newFileName);

      return result;

    } catch (Exception e) {
      log.error("获取电影详情失败: tmdbId={}", tmdbId, e);
      result.put("matched", false);
      result.put("error", e.getMessage());
      return result;
    }
  }

  public Map<String, Object> getTvByIdAndGenerateName(Integer tmdbId, Integer season, Integer episode, String originalFileName) {
    Map<String, Object> result = new HashMap<>();

    try {
      TmdbTvDetail tvDetail = tmdbApiService.getTvDetail(tmdbId);

      MediaInfo mediaInfo = new MediaInfo();
      
      if (season == null || episode == null) {
        Map<String, Object> regexConfig = systemConfigService.getScrapingRegexConfig();
        @SuppressWarnings("unchecked")
        List<String> tvFileRegexps =
            (List<String>) regexConfig.getOrDefault("tvFileRegexps", Collections.emptyList());
        
        MediaInfo parsedInfo = MediaFileParser.parse(originalFileName, "", 
            Collections.emptyList(), Collections.emptyList(), tvFileRegexps);
        
        if (season == null && parsedInfo.getSeason() != null) {
          season = parsedInfo.getSeason();
        }
        if (episode == null && parsedInfo.getEpisode() != null) {
          episode = parsedInfo.getEpisode();
        }
        
        log.info("从文件名解析 - season: {}, episode: {}", season, episode);
      }
      
      mediaInfo.setSeason(season);
      mediaInfo.setEpisode(episode);

      String newFileName = generateTvShowFileName(tvDetail, mediaInfo, originalFileName);

      result.put("matched", true);
      result.put("tmdbId", tvDetail.getId());
      result.put("title", tvDetail.getName());
      result.put("year", tvDetail.getFirstAirDate() != null
          ? new SimpleDateFormat("yyyy").format(tvDetail.getFirstAirDate())
          : null);
      result.put("posterUrl", tmdbApiService.buildPosterUrl(tvDetail.getPosterPath()));
      result.put("posterPath", tvDetail.getPosterPath());
      result.put("backdropPath", tvDetail.getBackdropPath());
      result.put("overview", tvDetail.getOverview());
      result.put("voteAverage", tvDetail.getVoteAverage());
      result.put("season", season);
      result.put("episode", episode);
      result.put("newFileName", newFileName);

      return result;

    } catch (Exception e) {
      log.error("获取电视剧详情失败: tmdbId={}", tmdbId, e);
      result.put("matched", false);
      result.put("error", e.getMessage());
      return result;
    }
  }

  private TmdbSearchResponse.TmdbSearchResult selectBestMovieMatch(
      List<TmdbSearchResponse.TmdbSearchResult> results, MediaInfo mediaInfo) {
    if (results == null || results.isEmpty()) {
      return null;
    }

    if (results.size() == 1) {
      return results.get(0);
    }

    if (mediaInfo.isHasYear() && mediaInfo.getYear() != null) {
      for (TmdbSearchResponse.TmdbSearchResult result : results) {
        if (mediaInfo.getYear().equals(result.getReleaseYear())) {
          return result;
        }
      }
    }

    return results.stream()
        .filter(r -> r.getVoteAverage() != null)
        .max((r1, r2) -> Double.compare(r1.getVoteAverage(), r2.getVoteAverage()))
        .orElse(results.get(0));
  }

  private TmdbSearchResponse.TmdbSearchResult selectBestTvMatch(
      List<TmdbSearchResponse.TmdbSearchResult> results, MediaInfo mediaInfo) {
    if (results == null || results.isEmpty()) {
      return null;
    }

    if (results.size() == 1) {
      return results.get(0);
    }

    if (mediaInfo.isHasYear() && mediaInfo.getYear() != null) {
      for (TmdbSearchResponse.TmdbSearchResult result : results) {
        if (mediaInfo.getYear().equals(result.getReleaseYear())) {
          return result;
        }
      }
    }

    return results.stream()
        .filter(r -> r.getVoteAverage() != null)
        .max((r1, r2) -> Double.compare(r1.getVoteAverage(), r2.getVoteAverage()))
        .orElse(results.get(0));
  }

  private String getFileExtension(String fileName) {
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return fileName.substring(lastDotIndex);
    }
    return "";
  }

  private String removeFileExtension(String fileName) {
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return fileName.substring(0, lastDotIndex);
    }
    return fileName;
  }

  private String extractDateFromFileName(String fileName) {
    java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
        "(\\d{4})[-_.年](\\d{1,2})[-_.月](\\d{1,2})");
    java.util.regex.Matcher matcher = datePattern.matcher(fileName);
    if (matcher.find()) {
      return String.format("%s-%02d-%02d", 
          matcher.group(1), 
          Integer.parseInt(matcher.group(2)), 
          Integer.parseInt(matcher.group(3)));
    }
    return null;
  }

  private Map<String, Integer> extractSeasonAndEpisodeFromFileName(String fileName) {
    log.debug("开始从文件名提取季号和集数: {}", fileName);
    
    String nameWithoutExt = removeFileExtension(fileName);
    Map<String, Integer> result = new HashMap<>();
    
    java.util.regex.Pattern seasonEpisodePattern = java.util.regex.Pattern.compile(
        "[S,s](\\d+)[E,e](\\d+)", 
        java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher seasonEpisodeMatcher = seasonEpisodePattern.matcher(nameWithoutExt);
    if (seasonEpisodeMatcher.find()) {
      Integer season = Integer.parseInt(seasonEpisodeMatcher.group(1));
      Integer episode = Integer.parseInt(seasonEpisodeMatcher.group(2));
      result.put("season", season);
      result.put("episode", episode);
      log.debug("从SXXEXX格式提取: {} -> S{}E{}", fileName, season, episode);
      return result;
    }
    
    java.util.regex.Pattern episodePattern1 = java.util.regex.Pattern.compile(
        "(?:第|ep?|episode)[-_\\s]*(\\d+)(?:期|集|话)?", 
        java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher1 = episodePattern1.matcher(nameWithoutExt);
    if (matcher1.find()) {
      Integer episode = Integer.parseInt(matcher1.group(1));
      result.put("episode", episode);
      log.debug("从第X期/集格式提取到集数: {} -> E{}", fileName, episode);
      return result;
    }
    
    java.util.regex.Pattern leadingNumberPattern = java.util.regex.Pattern.compile(
        "^(\\d+)[-_\\s]");
    java.util.regex.Matcher leadingMatcher = leadingNumberPattern.matcher(nameWithoutExt);
    if (leadingMatcher.find()) {
      String numStr = leadingMatcher.group(1);
      if (numStr.length() != 4 || !isValidYear(numStr)) {
        Integer episode = Integer.parseInt(numStr);
        result.put("episode", episode);
        log.debug("从文件名开头提取到集数: {} -> E{}", fileName, episode);
        return result;
      } else {
        log.debug("跳过4位年份数字: {}", numStr);
      }
    }
    
    log.debug("未从文件名提取到集数: {}", fileName);
    return result;
  }

  private boolean isValidYear(String yearStr) {
    try {
      int year = Integer.parseInt(yearStr);
      return year >= 1900 && year <= 2100;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private TmdbSeasonDetail.Episode smartMatchEpisodeToFileName(
      String fileName, 
      List<TmdbSeasonDetail.Episode> allEpisodes) {
    
    if (allEpisodes == null || allEpisodes.isEmpty()) {
      return null;
    }

    log.debug("开始智能匹配文件名到 TMDB 剧集: {}, 可用集数: {}", 
        fileName, allEpisodes.size());

    String nameWithoutExt = removeFileExtension(fileName);
    String fileDate = extractDateFromFileName(fileName);
    
    TmdbSeasonDetail.Episode bestMatch = null;
    double bestScore = 0;

    for (TmdbSeasonDetail.Episode episode : allEpisodes) {
      double score = calculateMatchScore(nameWithoutExt, fileDate, episode);
      
      if (score > bestScore) {
        bestScore = score;
        bestMatch = episode;
      }
    }

    if (bestMatch != null && bestScore >= 50) {
      log.info("智能匹配成功: {} -> S{}E{} (得分: {})", 
          fileName, 
          bestMatch.getSeasonNumber(), 
          bestMatch.getEpisodeNumber(), 
          bestScore);
      return bestMatch;
    } else {
      log.warn("智能匹配置信度不足: {}, 最佳得分: {}", fileName, bestScore);
      return null;
    }
  }

  private double calculateMatchScore(String fileName, String fileDate, TmdbSeasonDetail.Episode episode) {
    double score = 0;
    String lowerFileName = fileName.toLowerCase();
    
    if (episode.getName() != null) {
      String lowerEpisodeName = episode.getName().toLowerCase();
      
      if (lowerFileName.contains(lowerEpisodeName) || lowerEpisodeName.contains(lowerFileName)) {
        score += 50;
        log.debug("标题完全匹配: {} <-> {}", fileName, episode.getName());
      } else {
        String[] keywords = lowerFileName.split("[\\s._\\-]+");
        int matchCount = 0;
        for (String keyword : keywords) {
          if (keyword.length() > 1 && lowerEpisodeName.contains(keyword)) {
            matchCount++;
          }
        }
        score += Math.min(matchCount * 15, 40);
      }
    }

    if (fileDate != null && episode.getAirDate() != null) {
      if (fileDate.equals(episode.getAirDate())) {
        score += 45;
        log.debug("日期匹配成功: {} == {}", fileDate, episode.getAirDate());
      }
    }

    if (episode.getOverview() != null) {
      String lowerOverview = episode.getOverview().toLowerCase();
      String[] keywords = lowerFileName.split("[\\s._\\-]+");
      int matchCount = 0;
      for (String keyword : keywords) {
        if (keyword.length() > 2 && lowerOverview.contains(keyword)) {
          matchCount++;
        }
      }
      score += Math.min(matchCount * 2, 10);
    }

    log.debug("匹配得分 - 文件名: {}, 集数: S{}E{}, 得分: {}", 
        fileName, episode.getSeasonNumber(), episode.getEpisodeNumber(), score);

    return Math.min(score, 100);
  }

  private List<TmdbSeasonDetail.Episode> getAllEpisodes(TmdbTvDetail tvDetail) {
    List<TmdbSeasonDetail.Episode> allEpisodes = new ArrayList<>();
    
    if (tvDetail.getSeasons() == null) {
      return allEpisodes;
    }

    for (TmdbTvDetail.Season season : tvDetail.getSeasons()) {
      if (season.getSeasonNumber() != null) {
        try {
          TmdbSeasonDetail seasonDetail = 
              tmdbApiService.getSeasonDetail(tvDetail.getId(), season.getSeasonNumber());
          
          if (seasonDetail.getEpisodes() != null) {
            allEpisodes.addAll(seasonDetail.getEpisodes());
          }
        } catch (Exception e) {
          log.warn("获取季详情失败: TV ID={}, Season={}", 
              tvDetail.getId(), season.getSeasonNumber(), e);
        }
      }
    }

    return allEpisodes;
  }

  private Map<String, Object> matchTvShowEnhanced(MediaInfo mediaInfo, String fileName, String searchTitle) {
    return matchTvShowEnhanced(mediaInfo, fileName, searchTitle, null);
  }

  private Map<String, Object> matchTvShowEnhanced(MediaInfo mediaInfo, String fileName, String searchTitle, Integer episodeCounter) {
    Map<String, Object> result = new HashMap<>();
    result.put("fileName", fileName);
    result.put("type", "tv");

    try {
      String query = searchTitle != null ? searchTitle : mediaInfo.getSearchQuery();
      String yearStr = mediaInfo.getYear();
      
      TmdbSearchResponse searchResponse = tmdbApiService.searchTvShows(query, yearStr);

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        log.info("使用完整搜索查询未找到结果，尝试简化搜索: {}", fileName);
        String simpleQuery = extractSimpleTitle(query);
        if (!simpleQuery.equals(query)) {
          searchResponse = tmdbApiService.searchTvShows(simpleQuery, null);
        }
      }

      if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
        result.put("error", "未找到匹配的电视剧");
        result.put("matched", false);
        return result;
      }

      String fileDate = extractDateFromFileName(fileName);
      Integer fileEpisode = extractEpisodeFromFileName(fileName);
      
      if (fileEpisode != null && mediaInfo.getEpisode() == null) {
        mediaInfo.setEpisode(fileEpisode);
      }
      
      if (mediaInfo.getEpisode() == null && episodeCounter != null) {
        mediaInfo.setEpisode(episodeCounter);
        log.info("自动分配集数: {} -> E{}", fileName, episodeCounter);
      }

      TmdbSearchResponse.TmdbSearchResult bestMatch = selectBestTvMatchEnhanced(
          searchResponse.getResults(), mediaInfo, fileName, fileDate);

      if (bestMatch == null) {
        bestMatch = searchResponse.getResults().get(0);
        log.info("使用默认匹配结果: {}", bestMatch.getName());
      }

      TmdbTvDetail tvDetail = tmdbApiService.getTvDetail(bestMatch.getId());

      String newFileName = generateTvShowFileName(tvDetail, mediaInfo, fileName);

      result.put("matched", true);
      result.put("tmdbId", tvDetail.getId());
      result.put("title", tvDetail.getName());
      result.put("originalTitle", tvDetail.getOriginalName());
      result.put("year", tvDetail.getFirstAirDate() != null && tvDetail.getFirstAirDate().length() >= 4
          ? tvDetail.getFirstAirDate().substring(0, 4)
          : null);
      result.put("overview", tvDetail.getOverview());
      result.put("posterPath", tvDetail.getPosterPath());
      result.put("posterUrl", tmdbApiService.buildPosterUrl(tvDetail.getPosterPath()));
      result.put("backdropPath", tvDetail.getBackdropPath());
      result.put("backdropUrl", tmdbApiService.buildBackdropUrl(tvDetail.getBackdropPath()));
      result.put("voteAverage", tvDetail.getVoteAverage());
      result.put("season", mediaInfo.getSeason());
      result.put("episode", mediaInfo.getEpisode());
      result.put("newFileName", newFileName);
      result.put("searchResults", searchResponse.getResults());

      return result;

    } catch (Exception e) {
      log.error("匹配电视剧失败(增强版): {}", fileName, e);
      result.put("error", e.getMessage());
      result.put("matched", false);
      return result;
    }
  }

  private TmdbSearchResponse.TmdbSearchResult selectBestTvMatchEnhanced(
      List<TmdbSearchResponse.TmdbSearchResult> results, 
      MediaInfo mediaInfo, 
      String fileName,
      String fileDate) {
    if (results == null || results.isEmpty()) {
      return null;
    }

    if (results.size() == 1) {
      return results.get(0);
    }

    TmdbSearchResponse.TmdbSearchResult bestMatch = null;
    double bestScore = 0;

    for (TmdbSearchResponse.TmdbSearchResult result : results) {
      double score = 0;

      if (mediaInfo.isHasYear() && mediaInfo.getYear() != null) {
        if (mediaInfo.getYear().equals(result.getReleaseYear())) {
          score += 30;
        }
      }

      if (result.getName() != null) {
        String lowerFileName = fileName.toLowerCase();
        String lowerShowName = result.getName().toLowerCase();
        String[] keywords = lowerFileName.split("[\\s._\\-]+");
        int matchCount = 0;
        for (String keyword : keywords) {
          if (keyword.length() > 1 && lowerShowName.contains(keyword)) {
            matchCount++;
          }
        }
        score += Math.min(matchCount * 10, 40);
      }

      if (result.getOverview() != null) {
        String lowerFileName = fileName.toLowerCase();
        String lowerOverview = result.getOverview().toLowerCase();
        String[] keywords = lowerFileName.split("[\\s._\\-]+");
        int matchCount = 0;
        for (String keyword : keywords) {
          if (keyword.length() > 2 && lowerOverview.contains(keyword)) {
            matchCount++;
          }
        }
        score += Math.min(matchCount * 5, 20);
      }

      if (result.getVoteAverage() != null) {
        score += result.getVoteAverage() * 1;
      }

      if (score > bestScore) {
        bestScore = score;
        bestMatch = result;
      }
    }

    log.info("增强匹配 - 最佳得分: {}, 结果: {}", bestScore, 
        bestMatch != null ? bestMatch.getName() : "null");

    return bestMatch;
  }

  private Integer extractEpisodeFromFileName(String fileName) {
    log.debug("开始从文件名提取集数(旧方法): {}", fileName);
    
    String nameWithoutExt = removeFileExtension(fileName);
    
    java.util.regex.Pattern leadingNumberPattern = java.util.regex.Pattern.compile(
        "^(\\d+)[-_\\s]");
    java.util.regex.Matcher leadingMatcher = leadingNumberPattern.matcher(nameWithoutExt);
    if (leadingMatcher.find()) {
      Integer episode = Integer.parseInt(leadingMatcher.group(1));
      log.debug("从文件名开头提取到集数: {} -> {}", fileName, episode);
      return episode;
    }
    
    java.util.regex.Pattern episodePattern1 = java.util.regex.Pattern.compile(
        "(?:第|ep?|episode)[-_\\s]*(\\d+)(?:期|集|话)?", 
        java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher1 = episodePattern1.matcher(nameWithoutExt);
    if (matcher1.find()) {
      Integer episode = Integer.parseInt(matcher1.group(1));
      log.debug("从第X期/集格式提取到集数: {} -> {}", fileName, episode);
      return episode;
    }
    
    java.util.regex.Pattern seasonEpisodePattern = java.util.regex.Pattern.compile(
        "[S,s]\\d+[E,e](\\d+)", 
        java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher seasonEpisodeMatcher = seasonEpisodePattern.matcher(nameWithoutExt);
    if (seasonEpisodeMatcher.find()) {
      Integer episode = Integer.parseInt(seasonEpisodeMatcher.group(1));
      log.debug("从SXXEXX格式提取到集数: {} -> {}", fileName, episode);
      return episode;
    }
    
    log.debug("未从文件名提取到集数: {}", fileName);
    return null;
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "";
    }
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
