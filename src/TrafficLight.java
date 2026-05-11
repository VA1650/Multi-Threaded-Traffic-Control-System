class TrafficLight implements Runnable {
    private final TrafficLightController controller;
    public TrafficLight(TrafficLightController c) { this.controller = c; }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10000); // Смена фазы каждые 10 секунд
                controller.switchLight();
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}