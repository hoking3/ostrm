-- 添加API刷新配置相关字段
ALTER TABLE task_config ADD COLUMN enable_openlist_refresh TINYINT(1) DEFAULT 1;
ALTER TABLE task_config ADD COLUMN enable_emby_refresh TINYINT(1) DEFAULT 1;
ALTER TABLE task_config ADD COLUMN emby_server_url VARCHAR(255) DEFAULT 'http://localhost:8096';
ALTER TABLE task_config ADD COLUMN emby_api_key VARCHAR(255) DEFAULT '';
ALTER TABLE task_config ADD COLUMN emby_username VARCHAR(255) DEFAULT NULL;
ALTER TABLE task_config ADD COLUMN emby_password VARCHAR(255) DEFAULT NULL;

-- 添加注释说明字段用途
-- enable_openlist_refresh: 控制是否启用OpenList数据刷新，默认为1（启用）
-- enable_emby_refresh: 控制是否启用Emby媒体库刷新，默认为1（启用）
-- emby_server_url: Emby服务器URL，默认为http://localhost:8096
-- emby_api_key: Emby API密钥，默认为空字符串
-- emby_username: Emby用户名，默认为NULL
-- emby_password: Emby密码，默认为NULL
