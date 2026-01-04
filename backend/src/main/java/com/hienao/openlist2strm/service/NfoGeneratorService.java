/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.media.MediaInfo;
import com.hienao.openlist2strm.dto.tmdb.TmdbMovieDetail;
import com.hienao.openlist2strm.dto.tmdb.TmdbTvDetail;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * NFO 文件生成服务 - Quarkus CDI 版本
 * 生成兼容 Kodi/Jellyfin/Emby 的标准 NFO 格式文件
 *
 * @author hienao
 * @since 2025-12-31
 */
@ApplicationScoped
public class NfoGeneratorService {

  @Inject
  SystemConfigService systemConfigService;

  @Inject
  TmdbApiService tmdbApiService;

  public void generateMovieNfo(
      TmdbMovieDetail movieDetail, MediaInfo mediaInfo, String nfoFilePath) {
    try {
      String nfoContent = buildMovieNfoContent(movieDetail, mediaInfo);
      writeNfoFile(nfoFilePath, nfoContent);
      Log.info("电影NFO文件生成成功: " + nfoFilePath);
    } catch (Exception e) {
      Log.error("生成电影NFO文件失败: " + e.getMessage(), e);
      throw new RuntimeException("生成电影NFO文件失败", e);
    }
  }

  public void generateTvShowNfo(TmdbTvDetail tvDetail, MediaInfo mediaInfo, String nfoFilePath) {
    try {
      String nfoContent = buildTvShowNfoContent(tvDetail, mediaInfo);
      writeNfoFile(nfoFilePath, nfoContent);
      Log.info("电视剧NFO文件生成成功: " + nfoFilePath);
    } catch (Exception e) {
      Log.error("生成电视剧NFO文件失败: " + e.getMessage(), e);
      throw new RuntimeException("生成电视剧NFO文件失败", e);
    }
  }

  private String buildMovieNfoContent(TmdbMovieDetail movieDetail, MediaInfo mediaInfo) {
    StringBuilder nfo = new StringBuilder();

    nfo.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
    nfo.append("<movie>\n");

    appendElement(nfo, "title", movieDetail.getTitle());
    appendElement(nfo, "originaltitle", movieDetail.getOriginalTitle());
    appendElement(nfo, "plot", movieDetail.getOverview());
    appendElement(nfo, "tagline", movieDetail.getTagline());
    appendElement(nfo, "runtime", movieDetail.getRuntime());

    if (movieDetail.getVoteAverage() != null) {
      nfo.append("  <rating>\n");
      appendElement(nfo, "value", movieDetail.getVoteAverage(), 2);
      appendElement(nfo, "votes", movieDetail.getVoteCount(), 2);
      nfo.append("  </rating>\n");
    }

    appendElement(nfo, "year", movieDetail.getReleaseYear());
    appendElement(nfo, "releasedate", movieDetail.getReleaseDate());

    if (movieDetail.getGenres() != null) {
      for (TmdbMovieDetail.Genre genre : movieDetail.getGenres()) {
        appendElement(nfo, "genre", genre.getName());
      }
    }

    if (movieDetail.getProductionCompanies() != null) {
      for (TmdbMovieDetail.ProductionCompany company : movieDetail.getProductionCompanies()) {
        appendElement(nfo, "studio", company.getName());
      }
    }

    if (movieDetail.getPosterPath() != null) {
      String posterUrl = tmdbApiService.buildPosterUrl(movieDetail.getPosterPath());
      appendElement(nfo, "thumb", posterUrl);
    }

    if (movieDetail.getBackdropPath() != null) {
      String backdropUrl = tmdbApiService.buildBackdropUrl(movieDetail.getBackdropPath());
      appendElement(nfo, "fanart", backdropUrl);
    }

    appendElement(nfo, "tmdbid", movieDetail.getId());
    appendElement(nfo, "imdbid", movieDetail.getImdbId());

    appendElement(nfo, "country", getFirstProductionCountry(movieDetail));
    appendElement(nfo, "language", movieDetail.getOriginalLanguage());
    appendElement(nfo, "status", movieDetail.getStatus());

    appendElement(nfo, "dateadded", getCurrentDateTime());

    nfo.append("</movie>\n");

    return nfo.toString();
  }

  private String buildTvShowNfoContent(TmdbTvDetail tvDetail, MediaInfo mediaInfo) {
    StringBuilder nfo = new StringBuilder();

    nfo.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
    nfo.append("<tvshow>\n");

    appendElement(nfo, "title", tvDetail.getName());
    appendElement(nfo, "originaltitle", tvDetail.getOriginalName());
    appendElement(nfo, "plot", tvDetail.getOverview());

    if (tvDetail.getVoteAverage() != null) {
      nfo.append("  <rating>\n");
      appendElement(nfo, "value", tvDetail.getVoteAverage(), 2);
      appendElement(nfo, "votes", tvDetail.getVoteCount(), 2);
      nfo.append("  </rating>\n");
    }

    appendElement(nfo, "year", tvDetail.getFirstAirYear());
    appendElement(nfo, "premiered", tvDetail.getFirstAirDate());

    appendElement(nfo, "season", tvDetail.getNumberOfSeasons());
    appendElement(nfo, "episode", tvDetail.getNumberOfEpisodes());

    if (tvDetail.getGenres() != null) {
      for (TmdbMovieDetail.Genre genre : tvDetail.getGenres()) {
        appendElement(nfo, "genre", genre.getName());
      }
    }

    if (tvDetail.getNetworks() != null) {
      for (TmdbTvDetail.Network network : tvDetail.getNetworks()) {
        appendElement(nfo, "studio", network.getName());
      }
    }

    if (tvDetail.getCreatedBy() != null) {
      for (TmdbTvDetail.Creator creator : tvDetail.getCreatedBy()) {
        appendElement(nfo, "creator", creator.getName());
      }
    }

    if (tvDetail.getPosterPath() != null) {
      String posterUrl = tmdbApiService.buildPosterUrl(tvDetail.getPosterPath());
      appendElement(nfo, "thumb", posterUrl);
    }

    if (tvDetail.getBackdropPath() != null) {
      String backdropUrl = tmdbApiService.buildBackdropUrl(tvDetail.getBackdropPath());
      appendElement(nfo, "fanart", backdropUrl);
    }

    appendElement(nfo, "tmdbid", tvDetail.getId());

    if (tvDetail.getOriginCountry() != null && !tvDetail.getOriginCountry().isEmpty()) {
      appendElement(nfo, "country", tvDetail.getOriginCountry().get(0));
    }
    appendElement(nfo, "language", tvDetail.getOriginalLanguage());
    appendElement(nfo, "status", tvDetail.getStatus());

    Integer avgRuntime = tvDetail.getAverageEpisodeRuntime();
    if (avgRuntime != null) {
      appendElement(nfo, "runtime", avgRuntime);
    }

    appendElement(nfo, "dateadded", getCurrentDateTime());

    nfo.append("</tvshow>\n");

    return nfo.toString();
  }

  private void appendElement(StringBuilder sb, String tagName, Object value) {
    appendElement(sb, tagName, value, 1);
  }

  private void appendElement(StringBuilder sb, String tagName, Object value, int indentLevel) {
    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
      return;
    }

    String indent = "  ".repeat(indentLevel);
    sb.append(indent)
        .append("<")
        .append(tagName)
        .append(">")
        .append(escapeXml(value.toString()))
        .append("</")
        .append(tagName)
        .append(">\n");
  }

  private String escapeXml(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private String getFirstProductionCountry(TmdbMovieDetail movieDetail) {
    if (movieDetail.getProductionCountries() != null
        && !movieDetail.getProductionCountries().isEmpty()) {
      return movieDetail.getProductionCountries().get(0).getName();
    }
    return null;
  }

  private String getCurrentDateTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  @SuppressWarnings("unchecked")
  private void writeNfoFile(String nfoFilePath, String content) throws IOException {
    Path path = Paths.get(nfoFilePath);

    Path parentDir = path.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }

    if (Files.exists(path)) {
      Map<String, Object> scrapingConfig = systemConfigService.getScrapingConfig();
      boolean overwriteExisting = (Boolean) scrapingConfig.getOrDefault("overwriteExisting", false);

      if (!overwriteExisting) {
        Log.info("检测到同名NFO文件已存在，跳过生成: " + nfoFilePath);
        return;
      } else {
        Log.info("同名NFO文件已存在，但允许覆盖，继续生成: " + nfoFilePath);
      }
    }

    Files.writeString(path, content);
  }
}
