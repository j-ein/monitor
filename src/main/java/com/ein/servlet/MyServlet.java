package com.ein.servlet;

import com.ein.websocket.WebSocketServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by JiangFeng on 2016/6/21.
 */
public class MyServlet  extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text;html;charset=UTF-8");
        new WebSocketServer().service();
        resp.sendRedirect("/WEB-INF/ws.jsp");
    }
}
