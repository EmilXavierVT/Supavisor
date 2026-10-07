package app.controller;

import io.javalin.http.Context;

import java.util.Map;

public class SystemController {

    public void health(Context ctx) {
        ctx.status(200).json(Map.of("status", "ok"));
    }

    public void hello(Context ctx) {
        ctx.json(Map.of("msg", "Hello World"));
    }

    public void echo(Context ctx) {
        ctx.result(ctx.body());
    }

}
