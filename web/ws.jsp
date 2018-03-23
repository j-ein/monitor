<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:f="http://xmlns.jcp.org/jsf/core">
<script>
    var socket=null;
    function connect() {
        try{
            if(socket == null || socket.readyState > 1 ) {
                socket = new WebSocket('ws://127.0.0.1:9880');
            } else {
                alert('已经连接啦，请不要重复连接！');
            }
        }catch(e){
            alert('error:'+e.data);
            return;
        }
        var readyState = new Array("正在连接", "已建立连接", "正在关闭连接", "已关闭连接");
        socket.onopen = function(evt) { alert('connect success:'+readyState[socket.readyState]); };
        socket.onerror = function(evt) { alert('connect error:'+evt.data); };
        socket.onmessage = function(evt) { alert('server says:'+evt.data); };
        socket.onclose = function(evt) { alert('connect close:'+readyState[socket.readyState]); };
    }
    function send() {
        socket.send(document.getElementById('msg').value);
    }
    function disconnect() {
        socket.close();
    }
</script>
<body>
    <input id="msg" type="text">
    <button onclick="connect()">Connect</button>
    <button onclick="send()">Send</button>
    <button onclick="disconnect()">Disconnect</button>
</body>
</html>
