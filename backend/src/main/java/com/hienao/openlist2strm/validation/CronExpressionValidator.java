/*
 * OStrm - Stream Management System
 * @author hienao
 * @date 2025-12-31
 */

package com.hienao.openlist2strm.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Cron 表达式验证器 - 不依赖 Quartz
 *
 * @author hienao
 * @since 2025-12-31
 */
public class CronExpressionValidator implements ConstraintValidator<ValidCronExpression, String> {

  // 简化的 Cron 表达式正则（支持 5-7 个字段）
  private static final Pattern CRON_PATTERN = Pattern.compile(
      "^(\\*|([0-9]|[1-5][0-9])(-([0-9]|[1-5][0-9]))?(,([0-9]|[1-5][0-9])(-([0-9]|[1-5][0-9]))?)*(/[0-9]+)?|\\?)" +
          "(\\s+(\\*|([0-9]|[1-5][0-9])(-([0-9]|[1-5][0-9]))?(,([0-9]|[1-5][0-9])(-([0-9]|[1-5][0-9]))?)*(/[0-9]+)?|\\?)){4,6}$");

  @Override
  public boolean isValid(String cronExpression, ConstraintValidatorContext context) {
    // 如果为空字符串，则视为有效（允许为空）
    if (cronExpression == null || cronExpression.trim().isEmpty()) {
      return true;
    }

    String expression = cronExpression.trim();
    String[] parts = expression.split("\\s+");

    // Cron 表达式应有 5-7 个字段
    if (parts.length < 5 || parts.length > 7) {
      setErrorMessage(context, "定时任务表达式格式不正确");
      return false;
    }

    // 基本格式验证
    for (String part : parts) {
      if (!isValidCronField(part)) {
        setErrorMessage(context, "定时任务表达式格式不正确");
        return false;
      }
    }

    return true;
  }

  private boolean isValidCronField(String field) {
    if (field == null || field.isEmpty()) {
      return false;
    }
    // 允许的字符: 数字、*、?、-、/、,、以及 L、W、#（Quartz 扩展）
    return field.matches("^[0-9*?\\-/,LW#]+$");
  }

  private void setErrorMessage(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}
