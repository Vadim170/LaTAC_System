chcp 1251 >NUL
mysql.exe -uroot -hlocalhost -e"SELECT @@global.time_zone as 'Был часовой пояс:';SET GLOBAL time_zone = TIME_FORMAT(TIMEDIFF(NOW(), UTC_TIMESTAMP), '+%%H:%%i'); SELECT @@global.time_zone as 'Установленный часовой пояс:';"
chcp 866 >NUL
pause