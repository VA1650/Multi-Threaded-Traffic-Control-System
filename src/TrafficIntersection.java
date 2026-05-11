import java.util.concurrent.*;


public class TrafficIntersection {
    public static void main(String[] args) {
        TrafficLightController controller = new TrafficLightController();
        // Используем пул потоков для управления всеми участниками движения
        try (ExecutorService executor = Executors.newFixedThreadPool(30)) {

            // Запуск контроллера светофора
            executor.execute(new TrafficLight(controller));

            // Запуск полос движения
            String[] directions = {"Север", "Юг", "Запад", "Восток"};
            for (String dir : directions) {
                executor.execute(new TrafficLane(dir, "Правая", controller));
                executor.execute(new TrafficLane(dir, "Левая", controller));
            }

            // Добавляем потоки пешеходов (на четыре угла перекрестка)
            executor.execute(new PedestrianGroup("Северо-Запад"));
            executor.execute(new PedestrianGroup("Юго-Восток"));

            try {
                // Симуляция работает 3 минуты
                Thread.sleep(180_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("Завершение работы симулятора...");
            executor.shutdownNow();
        }
    }
}