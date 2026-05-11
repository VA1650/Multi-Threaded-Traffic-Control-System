import java.util.Random;

class TrafficLane implements Runnable {
    private final String dir, lane;
    private final TrafficLightController controller;
    private final Random rand = new Random();

    public TrafficLane(String d, String l, TrafficLightController c) { this.dir = d; this.lane = l; this.controller = c; }

    @Override

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(rand.nextInt(5000));

                // ЖЕСТКАЯ ЛОГИКА ПОЛОС:
                String m;
                if (lane.equals("Правая")) {
                    // С правой полосы — только направо или прямо
                    m = rand.nextBoolean() ? "Направо" : "Прямо";
                } else {
                    // С левой полосы — прямо, налево или разворот
                    int r = rand.nextInt(3);
                    m = (r == 0) ? "Прямо" : (r == 1) ? "Налево" : "Разворот";
                }

                controller.waitForEntry(dir, lane, m);
                Thread.sleep(rand.nextInt(2000));
                controller.release(dir, lane, m);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}