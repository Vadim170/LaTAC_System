function websocketConnect() {
    var uri = "ws://" + document.location.host + "/api/v1/mnemoscheme/websocket";
    console.log(uri);
    var socket = new WebSocket(uri);
    socket.onopen = function() {
      console.log("Соединение установлено.");
    };

    socket.onclose = function(event) {
      var str = "" ;
      if (event.wasClean) {
        str = 'Соединение закрыто чисто';
      } else {
        str = 'Обрыв соединения'; // например, "убит" процесс сервера
      }
      alert(str + '\nКод: ' + event.code + ' причина: ' + event.reason);
    };


    socket.onmessage = function(event) {
        console.log("Получены данные [" + event.data + "]");
        var data = JSON.parse(event.data)
        data.forEach(function(item, i, arr) {
            if(item.type == "level") {
                setLevel(item.indication.level, item.groupId, item.paramIdentifer);
            }
        });
    };

    socket.onerror = function(error) {
      alert("Ошибка " + error.message);
    };
}

function setLevel(levelPercent, groupId, paramIdentifer) {
    //Поиск в мнемосхеме по id группы, едси найдено более двух элементов с подходящим типом,
    //то фильтрация по идентификатору параметра(В АСКТ это имя, АСКУ это id)
    var indicators = $('g[groupid|="' + groupId + '"]').find($('rect[indicatortype|="level"]'));
    var indicator = (indicators.length > 1)? indicators.filter($('rect[paramidentifer|="' + paramIdentifer + '"]'))[0] : indicators[0];
    var y = parseFloat(indicator.getAttribute("y"));
    var height = parseFloat(indicator.getAttribute("height"));
    var maxHeight = parseFloat(indicator.getAttribute("maxheight"));
    var bottom = y + height;
    var newHeight = maxHeight * levelPercent;
    var newY = bottom - newHeight;
    indicator.setAttribute("y", newY + "px");
    indicator.setAttribute("height", newHeight + "px");
}