package com.heishandashu.tracker.client.diagnostic;

import com.heishandashu.tracker.TrackerMod;

import java.util.Map;

/**
 * Reports a stalled render thread instead of leaving a freeze unexplained.
 *
 * <p>A frozen client writes nothing to the log: the thread that would have logged is the one that
 * is stuck, so the last line is whatever happened to be printed before. This watchdog is a plain
 * daemon thread, independent of the render thread, that notices when the frame callback has not run
 * for a while and dumps every thread's stack. {@code jstack} does the same thing, but only if
 * someone is on the machine at the time and has a JDK to hand.</p>
 *
 * <p>It only reads state: no locks are taken that the game could be holding, and the dump is
 * written once per stall so a permanently hung client cannot flood the log.</p>
 */
public final class RenderWatchdog {

    /** How long the frame callback may be missing before the stall is reported. */
    private static final long STALL_MILLIS = 8000L;
    private static final long POLL_MILLIS = 2000L;

    private static volatile long lastBeat = System.currentTimeMillis();
    private static volatile boolean started;
    /** Cap on dumps, so a false positive cannot fill the log. */
    private static int dumps;

    private RenderWatchdog() {
    }

    /** Called once per frame from the HUD callback, before any feature decides to draw. */
    public static void beat() {
        lastBeat = System.currentTimeMillis();
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Thread thread = new Thread(RenderWatchdog::loop, "moreswitchtracker-watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop() {
        while (true) {
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            long stall = System.currentTimeMillis() - lastBeat;
            if (stall < STALL_MILLIS) {
                continue;
            }
            // Reset first: the dump itself can block on a frozen JVM, and a second dump is useless.
            lastBeat = System.currentTimeMillis();
            if (dumps++ < 3) {
                dump(stall);
            }
        }
    }

    private static void dump(long stallMillis) {
        StringBuilder builder = new StringBuilder();
        builder.append("render thread has not rendered for ").append(stallMillis)
                .append(" ms -- dumping thread stacks\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            builder.append("\n\"").append(thread.getName()).append("\" state=")
                    .append(thread.getState()).append('\n');
            for (StackTraceElement element : entry.getValue()) {
                builder.append("    at ").append(element).append('\n');
            }
        }
        TrackerMod.LOGGER.warn("[tracker] {}", builder);
    }
}
