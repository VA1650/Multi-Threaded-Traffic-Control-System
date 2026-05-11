class PedestrianGroup implements Runnable {
    private final String location;

    public PedestrianGroup(String loc) { this.location = loc;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(8000); // Пешеходы подходят к переходу
                // В данной модели пешеходы просто логируются, так как их фаза управляется светофором
                System.out.println("[!] Группа пешеходов на " + location + " ожидает перехода...");
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}