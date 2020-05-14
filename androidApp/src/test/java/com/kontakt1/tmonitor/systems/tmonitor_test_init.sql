DROP DATABASE IF EXISTS tmonitor_test;
CREATE DATABASE tmonitor_test DEFAULT CHARACTER SET utf8;

USE tmonitor_test;

DROP TABLE IF EXISTS tree;
# Создание таблицы объектов (древовидная структура, вложенные множества)
CREATE TABLE tree ( 
  id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  node_name VARCHAR(128) NOT NULL DEFAULT "",
  left_ix int NOT NULL DEFAULT 0,
  right_ix int NOT NULL DEFAULT 0,
  INDEX ix (left_ix, right_ix)
);

# Создание таблицы-списка температурных параметров
DROP TABLE IF EXISTS tparam;
CREATE TABLE tparam(
  #Идентификатор
  id int NOT NULL  AUTO_INCREMENT PRIMARY KEY,
  # Текстовый псевдоним
  t_alias VARCHAR(128) NOT NULL DEFAULT "",
  # Имя (описание)
  t_name VARCHAR(128) NOT NULL UNIQUE DEFAULT "Новый параметр",
  # Принадлежность объекту  
  t_parent int NOT NULL DEFAULT 0,
  # Число датчиков
  t_sensors TINYINT UNSIGNED NOT NULL DEFAULT 0,
  FOREIGN  KEY (t_parent) REFERENCES tree(id) ON DELETE CASCADE
);

# Создание таблицы-списка параметров уровнеметрии
DROP TABLE IF EXISTS lparam;
CREATE TABLE lparam(
  # Идентификатор
  id int NOT NULL  AUTO_INCREMENT PRIMARY KEY,
  # Текстовый псевдоним
  l_alias VARCHAR(128) NOT NULL  DEFAULT "",
  # Имя (описание)
  l_name VARCHAR(128) NOT NULL  UNIQUE DEFAULT "Новый параметр",
  # Принадлежность объекту  
  l_parent int NOT NULL DEFAULT 0,
  # Диапазон измерений
  l_range float NOT NULL DEFAULT 0,
  FOREIGN  KEY (l_parent) REFERENCES tree(id) ON DELETE CASCADE
);

# Создание таблицы-списка параметров уставок уровнеметрии
DROP TABLE IF EXISTS ldupparam;
CREATE TABLE ldupparam(
  # Идентификатор
  id int NOT NULL  AUTO_INCREMENT PRIMARY KEY,
  # Текстовый псевдоним
  ld_alias VARCHAR(128) NOT NULL  DEFAULT "",
  # Имя (описание)
  ld_name VARCHAR(128) NOT NULL  UNIQUE DEFAULT "Новый параметр",
  # Принадлежность объекту  
  ld_parent int NOT NULL DEFAULT 0,
  # Показания
  # ld_up   TINYINT NOT NULL DEFAULT 0,
  FOREIGN  KEY (ld_parent) REFERENCES tree(id) ON DELETE CASCADE
);

# Создание таблицы-списка параметров уставок уровнеметрии
DROP TABLE IF EXISTS lddownparam;
CREATE TABLE lddownparam(
  # Идентификатор
  id int NOT NULL  AUTO_INCREMENT PRIMARY KEY,
  # Текстовый псевдоним
  ld_alias VARCHAR(128) NOT NULL  DEFAULT "",
  # Имя (описание)
  ld_name VARCHAR(128) NOT NULL  UNIQUE DEFAULT "Новый параметр",
  # Принадлежность объекту  
  ld_parent int NOT NULL DEFAULT 0,
  # Показания
  # ld_down TINYINT NOT NULL DEFAULT 0,
  FOREIGN  KEY (ld_parent) REFERENCES tree(id) ON DELETE CASCADE
);

# Создание таблицы-списка уставок
DROP TABLE IF EXISTS constraints;
CREATE TABLE constraints (
  # Идентификатор
  id int NOT NULL  AUTO_INCREMENT PRIMARY KEY,
  # Имя уставки
  cnstr_name VARCHAR(128) NOT NULL DEFAULT "Уставка",
  # Применение
  cnstr_using int NOT NULL DEFAULT 0,
  # Значение
  cnstr_value float NOT NULL DEFAULT 0,
  # Нечувствительность (гистерезис)
  cnstr_insens float NOT NULL DEFAULT 0,
  # Направление: >0 - по превышению, <0 - по занижению, =0 - уставка отключена
  cnstr_direction int NOT NULL DEFAULT 0  
);

# Создание таблицы применения уставок для температурных параметров
DROP TABLE IF EXISTS tcnstr;
CREATE TABLE tcnstr (
  # Идентификатор (Это какой-то мусор, но удалять его проблематично. Он был PRIMARY KEY, изменил на UNIQUE)
  id int NOT NULL  AUTO_INCREMENT UNIQUE,
  # Ссылка на параметр
  prm_id int NOT NULL,
  # Ссылка на уставку
  cnstr_id int NOT NULL,
  # Текущее состояние 0 - Всё хорошо 1 - уставка сработала
  cnstr_state int NOT NULL DEFAULT 1,
  # Дата последнего поступления данных
  cnstr_last_savetime datetime DEFAULT NULL,
  PRIMARY KEY (prm_id, cnstr_id),
  FOREIGN KEY (prm_id) REFERENCES tparam(id) ON DELETE CASCADE,
  FOREIGN KEY (cnstr_id) REFERENCES constraints(id) ON DELETE CASCADE
);  

# Создание таблицы применения уставок для параметров уровнеметрии
DROP TABLE IF EXISTS lcnstr;
CREATE TABLE lcnstr (
  # Идентификатор (Это какой-то мусор, но удалять его проблематично. Он был PRIMARY KEY, изменил на UNIQUE)
  id int NOT NULL  AUTO_INCREMENT UNIQUE,
  # Ссылка на параметр
  prm_id int NOT NULL,
  # Ссылка на уставку
  cnstr_id int NOT NULL,
  # Текущее состояние 0 - Всё хорошо 1 - уставка сработала
  cnstr_state int NOT NULL DEFAULT 1,
  # Дата последнего поступления данных
  cnstr_last_savetime datetime DEFAULT NULL,
  PRIMARY KEY (prm_id, cnstr_id),
  FOREIGN KEY (prm_id) REFERENCES lparam(id) ON DELETE CASCADE,
  FOREIGN KEY (cnstr_id) REFERENCES constraints(id) ON DELETE CASCADE
);  


DELIMITER //


# Процедура добавления нового элемента
DROP PROCEDURE IF EXISTS tmonitor_test.addnode//
CREATE PROCEDURE tmonitor_test.addnode(parent_node_id INTEGER, new_node_name VARCHAR(128))
BEGIN
   
   DECLARE prt_right INTEGER ;

   IF (parent_node_id=0) THEN
     # Добавляем узел в корень
     SELECT IFNULL(MAX(right_ix), 0) INTO prt_right FROM tree;
     INSERT INTO tree SET node_name=new_node_name, left_ix=prt_right+1, right_ix=prt_right+2;
     SELECT id , left_ix, right_ix FROM tree ORDER BY id DESC LIMIT 1;
   ELSE
     SELECT right_ix INTO prt_right FROM tree WHERE id=parent_node_id;
     IF (NOT ISNULL(prt_right)) THEN
       # Модифицируем дерево 
       UPDATE tree SET right_ix = right_ix + 2, left_ix = IF((left_ix > prt_right), (left_ix + 2), left_ix) WHERE (right_ix >= prt_right);
       #Добавляем дочерний узел
       INSERT INTO tree SET node_name=new_node_name, left_ix = prt_right, right_ix = prt_right+1;
       SELECT id , left_ix, right_ix FROM tree ORDER BY id DESC LIMIT 1;       
     ELSE
       # Попытка добавить узел к несуществующему родителю
       SELECT 0 as id, 0 as left_ix, 0 as right_ix;   
     END IF;
   END IF;
 
END//

DROP PROCEDURE IF EXISTS tmonitor_test.delnode//
# Процедура удаления элемента из дерева (вместе с потомками)
CREATE PROCEDURE tmonitor_test.delnode(deleting_node_id INTEGER)
BEGIN
  DECLARE delnodeleft, delnoderight INTEGER;
  SELECT left_ix, right_ix INTO delnodeleft, delnoderight FROM tree WHERE id=deleting_node_id;
  IF (ISNULL(delnodeleft) OR ISNULL(delnoderight)) THEN
    # Попытка удаления несуществующего узла
   SELECT 0 as result;
   ELSE
     # Удаляем узел
     DELETE FROM tree WHERE (left_ix>=delnodeleft) and (right_ix<=delnoderight); 
     # Модифицируем дерево
     UPDATE tree SET left_ix=IF(left_ix>delnodeleft, left_ix-(delnoderight-delnodeleft+1), left_ix), right_ix=right_ix-(delnoderight-delnodeleft+1) WHERE right_ix>delnoderight;
     SELECT 1 as result;
   END IF;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.movenode//
# Процедура перемещения узла по дереву
CREATE PROCEDURE tmonitor_test.movenode(nodeid INTEGER, moveto INTEGER)
BEGIN
  #Выясняем информацию о предложенных к перемещению узлах
  DECLARE n INTEGER;
  # Проверяем существование источника
  SELECT COUNT(*) INTO n FROM tree WHERE id=nodeid; 
  IF (n>0) THEN 
     # Проверяем существование приемника (или добавление в корень)
     SELECT COUNT(*) INTO n FROM tree WHERE id=moveto;
     IF ((moveto = 0) OR n>0)  THEN
    BEGIN 
       DECLARE nodeLeft, nodeRight INTEGER;
       # Ключи перемещаемого узла 
       SELECT left_ix, right_ix INTO nodeLeft, nodeRight FROM tree WHERE id = nodeid;
       # Проверяем попытку переместить в наследника или в самого себя
       SELECT COUNT(*)  INTO n FROM tree WHERE id=moveto AND left_ix>=nodeLeft AND right_ix<=nodeRight;
       IF (n=0) THEN
       BEGIN 
         DECLARE parentLeft, nodeToRight INTEGER; 
         DECLARE offsetForMovingNode, offsetForOtherNodes INTEGER;
         
         #Определяем максимальное значение правого индекса узла за которым производится вставка. 
         #Если производится добавление в узел, не содержащий потомка, полученному значению не будет соответствовать ни один из
         #правых индексов, но значение остается легитимным         
         IF (moveto>0) THEN
           SELECT left_ix, right_ix-1 INTO parentLeft, nodeToRight FROM tree WHERE id=moveto;
         ELSE
           SELECT MAX(right_ix) INTO nodeToRight FROM tree;
           SET parentLeft=nodeToRight+1;
        END IF;  
         
         # Смещение узлов дерева
         SET offsetForOtherNodes = nodeRight-nodeLeft+1;
         
         # "Вверх" - принимающий узел имеет правый индекс меньше чем левый у переносимого, т .е. левый индекс переносимого узла 
         # должен будет уменьшиться. "Вниз" - больше (индекс увеличится)
         IF (nodeToRight<nodeLeft) THEN
           # Перемещение "вверх" по дереву
           SET offsetForMovingNode = nodeToRight-nodeLeft+1;
           UPDATE tree SET
             right_ix = IF(left_ix>=nodeLeft, right_ix+offsetForMovingNode, IF(right_ix<nodeLeft, right_ix+offsetForOtherNodes, right_ix)),
             left_ix = IF(left_ix>=nodeLeft, left_ix+offsetForMovingNode,  IF(left_ix>nodeToRight, left_ix+offsetForOtherNodes, left_ix))
           WHERE (right_ix>nodeToRight ) AND (left_ix<nodeRight);            
           # WARNING Порядок установки значений полей имеет значение!!! 
         ELSE
           # Перемещение вниз по дереву
           SET offsetForMovingNode = nodeToRight-nodeLeft-offsetForOtherNodes+1;
           UPDATE tree SET
             left_ix = IF(right_ix<=nodeRight, left_ix+offsetForMovingNode, IF(left_ix>nodeRight, left_ix-offsetForOtherNodes, left_ix)),
             right_ix=IF(right_ix<=nodeRight, right_ix+offsetForMovingNode, IF(right_ix<=NodeToRight, right_ix-offsetForOtherNodes, right_ix))
           WHERE (right_ix>nodeLeft) AND (left_ix<=nodeToRight); 
         END IF;
         SELECT  1 as result;
       END;  
       ELSE 
         SELECT 0 AS result;  
       END IF;
    END;  
     ELSE SELECT 0 AS result;
     END IF;
  ELSE SELECT 0 AS result;
  END IF;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.checktree//
# Процедура проверки правильности составления дерева
CREATE PROCEDURE tmonitor_test.checktree()
BEGIN
  DECLARE n INTEGER;
  # Левый индекс всегда меньше правого
  SELECT COUNT(*) INTO n FROM tree WHERE left_ix>right_ix;
  IF (n>0)  THEN
    SELECT 0 as result;
  ELSE
  BEGIN 
    DECLARE minLeft, maxRight INTEGER;
    # Минимальный левый индекс всегда 1, максимальный правый - удвоенное число узлов в дереве
    SELECT COUNT(id), MIN(left_ix), MAX(right_ix) INTO n, minLeft, maxRight FROM tree;
    IF ((minLeft<>1) OR (maxRight<>2*n)) THEN
      SELECT 0 AS result;
    ELSE
      # Разница между правым и левым узлом всегда нечетное число
      SELECT COUNT(id) INTO n FROM tree WHERE ((right_ix-left_ix) % 2) =0;
      IF (n>0) THEN
        SELECT 0 as result;
      ELSE
        # Все ключи должны быть уникальны (число совпадений - 0)
        SELECT Count(*) INTO n FROM  tree AS t1, tree AS t2 WHERE  ((t1.left_ix = t2.left_ix) OR  (t1.right_ix = t2.right_ix))  AND (t1.id<t2.id) OR (t1.left_ix = t2.right_ix);
        IF (n>0) THEN
          SELECT 0 as result;
        ELSE
          SELECT 1 AS result;
        END IF;
      END IF;
    END IF;  
  END;  
  END IF;
END//


DROP PROCEDURE IF EXISTS tmonitor_test.addtparam//
CREATE PROCEDURE tmonitor_test.addtparam( tname VARCHAR(128), talias VARCHAR(128), tsensors TINYINT UNSIGNED, tparent int)
BEGIN
  INSERT INTO tparam (t_name, t_alias, t_sensors, t_parent) VALUES (tname, talias, tsensors, tparent);
  SELECT LAST_INSERT_ID() as lastid;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.addlparam//
CREATE PROCEDURE tmonitor_test.addlparam( lname VARCHAR(128), lalias VARCHAR(128), lrange float, lparent int)
BEGIN
  INSERT INTO lparam (l_name, l_alias, l_range, l_parent) VALUES (lname, lalias, lrange, lparent);
  SELECT LAST_INSERT_ID() as lastid;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.addldupparam//
CREATE PROCEDURE tmonitor_test.addldupparam( ldname VARCHAR(128), ldalias VARCHAR(128), /*ldup TINYINT, lddown TINYINT, */ldparent int)
BEGIN
  INSERT INTO ldupparam (ld_name, ld_alias, /*ld_up, ld_down,*/ ld_parent) VALUES (ldname, ldalias, /*ldup, lddown,*/ ldparent);
  SELECT LAST_INSERT_ID() as lastid;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.addlddownparam//
CREATE PROCEDURE tmonitor_test.addlddownparam( ldname VARCHAR(128), ldalias VARCHAR(128), /*ldup TINYINT, lddown TINYINT, */ldparent int)
BEGIN
  INSERT INTO lddownparam (ld_name, ld_alias, /*ld_up, ld_down,*/ ld_parent) VALUES (ldname, ldalias, /*ldup, lddown,*/ ldparent);
  SELECT LAST_INSERT_ID() as lastid;
END//


DROP PROCEDURE IF EXISTS tmonitor_test.clear_t_parameters//
# Процедура очистки информации о температурных параметрах
CREATE PROCEDURE tmonitor_test.clear_t_parameters()
BEGIN
  # Удаление всех записей о температурных параметрах
  DELETE FROM tmonitor_test.tparam;
  # Удаление всех записей об использовании уставок температурными параметрами
  DELETE FROM tmonitor_test.tcnstr ;
  # Обнуление счетчика температурных параметров
  ALTER TABLE tmonitor_test.tparam AUTO_INCREMENT = 1;
  # Обнуление таблицы использования уставок температурными параметрами
  ALTER TABLE tmonitor_test.tcnstr AUTO_INCREMENT = 1;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.clear_l_parameters//
# Процедура очистки информации о параметрах уровнеметрии
CREATE PROCEDURE tmonitor_test.clear_l_parameters()
BEGIN
  # Удаление всех записей о  параметрах уровнеметрии
  DELETE FROM tmonitor_test.lparam;
  # Удаление всех записей об использовании уставок параметрами уровнеметрии
  DELETE FROM tmonitor_test.lcnstr ;
  # Обнуление счетчика параметров уровнеметрии
  ALTER TABLE tmonitor_test.lparam AUTO_INCREMENT = 1;
  # Обнуление таблицы использования уставок параметрами уровнеметрии
  ALTER TABLE tmonitor_test.lcnstr AUTO_INCREMENT = 1;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.clear_ld_up_parameters//
# Процедура очистки информации о параметрах уровнеметрии
CREATE PROCEDURE tmonitor_test.clear_ld_up_parameters()
BEGIN
  # Удаление всех записей о  параметрах уровнеметрии
  DELETE FROM tmonitor_test.ldupparam;
  # Обнуление счетчика параметров уровнеметрии
  ALTER TABLE tmonitor_test.ldupparam AUTO_INCREMENT = 1;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.clear_ld_down_parameters//
# Процедура очистки информации о параметрах уровнеметрии
CREATE PROCEDURE tmonitor_test.clear_ld_down_parameters()
BEGIN
  # Удаление всех записей о  параметрах уровнеметрии
  DELETE FROM tmonitor_test.lddownparam;
  # Обнуление счетчика параметров уровнеметрии
  ALTER TABLE tmonitor_test.lddownparam AUTO_INCREMENT = 1;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.clear_tree//
# Процедура очистки структуры проекта
CREATE PROCEDURE tmonitor_test.clear_tree()
BEGIN
  # Очистка дерева
  DELETE FROM tmonitor_test.tree;
  ALTER TABLE tmonitor_test.tree AUTO_INCREMENT = 1;

  # Очистка таблиц параметроа
  CALL clear_t_parameters();
  CALL clear_l_parameters();
  CALL clear_ld_up_parameters();
  CALL clear_ld_down_parameters();
END//

DROP PROCEDURE IF EXISTS tmonitor_test.clear_constraints//
# Процедура очистки таблицы уставок
CREATE PROCEDURE tmonitor_test.clear_constraints()
BEGIN
  # Очистка таблицы уставок
  DELETE FROM tmonitor_test.constraints;
  ALTER TABLE tmonitor_test.constraints AUTO_INCREMENT = 1;

  # Очистка таблиц применения уставок
  # температура
  DELETE FROM tmonitor_test.tcnstr;
  ALTER TABLE tmonitor_test.tcnstr AUTO_INCREMENT = 1;
  # уровень
  DELETE FROM tmonitor_test.lcnstr;
  ALTER TABLE tmonitor_test.lcnstr AUTO_INCREMENT = 1;
END//

DROP PROCEDURE IF EXISTS tmonitor_test.create_t_table//
# Процедура создания таблицы термометрии
CREATE PROCEDURE tmonitor_test.create_t_table(table_name VARCHAR(32))
BEGIN
  DECLARE nameWithoutPrefix VARCHAR(32) DEFAULT SUBSTRING(table_name, 2);

  SET @stmt=CONCAT('DROP TABLE IF EXISTS ', table_name,';');
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

  SET @stmt=CONCAT(
    'CREATE  TABLE ', table_name,'(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    savetime DATETIME NOT NULL UNIQUE, 
    t1 FLOAT NULL, 
    t2 FLOAT NULL,
    t3 FLOAT NULL,
    t4 FLOAT NULL,
    t5 FLOAT NULL,
    t6 FLOAT NULL,
    t7 FLOAT NULL,
    t8 FLOAT NULL,
    t9 FLOAT NULL,
    t10 FLOAT NULL,
    t11 FLOAT NULL,
    t12 FLOAT NULL,
    t13 FLOAT NULL,
    t14 FLOAT NULL,
    t15 FLOAT NULL,
    t16 FLOAT NULL,
    t17 FLOAT NULL,
    t18 FLOAT NULL,
    t19 FLOAT NULL,
    t20 FLOAT NULL,
    t21 FLOAT NULL,
    t22 FLOAT NULL,
    t23 FLOAT NULL,
    t24 FLOAT NULL,
    t25 FLOAT NULL,
    t26 FLOAT NULL,
    t27 FLOAT NULL,
    t28 FLOAT NULL,
    t29 FLOAT NULL,
    t30 FLOAT NULL
   )');
  
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

END//

DROP PROCEDURE IF EXISTS tmonitor_test.create_l_table//
# Процедура создания таблицы термометрии
CREATE PROCEDURE tmonitor_test.create_l_table(table_name VARCHAR(32))
BEGIN
  DECLARE nameWithoutPrefix VARCHAR(32) DEFAULT SUBSTRING(table_name, 2);

  SET @stmt=CONCAT('DROP TABLE IF EXISTS ', table_name,';');
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

  SET @stmt=CONCAT(
    'CREATE  TABLE ', table_name,'(
		id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
		savetime DATETIME NOT NULL UNIQUE, 
		l FLOAT NOT NULL 
	)');
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;
  
END//

DROP PROCEDURE IF EXISTS tmonitor_test.create_ld_up_table//
# Процедура создания таблицы термометрии
CREATE PROCEDURE tmonitor_test.create_ld_up_table(table_name VARCHAR(32))
BEGIN

  SET @stmt=CONCAT('DROP TABLE IF EXISTS ', table_name,';');
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

  SET @stmt=CONCAT(
    'CREATE  TABLE ', 
    table_name,
    '(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    savetime DATETIME NOT NULL UNIQUE, 
    ld_up TINYINT UNSIGNED NOT NULL
   )');
  
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

END//

DROP PROCEDURE IF EXISTS tmonitor_test.create_ld_down_table//
# Процедура создания таблицы термометрии
CREATE PROCEDURE tmonitor_test.create_ld_down_table(table_name VARCHAR(32))
BEGIN

  SET @stmt=CONCAT('DROP TABLE IF EXISTS ', table_name,';');
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

  SET @stmt=CONCAT(
    'CREATE  TABLE ', 
    table_name,
    '(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    savetime DATETIME NOT NULL UNIQUE, 
    ld_down TINYINT UNSIGNED NOT NULL 
   )');
  
  PREPARE SQL_STATEMENT FROM @stmt;
  EXECUTE SQL_STATEMENT;
  DEALLOCATE PREPARE SQL_STATEMENT;

END//

DROP PROCEDURE IF EXISTS tmonitor_test.heating_search//
# Процедура определения согревания
CREATE PROCEDURE tmonitor_test.heating_search(param_name VARCHAR(32), avg_interval INTEGER, avg_max FLOAT, disp_max FLOAT)
BEGIN

   SET @Time5 = now() ;
   SET @Time4 = SUBDATE(@Time5, INTERVAL avg_interval HOUR);
   SET @Time3 = SUBDATE(@Time4, INTERVAL avg_interval HOUR);
   SET @Time2 = SUBDATE(@Time3, INTERVAL avg_interval HOUR);
   SET @Time1 = SUBDATE(@Time2, INTERVAL avg_interval HOUR);
   SET @sens_count = (SELECT t_sensors FROM tparam WHERE t_name=param_name); 
   SET @sens_index = 1;
    
   DROP TABLE  IF EXISTS  _tmp_;
   CREATE TEMPORARY TABLE  _tmp_ (sensor_index integer NOT NULL, avg1 FLOAT, avg2 FLOAT, avg3 FLOAT, avg4 FLOAT);
    
   SET @stmt_pattern =  REPLACE(
        'INSERT INTO _tmp_ VALUES  (@sens_index,   (SELECT avg(col) FROM tablename WHERE SaveTime<=@Time5 AND SaveTime> @Time4),   (SELECT avg(col) FROM tablename WHERE SaveTime<=@Time4 AND SaveTime> @Time3),   (SELECT avg(col) FROM tablename WHERE SaveTime<=@Time3 AND SaveTime> @Time2), (SELECT avg(col) FROM tablename WHERE SaveTime<=@Time2 AND SaveTime> @Time1) )' ,
        'tablename',
        CONCAT('t', param_name)
   ); 
    
   WHILE (@sens_count IS NOT NULL) AND (@sens_index<=@sens_count ) DO
       
       SET @stmt = REPLACE (@stmt_pattern, 'col', CONCAT('t', @sens_index));
       PREPARE SQL_STATEMENT FROM @stmt;
       EXECUTE SQL_STATEMENT;
       DEALLOCATE PREPARE SQL_STATEMENT; 
       
       #UNLOCK TABLES;
       
       SET @sens_index= @sens_index+1;  
   END WHILE;
     

   SELECT  * FROM _tmp_ WHERE (avg1>avg_max) AND ((avg1-avg4)>=disp_max) AND (avg1>avg2) AND (avg2>avg3) AND (avg3>avg4) ;
       
END//

DELIMITER ;

# -----------------------------------------
# Триггеры для обновления состояния уставок
# -----------------------------------------
# На момент 12.2019 они используются только в мобильном приложении, в других местах происходит расчет состояний уставок вне SQL. 
# Целесообразнее в будующем перейти полностью на использование состояний, сгенерированных этими триггерами, так как они всегда зависят от предыдущего показания,
# Независимо от того, в какой момент был запузен клиент. Ну и делать такие типичные, одинаковые вычисления на раздых устройствах - тупо.

# Текущее состояние 0 - Всё хорошо 1 - уставка сработала
# Недостаток этого триггера в том, что он включит уставку если откажет один любой датчик.(Будет гнаться темп 250 градусов.)
/*CREATE TRIGGER `update_cnstr_t%s` AFTER INSERT ON `t%s`
FOR EACH ROW BEGIN # Обычно добавляем лишь одну запись
	DECLARE done INT DEFAULT 0; # Переменная для проверки выхода указателя за пределы курсора
	DECLARE prmid INT DEFAULT (SELECT id FROM tparam WHERE tparam.t_name = %s); # Найдем id параметра который создали
	#DECLARE sensorsCount INT DEFAULT (SELECT t_sensors FROM tparam WHERE tparam.t_name = %s);
	DECLARE newstate INT;
	DECLARE actualMaxTemp, actualMinTemp float;
	DECLARE lastsavetime datetime;
	DECLARE cnstrid, tvalue, insens, direction INT;
	# Курсор для пробегания по всем включенным уставкам, применённым к параметру
	DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value, 
			constraints.cnstr_insens, constraints.cnstr_direction, tcnstr.cnstr_state
		FROM constraints join tcnstr 
		ON tcnstr.cnstr_id = constraints.id
		WHERE tcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;
	DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1; # Меняем состояние при выходе указателя за пределы курсора

	# COALESCE Return the first non-NULL argument
	# GREATEST Return the largest argument
	# LEAST Return the smallest argument
	# найдем последнее время сохранения
	SELECT savetime, 
			GREATEST(	
				COALESCE(t1, -70000), COALESCE(t2, -70000), COALESCE(t3, -70000), 
				COALESCE(t4, -70000), COALESCE(t5, -70000), COALESCE(t6, -70000),
				COALESCE(t7, -70000), COALESCE(t8, -70000),	COALESCE(t9, -70000), 
				COALESCE(t10, -70000), COALESCE(t11, -70000), COALESCE(t12, -70000),
				COALESCE(t13, -70000), COALESCE(t14, -70000), COALESCE(t15, -70000), 
				COALESCE(t16, -70000), COALESCE(t17, -70000), COALESCE(t18, -70000),
				COALESCE(t19, -70000), COALESCE(t20, -70000), COALESCE(t21, -70000),
				COALESCE(t22, -70000), COALESCE(t23, -70000), COALESCE(t24, -70000),
				COALESCE(t25, -70000), COALESCE(t26, -70000), COALESCE(t27, -70000), 
				COALESCE(t28, -70000), COALESCE(t29, -70000), 
				COALESCE(t30, -70000)) as max, 
			LEAST(	
				COALESCE(t1, 70000), COALESCE(t2, 70000), COALESCE(t3, 70000), 
				COALESCE(t4, 70000), COALESCE(t5, 70000), COALESCE(t6, 70000),
				COALESCE(t7, 70000), COALESCE(t8, 70000), COALESCE(t9, 70000), 
				COALESCE(t10, 70000), COALESCE(t11, 70000), COALESCE(t12, 70000),
				COALESCE(t13, 70000), COALESCE(t14, 70000),	COALESCE(t15, 70000), 
				COALESCE(t16, 70000), COALESCE(t17, 70000), COALESCE(t18, 70000),
				COALESCE(t19, 70000), COALESCE(t20, 70000),	COALESCE(t21, 70000), 
				COALESCE(t22, 70000), COALESCE(t23, 70000), COALESCE(t24, 70000),
				COALESCE(t25, 70000), COALESCE(t26, 70000),	COALESCE(t27, 70000), 
				COALESCE(t28, 70000), COALESCE(t29, 70000), 
				COALESCE(t30, 70000)) as min 
		INTO lastsavetime, actualMaxTemp, actualMinTemp
		FROM t%s
		ORDER BY savetime DESC
		LIMIT 1;

	OPEN cur;
	#найдем применённые к параметру уставки
	REPEAT
		FETCH cur INTO cnstrid, tvalue, insens, direction, newstate;
		IF NOT done THEN
			# найдем состояние уставки
			IF direction > 0 THEN #По превышению
				IF actualMaxTemp >= tvalue THEN 
					SET newstate = 1;
				ELSE 
					IF actualMaxTemp <= tvalue-insens THEN 
						SET newstate = 0;
					END IF;
				END IF;
			END IF;
			IF direction < 0 THEN #По занижению
				IF actualMinTemp <= tvalue THEN 
					SET newstate = 1;
				ELSE
					IF actualMinTemp >= tvalue+insens THEN 
						SET newstate = 0;
					END IF;
				END IF;
			END IF;
		END IF;
		UPDATE tcnstr 
			SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime
			WHERE tcnstr.prm_id = prmid AND tcnstr.cnstr_id = cnstrid;
	UNTIL done END REPEAT;
	CLOSE cur;
END;*/

# Текущее состояние 0 - Всё хорошо 1 - уставка сработала
# Триггер обновляет состояние и время последних поступивших данных в таблице lcnstr, связывающей параметры уровня и уставки
# Данные берутся последние в таблине, а не поступившие
# Для каждой связи параметр-уставка, если уставок включена обновляем данные 
/*CREATE TRIGGER `update_cnstr_l%s` AFTER INSERT ON `l%s`
FOR EACH ROW BEGIN # Обычно добавляем лишь одну запись
	DECLARE done INT DEFAULT 0; # Переменная для проверки выхода указателя за пределы курсора
	DECLARE prmid INT DEFAULT (SELECT id FROM lparam WHERE lparam.l_name = %s); # Найдем id параметра который создали
	DECLARE newstate INT;
	DECLARE actuallevel float;
	DECLARE lastsavetime datetime;
	DECLARE cnstrid, lvalue, insens, direction INT;
	# Курсор для пробегания по всем включенным уставкам, применённым к параметру
	DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value, 
			constraints.cnstr_insens, constraints.cnstr_direction, lcnstr.cnstr_state
		FROM constraints join lcnstr 
		ON lcnstr.cnstr_id = constraints.id
		WHERE lcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;
	DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1; # Меняем состояние при выходе указателя за пределы курсора
		
	# найдем последнее время сохранения
	SELECT savetime, l
		INTO lastsavetime, actuallevel
		FROM %s
		ORDER BY savetime DESC
		LIMIT 1;
		
	OPEN cur;
	#найдем применённые к параметру уставки
	REPEAT
		FETCH cur INTO cnstrid, lvalue, insens, direction, newstate;
		IF NOT done THEN
			# найдем состояние уставки
			IF direction > 0 THEN #По превышению
				IF actuallevel >= lvalue THEN 
					SET newstate = 1;
				ELSE
					IF actuallevel <= lvalue-insens THEN 
						SET newstate = 0;
					END IF;
				END IF;
			END IF;
			IF direction < 0 THEN #По занижению
				IF actuallevel <= lvalue THEN 
					SET newstate = 1;
				ELSE
					IF actuallevel >= lvalue+insens THEN 
						SET newstate = 0;
					END IF;
				END IF;
			END IF;
		END IF;
		UPDATE lcnstr 
			SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime
			WHERE lcnstr.prm_id = prmid AND lcnstr.cnstr_id = cnstrid;
	UNTIL done END REPEAT;
	CLOSE cur;
END;*/


--
-- Дамп данных таблицы `tree`
--

INSERT INTO `tree` (`id`, `node_name`, `left_ix`, `right_ix`) VALUES
(1, 'Силос 2', 2, 3),
(2, 'Силос 1', 1, 4),
(13, 'Силос 3', 5, 6);

--
-- Дамп данных таблицы `constraints`
--

INSERT INTO `constraints` (`id`, `cnstr_name`, `cnstr_using`, `cnstr_value`, `cnstr_insens`, `cnstr_direction`) VALUES
(1, 'Критическая температура', 1, 35, 1, 1),
(2, 'Критическая температура нижняя', 1, 0, 1, -1),
(3, 'Критический уровень верх', 2, 18, 1, 1),
(4, 'Критический уровень низ', 2, 2, 1, -1);

--
-- Дамп данных таблицы `lddownparam`
--

INSERT INTO `lddownparam` (`id`, `ld_alias`, `ld_name`, `ld_parent`) VALUES
(1, '001_01', '001_01', 2),
(2, '001_02', '001_02', 2);

--
-- Дамп данных таблицы `ldupparam`
--

INSERT INTO `ldupparam` (`id`, `ld_alias`, `ld_name`, `ld_parent`) VALUES
(1, '001_01', '001_01', 2);


--
-- Дамп данных таблицы `lparam`
--

INSERT INTO `lparam` (`id`, `l_alias`, `l_name`, `l_parent`, `l_range`) VALUES
(1, '001_01', '001_01', 2, 20),
(2, '001_02', '001_02', 2, 20),
(3, '002_01', '002_01', 1, 20);
#(4, '002_02', '002_02', 1, 20),
#(5, '003_01', '003_01', 13, 20),
#(6, '003_02', '003_02', 13, 20);

--
-- Дамп данных таблицы `tparam`
--

INSERT INTO `tparam` (`id`, `t_alias`, `t_name`, `t_parent`, `t_sensors`) VALUES
(1, '001_01', '001_01', 2, 20),
(2, '001_02', '001_02', 2, 20),
(3, '002_01', '002_01', 1, 20);
#(4, '002_02', '002_02', 1, 20),
#(5, '003_01', '003_01', 13, 20),
#(6, '003_02', '003_02', 13, 20);

--
-- Структура таблицы `l001_01`
--

CREATE TABLE `l001_01` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `l` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `l001_01`
--

INSERT INTO `l001_01` (`id`, `savetime`, `l`) VALUES
(1, '2020-01-01 07:00:00', 0.0),
(2, '2020-01-01 07:05:00', 5),
(3, '2020-01-01 07:10:00', 0.0),
(4, '2020-01-01 07:15:00', 5),
(5, '2020-01-01 07:20:00', 20),
(6, '2020-01-01 07:25:00', 10),
(7, '2020-01-01 07:30:00', 20);

--
-- Триггеры `l001_01`
--
DELIMITER $$
CREATE TRIGGER `update_cnstr_l001_01` AFTER INSERT ON `l001_01` FOR EACH ROW BEGIN   DECLARE done INT DEFAULT 0;   DECLARE prmid INT DEFAULT (SELECT id FROM lparam WHERE lparam.l_name = "001_01");   DECLARE newstate INT;   DECLARE actuallevel float;   DECLARE lastsavetime datetime;   DECLARE cnstrid, lvalue, insens, direction INT;   DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value,        constraints.cnstr_insens, constraints.cnstr_direction, lcnstr.cnstr_state     FROM constraints join lcnstr      ON lcnstr.cnstr_id = constraints.id     WHERE lcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;   DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1;   SELECT savetime, l     INTO lastsavetime, actuallevel     FROM l001_01     ORDER BY savetime DESC     LIMIT 1;        OPEN cur;   REPEAT     FETCH cur INTO cnstrid, lvalue, insens, direction, newstate;     IF NOT done THEN       IF direction > 0 THEN         IF actuallevel >= lvalue THEN            SET newstate = 1;         ELSE           IF actuallevel <= lvalue-insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;       IF direction < 0 THEN         IF actuallevel <= lvalue THEN            SET newstate = 1;         ELSE           IF actuallevel >= lvalue+insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;     END IF;     UPDATE lcnstr        SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime       WHERE lcnstr.prm_id = prmid AND lcnstr.cnstr_id = cnstrid;   UNTIL done END REPEAT;   CLOSE cur; END
$$
DELIMITER ;

--
-- Структура таблицы `l001_02`
--

CREATE TABLE `l001_02` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `l` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `l001_02`
--

INSERT INTO `l001_02` (`id`, `savetime`, `l`) VALUES
(1, '2020-01-01 07:00:00', 0.0),
(2, '2020-01-01 07:05:00', 5),
(3, '2020-01-01 07:10:00', 0.0),
(4, '2020-01-01 07:15:00', 5),
(5, '2020-01-01 07:20:00', 20),
(6, '2020-01-01 07:25:00', 10),
(7, '2020-01-01 07:30:00', 20);

--
-- Триггеры `l001_02`
--
DELIMITER $$
CREATE TRIGGER `update_cnstr_l001_02` AFTER INSERT ON `l001_02` FOR EACH ROW BEGIN   DECLARE done INT DEFAULT 0;   DECLARE prmid INT DEFAULT (SELECT id FROM lparam WHERE lparam.l_name = "001_02");   DECLARE newstate INT;   DECLARE actuallevel float;   DECLARE lastsavetime datetime;   DECLARE cnstrid, lvalue, insens, direction INT;   DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value,        constraints.cnstr_insens, constraints.cnstr_direction, lcnstr.cnstr_state     FROM constraints join lcnstr      ON lcnstr.cnstr_id = constraints.id     WHERE lcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;   DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1;   SELECT savetime, l     INTO lastsavetime, actuallevel     FROM l001_02     ORDER BY savetime DESC     LIMIT 1;        OPEN cur;   REPEAT     FETCH cur INTO cnstrid, lvalue, insens, direction, newstate;     IF NOT done THEN       IF direction > 0 THEN         IF actuallevel >= lvalue THEN            SET newstate = 1;         ELSE           IF actuallevel <= lvalue-insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;       IF direction < 0 THEN         IF actuallevel <= lvalue THEN            SET newstate = 1;         ELSE           IF actuallevel >= lvalue+insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;     END IF;     UPDATE lcnstr        SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime       WHERE lcnstr.prm_id = prmid AND lcnstr.cnstr_id = cnstrid;   UNTIL done END REPEAT;   CLOSE cur; END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Структура таблицы `l002_01`
--

CREATE TABLE `l002_01` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `l` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `l002_01`
--

INSERT INTO `l002_01` (`id`, `savetime`, `l`) VALUES
(1, '2020-01-01 07:00:00', 0.0),
(2, '2020-01-01 07:05:00', 5),
(3, '2020-01-01 07:10:00', 0.0),
(4, '2020-01-01 07:15:00', 5),
(5, '2020-01-01 07:20:00', 20),
(6, '2020-01-01 07:25:00', 10),
(7, '2020-01-01 07:30:00', 20);

--
-- Триггеры `l002_01`
--
DELIMITER $$
CREATE TRIGGER `update_cnstr_l002_01` AFTER INSERT ON `l002_01` FOR EACH ROW BEGIN   DECLARE done INT DEFAULT 0;   DECLARE prmid INT DEFAULT (SELECT id FROM lparam WHERE lparam.l_name = "002_01");   DECLARE newstate INT;   DECLARE actuallevel float;   DECLARE lastsavetime datetime;   DECLARE cnstrid, lvalue, insens, direction INT;   DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value,        constraints.cnstr_insens, constraints.cnstr_direction, lcnstr.cnstr_state     FROM constraints join lcnstr      ON lcnstr.cnstr_id = constraints.id     WHERE lcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;   DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1;   SELECT savetime, l     INTO lastsavetime, actuallevel     FROM l002_01     ORDER BY savetime DESC     LIMIT 1;        OPEN cur;   REPEAT     FETCH cur INTO cnstrid, lvalue, insens, direction, newstate;     IF NOT done THEN       IF direction > 0 THEN         IF actuallevel >= lvalue THEN            SET newstate = 1;         ELSE           IF actuallevel <= lvalue-insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;       IF direction < 0 THEN         IF actuallevel <= lvalue THEN            SET newstate = 1;         ELSE           IF actuallevel >= lvalue+insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;     END IF;     UPDATE lcnstr        SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime       WHERE lcnstr.prm_id = prmid AND lcnstr.cnstr_id = cnstrid;   UNTIL done END REPEAT;   CLOSE cur; END
$$
DELIMITER ;

--
-- Структура таблицы `ldup001_01`
--

CREATE TABLE `ldup001_01` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `ld_up` tinyint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `ldup001_01`
--

INSERT INTO `ldup001_01` (`id`, `savetime`, `ld_up`) VALUES
(1, '2020-01-01 07:00:00', 0),
(2, '2020-01-01 07:05:00', 0),
(3, '2020-01-01 07:10:00', 0),
(4, '2020-01-01 07:15:00', 0),
(5, '2020-01-01 07:20:00', 1),
(6, '2020-01-01 07:25:00', 0),
(7, '2020-01-01 07:30:00', 1);

--
-- Структура таблицы `lddown001_01`
--

CREATE TABLE `lddown001_01` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `ld_down` tinyint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `lddown001_01`
--

INSERT INTO `lddown001_01` (`id`, `savetime`, `ld_down`) VALUES
(1, '2020-01-01 07:00:00', 1),
(2, '2020-01-01 07:05:00', 0),
(3, '2020-01-01 07:10:00', 1),
(4, '2020-01-01 07:15:00', 0),
(5, '2020-01-01 07:20:00', 0),
(6, '2020-01-01 07:35:00', 0),
(7, '2020-01-01 07:30:00', 0);

--
-- Структура таблицы `lddown001_02`
--

CREATE TABLE `lddown001_02` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `ld_down` tinyint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `lddown001_02`
--

INSERT INTO `lddown001_02` (`id`, `savetime`, `ld_down`) VALUES
(1, '2020-01-01 07:00:00', 1),
(2, '2020-01-01 07:05:00', 0),
(3, '2020-01-01 07:10:00', 1),
(4, '2020-01-01 07:15:00', 0),
(5, '2020-01-01 07:20:00', 0),
(6, '2020-01-01 07:35:00', 0),
(7, '2020-01-01 07:30:00', 0);

--
-- Структура таблицы `t001_01`
--

CREATE TABLE `t001_01` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `t1` float DEFAULT NULL,
  `t2` float DEFAULT NULL,
  `t3` float DEFAULT NULL,
  `t4` float DEFAULT NULL,
  `t5` float DEFAULT NULL,
  `t6` float DEFAULT NULL,
  `t7` float DEFAULT NULL,
  `t8` float DEFAULT NULL,
  `t9` float DEFAULT NULL,
  `t10` float DEFAULT NULL,
  `t11` float DEFAULT NULL,
  `t12` float DEFAULT NULL,
  `t13` float DEFAULT NULL,
  `t14` float DEFAULT NULL,
  `t15` float DEFAULT NULL,
  `t16` float DEFAULT NULL,
  `t17` float DEFAULT NULL,
  `t18` float DEFAULT NULL,
  `t19` float DEFAULT NULL,
  `t20` float DEFAULT NULL,
  `t21` float DEFAULT NULL,
  `t22` float DEFAULT NULL,
  `t23` float DEFAULT NULL,
  `t24` float DEFAULT NULL,
  `t25` float DEFAULT NULL,
  `t26` float DEFAULT NULL,
  `t27` float DEFAULT NULL,
  `t28` float DEFAULT NULL,
  `t29` float DEFAULT NULL,
  `t30` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `t001_01`
--

INSERT INTO `t001_01` (`id`, `savetime`, `t1`, `t2`, `t3`, `t4`, `t5`, `t6`, `t7`, `t8`, `t9`, `t10`, `t11`, `t12`, `t13`, `t14`, `t15`, `t16`, `t17`, `t18`, `t19`, `t20`, `t21`, `t22`, `t23`, `t24`, `t25`, `t26`, `t27`, `t28`, `t29`, `t30`) VALUES
(1, '2020-01-01 07:00:00', 18.1, 35.9, 25.2, 29, 12.3, 8.5, 20, 4.5, 26.4, 10.5, 20.3, 14.9, 35.4, 33.2, 24.2, 16.3, 33.5, 4.7, 12.8, 21.1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(2, '2020-01-01 07:15:00', 18.1, 35.9, 25.2, 29, 12.3, 8.5, 20, 4.5, 26.4, 10.5, 20.3, 14.9, 35.4, 33.2, 24.2, 16.3, 33.5, 4.7, 12.8, 21.1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(3, '2020-01-01 07:30:00', 18.1, 35.9, 25.2, 29, 12.3, 8.5, 20, 4.5, 26.4, 10.5, 20.3, 14.9, 35.4, 33.2, 24.2, 16.3, 33.5, 4.7, 12.8, 21.1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

--
-- Триггеры `t001_01`
--
DELIMITER $$
CREATE TRIGGER `update_cnstr_t001_01` AFTER INSERT ON `t001_01` FOR EACH ROW BEGIN   DECLARE done INT DEFAULT 0;   DECLARE prmid INT DEFAULT (SELECT id FROM tparam WHERE tparam.t_name = "001_01");   DECLARE newstate INT;   DECLARE actualMaxTemp, actualMinTemp float;   DECLARE lastsavetime datetime;   DECLARE cnstrid, tvalue, insens, direction INT;   DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value,        constraints.cnstr_insens, constraints.cnstr_direction, tcnstr.cnstr_state     FROM constraints join tcnstr      ON tcnstr.cnstr_id = constraints.id     WHERE tcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;   DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1;   SELECT savetime,        GREATEST(           COALESCE(t1, -70000), COALESCE(t2, -70000), COALESCE(t3, -70000),          COALESCE(t4, -70000), COALESCE(t5, -70000), COALESCE(t6, -70000),         COALESCE(t7, -70000), COALESCE(t8, -70000),  COALESCE(t9, -70000),          COALESCE(t10, -70000), COALESCE(t11, -70000), COALESCE(t12, -70000),         COALESCE(t13, -70000), COALESCE(t14, -70000), COALESCE(t15, -70000),          COALESCE(t16, -70000), COALESCE(t17, -70000), COALESCE(t18, -70000),         COALESCE(t19, -70000), COALESCE(t20, -70000), COALESCE(t21, -70000),         COALESCE(t22, -70000), COALESCE(t23, -70000), COALESCE(t24, -70000),         COALESCE(t25, -70000), COALESCE(t26, -70000), COALESCE(t27, -70000),          COALESCE(t28, -70000), COALESCE(t29, -70000),          COALESCE(t30, -70000)) as max,        LEAST(           COALESCE(t1, 70000), COALESCE(t2, 70000), COALESCE(t3, 70000),          COALESCE(t4, 70000), COALESCE(t5, 70000), COALESCE(t6, 70000),         COALESCE(t7, 70000), COALESCE(t8, 70000), COALESCE(t9, 70000),          COALESCE(t10, 70000), COALESCE(t11, 70000), COALESCE(t12, 70000),         COALESCE(t13, 70000), COALESCE(t14, 70000),  COALESCE(t15, 70000),          COALESCE(t16, 70000), COALESCE(t17, 70000), COALESCE(t18, 70000),         COALESCE(t19, 70000), COALESCE(t20, 70000),  COALESCE(t21, 70000),          COALESCE(t22, 70000), COALESCE(t23, 70000), COALESCE(t24, 70000),         COALESCE(t25, 70000), COALESCE(t26, 70000),  COALESCE(t27, 70000),          COALESCE(t28, 70000), COALESCE(t29, 70000),          COALESCE(t30, 70000)) as min      INTO lastsavetime, actualMaxTemp, actualMinTemp     FROM t001_01     ORDER BY savetime DESC     LIMIT 1;    OPEN cur;   REPEAT     FETCH cur INTO cnstrid, tvalue, insens, direction, newstate;     IF NOT done THEN       IF direction > 0 THEN         IF actualMaxTemp >= tvalue THEN            SET newstate = 1;         ELSE            IF actualMaxTemp <= tvalue-insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;       IF direction < 0 THEN         IF actualMinTemp <= tvalue THEN            SET newstate = 1;         ELSE           IF actualMinTemp >= tvalue+insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;     END IF;     UPDATE tcnstr        SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime       WHERE tcnstr.prm_id = prmid AND tcnstr.cnstr_id = cnstrid;   UNTIL done END REPEAT;   CLOSE cur; END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Структура таблицы `t001_02`
--

CREATE TABLE `t001_02` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `t1` float DEFAULT NULL,
  `t2` float DEFAULT NULL,
  `t3` float DEFAULT NULL,
  `t4` float DEFAULT NULL,
  `t5` float DEFAULT NULL,
  `t6` float DEFAULT NULL,
  `t7` float DEFAULT NULL,
  `t8` float DEFAULT NULL,
  `t9` float DEFAULT NULL,
  `t10` float DEFAULT NULL,
  `t11` float DEFAULT NULL,
  `t12` float DEFAULT NULL,
  `t13` float DEFAULT NULL,
  `t14` float DEFAULT NULL,
  `t15` float DEFAULT NULL,
  `t16` float DEFAULT NULL,
  `t17` float DEFAULT NULL,
  `t18` float DEFAULT NULL,
  `t19` float DEFAULT NULL,
  `t20` float DEFAULT NULL,
  `t21` float DEFAULT NULL,
  `t22` float DEFAULT NULL,
  `t23` float DEFAULT NULL,
  `t24` float DEFAULT NULL,
  `t25` float DEFAULT NULL,
  `t26` float DEFAULT NULL,
  `t27` float DEFAULT NULL,
  `t28` float DEFAULT NULL,
  `t29` float DEFAULT NULL,
  `t30` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `t001_02`
--

INSERT INTO `t001_02` (`id`, `savetime`, `t1`, `t2`, `t3`, `t4`, `t5`, `t6`, `t7`, `t8`, `t9`, `t10`, `t11`, `t12`, `t13`, `t14`, `t15`, `t16`, `t17`, `t18`, `t19`, `t20`, `t21`, `t22`, `t23`, `t24`, `t25`, `t26`, `t27`, `t28`, `t29`, `t30`) VALUES
(1, '2020-01-01 07:00:00', 35.5, 35, 34.4, 27.9, 35.5, 15.7, 14.5, 29.6, 6.8, 29.8, 11.9, 10, 10.2, 9.1, 4.9, 26.7, 21.2, 4.9, 7.9, 5.9, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(2, '2020-01-01 07:15:00', 35.5, 35, 34.4, 27.9, 35.5, 15.7, 14.5, 29.6, 6.8, 29.8, 11.9, 10, 10.2, 9.1, 4.9, 26.7, 21.2, 4.9, 7.9, 5.9, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(3, '2020-01-01 07:30:00', 35.5, 35, 34.4, 27.9, 35.5, 15.7, 14.5, 29.6, 6.8, 29.8, 11.9, 10, 10.2, 9.1, 4.9, 26.7, 21.2, 4.9, 7.9, 5.9, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

--
-- Триггеры `t001_02`
--
DELIMITER $$
CREATE TRIGGER `update_cnstr_t001_02` AFTER INSERT ON `t001_02` FOR EACH ROW BEGIN   DECLARE done INT DEFAULT 0;   DECLARE prmid INT DEFAULT (SELECT id FROM tparam WHERE tparam.t_name = "001_02");   DECLARE newstate INT;   DECLARE actualMaxTemp, actualMinTemp float;   DECLARE lastsavetime datetime;   DECLARE cnstrid, tvalue, insens, direction INT;   DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value,        constraints.cnstr_insens, constraints.cnstr_direction, tcnstr.cnstr_state     FROM constraints join tcnstr      ON tcnstr.cnstr_id = constraints.id     WHERE tcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;   DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1;   SELECT savetime,        GREATEST(           COALESCE(t1, -70000), COALESCE(t2, -70000), COALESCE(t3, -70000),          COALESCE(t4, -70000), COALESCE(t5, -70000), COALESCE(t6, -70000),         COALESCE(t7, -70000), COALESCE(t8, -70000),  COALESCE(t9, -70000),          COALESCE(t10, -70000), COALESCE(t11, -70000), COALESCE(t12, -70000),         COALESCE(t13, -70000), COALESCE(t14, -70000), COALESCE(t15, -70000),          COALESCE(t16, -70000), COALESCE(t17, -70000), COALESCE(t18, -70000),         COALESCE(t19, -70000), COALESCE(t20, -70000), COALESCE(t21, -70000),         COALESCE(t22, -70000), COALESCE(t23, -70000), COALESCE(t24, -70000),         COALESCE(t25, -70000), COALESCE(t26, -70000), COALESCE(t27, -70000),          COALESCE(t28, -70000), COALESCE(t29, -70000),          COALESCE(t30, -70000)) as max,        LEAST(           COALESCE(t1, 70000), COALESCE(t2, 70000), COALESCE(t3, 70000),          COALESCE(t4, 70000), COALESCE(t5, 70000), COALESCE(t6, 70000),         COALESCE(t7, 70000), COALESCE(t8, 70000), COALESCE(t9, 70000),          COALESCE(t10, 70000), COALESCE(t11, 70000), COALESCE(t12, 70000),         COALESCE(t13, 70000), COALESCE(t14, 70000),  COALESCE(t15, 70000),          COALESCE(t16, 70000), COALESCE(t17, 70000), COALESCE(t18, 70000),         COALESCE(t19, 70000), COALESCE(t20, 70000),  COALESCE(t21, 70000),          COALESCE(t22, 70000), COALESCE(t23, 70000), COALESCE(t24, 70000),         COALESCE(t25, 70000), COALESCE(t26, 70000),  COALESCE(t27, 70000),          COALESCE(t28, 70000), COALESCE(t29, 70000),          COALESCE(t30, 70000)) as min      INTO lastsavetime, actualMaxTemp, actualMinTemp     FROM t001_02     ORDER BY savetime DESC     LIMIT 1;    OPEN cur;   REPEAT     FETCH cur INTO cnstrid, tvalue, insens, direction, newstate;     IF NOT done THEN       IF direction > 0 THEN         IF actualMaxTemp >= tvalue THEN            SET newstate = 1;         ELSE            IF actualMaxTemp <= tvalue-insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;       IF direction < 0 THEN         IF actualMinTemp <= tvalue THEN            SET newstate = 1;         ELSE           IF actualMinTemp >= tvalue+insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;     END IF;     UPDATE tcnstr        SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime       WHERE tcnstr.prm_id = prmid AND tcnstr.cnstr_id = cnstrid;   UNTIL done END REPEAT;   CLOSE cur; END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Структура таблицы `t002_01`
--

CREATE TABLE `t002_01` (
  `id` int NOT NULL,
  `savetime` datetime NOT NULL,
  `t1` float DEFAULT NULL,
  `t2` float DEFAULT NULL,
  `t3` float DEFAULT NULL,
  `t4` float DEFAULT NULL,
  `t5` float DEFAULT NULL,
  `t6` float DEFAULT NULL,
  `t7` float DEFAULT NULL,
  `t8` float DEFAULT NULL,
  `t9` float DEFAULT NULL,
  `t10` float DEFAULT NULL,
  `t11` float DEFAULT NULL,
  `t12` float DEFAULT NULL,
  `t13` float DEFAULT NULL,
  `t14` float DEFAULT NULL,
  `t15` float DEFAULT NULL,
  `t16` float DEFAULT NULL,
  `t17` float DEFAULT NULL,
  `t18` float DEFAULT NULL,
  `t19` float DEFAULT NULL,
  `t20` float DEFAULT NULL,
  `t21` float DEFAULT NULL,
  `t22` float DEFAULT NULL,
  `t23` float DEFAULT NULL,
  `t24` float DEFAULT NULL,
  `t25` float DEFAULT NULL,
  `t26` float DEFAULT NULL,
  `t27` float DEFAULT NULL,
  `t28` float DEFAULT NULL,
  `t29` float DEFAULT NULL,
  `t30` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `t002_01`
--

INSERT INTO `t002_01` (`id`, `savetime`, `t1`, `t2`, `t3`, `t4`, `t5`, `t6`, `t7`, `t8`, `t9`, `t10`, `t11`, `t12`, `t13`, `t14`, `t15`, `t16`, `t17`, `t18`, `t19`, `t20`, `t21`, `t22`, `t23`, `t24`, `t25`, `t26`, `t27`, `t28`, `t29`, `t30`) VALUES
(1, '2020-01-01 07:00:00', 31.9, 5.4, 5.8, 17.8, 15.5, 16.8, 15.9, 13.4, 12.2, 26, 5.2, 31.8, 34.5, 10.8, 24.4, 23, 18.1, 13.6, 32.9, 33.7, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(2, '2020-01-01 07:15:00', 31.9, 5.4, 5.8, 17.8, 15.5, 16.8, 15.9, 13.4, 12.2, 26, 5.2, 31.8, 34.5, 10.8, 24.4, 23, 18.1, 13.6, 32.9, 33.7, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(3, '2020-01-01 07:30:00', 31.9, 5.4, 5.8, 17.8, 15.5, 16.8, 15.9, 13.4, 12.2, 26, 5.2, 31.8, 34.5, 10.8, 24.4, 23, 18.1, 13.6, 32.9, 33.7, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

--
-- Триггеры `t002_01`
--
DELIMITER $$
CREATE TRIGGER `update_cnstr_t002_01` AFTER INSERT ON `t002_01` FOR EACH ROW BEGIN   DECLARE done INT DEFAULT 0;   DECLARE prmid INT DEFAULT (SELECT id FROM tparam WHERE tparam.t_name = "002_01");   DECLARE newstate INT;   DECLARE actualMaxTemp, actualMinTemp float;   DECLARE lastsavetime datetime;   DECLARE cnstrid, tvalue, insens, direction INT;   DECLARE cur CURSOR FOR SELECT constraints.id, constraints.cnstr_value,        constraints.cnstr_insens, constraints.cnstr_direction, tcnstr.cnstr_state     FROM constraints join tcnstr      ON tcnstr.cnstr_id = constraints.id     WHERE tcnstr.prm_id = prmid AND constraints.cnstr_direction != 0 AND constraints.cnstr_using != 0;   DECLARE CONTINUE HANDLER FOR SQLSTATE "02000" SET done = 1;   SELECT savetime,        GREATEST(           COALESCE(t1, -70000), COALESCE(t2, -70000), COALESCE(t3, -70000),          COALESCE(t4, -70000), COALESCE(t5, -70000), COALESCE(t6, -70000),         COALESCE(t7, -70000), COALESCE(t8, -70000),  COALESCE(t9, -70000),          COALESCE(t10, -70000), COALESCE(t11, -70000), COALESCE(t12, -70000),         COALESCE(t13, -70000), COALESCE(t14, -70000), COALESCE(t15, -70000),          COALESCE(t16, -70000), COALESCE(t17, -70000), COALESCE(t18, -70000),         COALESCE(t19, -70000), COALESCE(t20, -70000), COALESCE(t21, -70000),         COALESCE(t22, -70000), COALESCE(t23, -70000), COALESCE(t24, -70000),         COALESCE(t25, -70000), COALESCE(t26, -70000), COALESCE(t27, -70000),          COALESCE(t28, -70000), COALESCE(t29, -70000),          COALESCE(t30, -70000)) as max,        LEAST(           COALESCE(t1, 70000), COALESCE(t2, 70000), COALESCE(t3, 70000),          COALESCE(t4, 70000), COALESCE(t5, 70000), COALESCE(t6, 70000),         COALESCE(t7, 70000), COALESCE(t8, 70000), COALESCE(t9, 70000),          COALESCE(t10, 70000), COALESCE(t11, 70000), COALESCE(t12, 70000),         COALESCE(t13, 70000), COALESCE(t14, 70000),  COALESCE(t15, 70000),          COALESCE(t16, 70000), COALESCE(t17, 70000), COALESCE(t18, 70000),         COALESCE(t19, 70000), COALESCE(t20, 70000),  COALESCE(t21, 70000),          COALESCE(t22, 70000), COALESCE(t23, 70000), COALESCE(t24, 70000),         COALESCE(t25, 70000), COALESCE(t26, 70000),  COALESCE(t27, 70000),          COALESCE(t28, 70000), COALESCE(t29, 70000),          COALESCE(t30, 70000)) as min      INTO lastsavetime, actualMaxTemp, actualMinTemp     FROM t002_01     ORDER BY savetime DESC     LIMIT 1;    OPEN cur;   REPEAT     FETCH cur INTO cnstrid, tvalue, insens, direction, newstate;     IF NOT done THEN       IF direction > 0 THEN         IF actualMaxTemp >= tvalue THEN            SET newstate = 1;         ELSE            IF actualMaxTemp <= tvalue-insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;       IF direction < 0 THEN         IF actualMinTemp <= tvalue THEN            SET newstate = 1;         ELSE           IF actualMinTemp >= tvalue+insens THEN              SET newstate = 0;           END IF;         END IF;       END IF;     END IF;     UPDATE tcnstr        SET cnstr_state = newstate, cnstr_last_savetime = lastsavetime       WHERE tcnstr.prm_id = prmid AND tcnstr.cnstr_id = cnstrid;   UNTIL done END REPEAT;   CLOSE cur; END
$$
DELIMITER ;
