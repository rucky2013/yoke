package com.jetdrone.vertx.yoke.middleware;

import com.jetdrone.vertx.yoke.Middleware;
import com.jetdrone.vertx.yoke.Yoke;
import org.jetbrains.annotations.NotNull;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

public final class TooBusy extends Middleware {

    private final long highWaterMark;
    private final String message;

    private long timerID;
    private long t0;
    private long dt;

    public TooBusy() {
        this(70);
    }

    public TooBusy(long highWaterMark) {
        this(highWaterMark, "Server is too busy. Please, try again later.");
    }

    public TooBusy(long highWaterMark, String message) {
        this.highWaterMark = highWaterMark;
        this.message = message;
    }

    @Override
    public Middleware init(@NotNull final Yoke yoke, @NotNull final String mount) {
        super.init(yoke, mount);

        t0 = System.currentTimeMillis();
        dt = 500;

        timerID = yoke.vertx().setPeriodic(500, new Handler<Long>() {
            @Override
            public void handle(Long timerID) {
                final long t1 = System.currentTimeMillis();
                dt = t1 - t0;
                t0 = t1;
            }
        });

        return this;
    }

    public long getLag() {
        return dt - 500;
    }

    public void shutdown() {
        yoke.vertx().cancelTimer(timerID);
    }

    @Override
    public void handle(@NotNull YokeRequest request, @NotNull Handler<Object> next) {
        if (dt - 500 > highWaterMark) {
            final YokeResponse response = request.response();

            response.setStatusCode(503);
            response.setStatusMessage(message);
            response.end();
        } else {
            next.handle(null);
        }
    }
}
