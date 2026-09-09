package app.controller;

import io.javalin.http.Context;

import java.util.Map;

public class SystemController {

    public void health(Context ctx) {
        ctx.status(200).json(Map.of("status", "ok"));
    }

//    public static void addRoutes(io.javalin.Javalin app) {
//        app.get("/", ctx -> ctx.render("/index"));
//    }
}
