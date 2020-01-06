package club.moddedminecraft.polychat.bukkitclient.threads;


import java.io.IOException;

public abstract class HeartbeatThread {

    private int interval;
    private Thread thread;

    public HeartbeatThread(int interval) {
        this.interval = interval;
        this.thread = new Thread(new Runnable(){
            @Override
            public void run(){
                runThread();
            }
        });
    }

    protected abstract void run() throws InterruptedException, IOException;

    public void start() {
        this.thread.start();
    }

    public void interrupt() {
        this.thread.interrupt();
    }

    private void runThread() {
        while (true) {
            try {
                run();
                Thread.sleep(this.interval);
            } catch (InterruptedException | IOException ignored) {
                System.out.println("Heartbeat thread " + this.getClass().getSimpleName() + "interrupted, stopping...");
            }
        }
    }

}