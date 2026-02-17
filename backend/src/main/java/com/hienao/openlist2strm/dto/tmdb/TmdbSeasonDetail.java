package com.hienao.openlist2strm.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * TMDB 剧集季详情DTO
 *
 * @author hienao
 * @since 2024-01-01
 */
@Data
public class TmdbSeasonDetail {

  /** TMDB ID */
  private Integer id;

  /** 季号 */
  @JsonProperty("season_number")
  private Integer seasonNumber;

  /** 名称 */
  private String name;

  /** 概述 */
  private String overview;

  /** 海报路径 */
  @JsonProperty("poster_path")
  private String posterPath;

  /** 首播日期 */
  @JsonProperty("air_date")
  private String airDate;

  /** 集数列表 */
  private List<Episode> episodes;

  /**
   * 单集详情
   */
  @Data
  public static class Episode {
    private Integer id;

    /** 集号 */
    @JsonProperty("episode_number")
    private Integer episodeNumber;

    /** 季号 */
    @JsonProperty("season_number")
    private Integer seasonNumber;

    /** 名称 */
    private String name;

    /** 概述 */
    private String overview;

    /** 首播日期 */
    @JsonProperty("air_date")
    private String airDate;

    /** 时长（分钟） */
    @JsonProperty("runtime")
    private Integer runtime;

    /** 评分 */
    @JsonProperty("vote_average")
    private Double voteAverage;

    /** 评分人数 */
    @JsonProperty("vote_count")
    private Integer voteCount;

    /** 剧照路径 */
    @JsonProperty("still_path")
    private String stillPath;
  }
}
