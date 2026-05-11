import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class TrafficLightController {
    private final ReentrantLock lightLock = new ReentrantLock(true); // fair = true для честной очереди
    private final Condition lightCondition = lightLock.newCondition();

    private boolean isGreenNorthSouth = true;
    private boolean isPedestrianPhase = false; // НОВАЯ ФАЗА
    private int carsInIntersection = 0;

    private final Map<String, ReentrantLock> laneLocks = new HashMap<>();
    private final Map<String, Car> laneOccupied = new HashMap<>();
    private final List<Car> carsWaitingToFinish = new CopyOnWriteArrayList<>(); // Потокобезопасный список

    public TrafficLightController() {
        String[] directions = {"Север", "Юг", "Запад", "Восток"};
        String[] lanes = {"Правая", "Левая"};
        for (String direction : directions) {
            for (String lane : lanes) {
                laneLocks.put(direction + "_" + lane, new ReentrantLock(true));
            }
        }
    }

    public void waitForEntry(String direction, String lane, String maneuver) throws InterruptedException {
        String laneKey = direction + "_" + lane;
        ReentrantLock laneLock = laneLocks.get(laneKey);

        laneLock.lock();
        try {
            lightLock.lock();
            try {
                boolean isNorthSouth = direction.equals("Север") || direction.equals("Юг");

                // Машина ждет, если: идет пешеход ИЛИ свет не её направления
                while (isPedestrianPhase || isGreenNorthSouth != isNorthSouth) {
                    String reason = isPedestrianPhase ? "ПЕШЕХОДЫ ИДУТ" : "КРАСНЫЙ СВЕТ";
                    logStatus(direction, lane, maneuver, "Ждет (" + reason + ")");
                    lightCondition.await();
                }

                // Ожидание освобождения перекрестка от машин старой фазы
                while (hasConflictingOldCars(isGreenNorthSouth)) {
                    logStatus(direction, lane, maneuver, "Ждет завершения маневров старой фазы");
                    lightCondition.await();
                }

                // Вход на перекресток
                Car car = new Car(direction, lane, maneuver, isGreenNorthSouth);
                laneOccupied.put(laneKey, car);
                carsWaitingToFinish.add(car);
                carsInIntersection++;
                System.out.printf(">>> [%s, %s] ВЪЕХАЛ (%s). Машин на перекрестке: %d%n", direction, lane, maneuver, carsInIntersection);

            } finally {
                lightLock.unlock();
            }
        } finally {
            laneLock.unlock();
        }
    }

    public void release(String direction, String lane, String maneuver) throws InterruptedException {
        lightLock.lock();
        try {
            while (shouldYield(direction, maneuver)) {
                lightCondition.await();
            }

            String laneKey = direction + "_" + lane;
            Car car = laneOccupied.remove(laneKey);
            if (car != null) {
                carsWaitingToFinish.remove(car);
            }
            carsInIntersection--;
            // ИСПРАВЛЕНО: Теперь 3 плейсхолдера на 4 аргумента не ругаются
            System.out.printf("<<< [%s, %s, %s] ВЫЕХАЛ. Осталось на перекрестке: %d%n",
                    direction, lane, maneuver, carsInIntersection);

            lightCondition.signalAll();
        } finally {
            lightLock.unlock();
        }
    }

    public void switchLight() {
        lightLock.lock();
        try {
            // Чистая циклическая логика
            if (isPedestrianPhase) {
                isPedestrianPhase = false;
                isGreenNorthSouth = true;
            } else if (isGreenNorthSouth) {
                isGreenNorthSouth = false;
            } else {
                isPedestrianPhase = true;
            }

            System.out.println("\n=== СВЕТОФОР ПЕРЕКЛЮЧЕН ===");
            System.out.println("Статус: " + getStatusString());
            System.out.println("===========================\n");

            lightCondition.signalAll();
        } finally {
            lightLock.unlock();
        }
    }

    private String getStatusString() {
        if (isPedestrianPhase) return "ПЕШЕХОДНЫЙ ПЕРЕХОД (Всем стоять)";
        return isGreenNorthSouth ? "ЗЕЛЕНЫЙ: СЕВЕР-ЮГ" : "ЗЕЛЕНЫЙ: ЗАПАД-ВОСТОК";
    }

    private boolean hasConflictingOldCars(boolean currentNSGreen) {
        return carsWaitingToFinish.stream().anyMatch(c -> c.wasNSGreen != currentNSGreen);
    }

    private boolean shouldYield(String myDir, String myManeuver) {
        // Упрощенная логика: при повороте налево/развороте уступаем встречке
        if (!myManeuver.equals("Налево") && !myManeuver.equals("Разворот")) return false;

        String opposite = getOppositeDirection(myDir);
        return carsWaitingToFinish.stream().anyMatch(other ->
                other.direction.equals(opposite) &&
                        (other.maneuver.equals("Прямо") || other.maneuver.equals("Направо"))
        );
    }


    private String getOppositeDirection(String dir) {
        return switch (dir) {
            case "Север" -> "Юг";
            case "Юг" -> "Север";
            case "Запад" -> "Восток";
            case "Восток" -> "Запад";
            default -> "";
        };
    }

    private void logStatus(String dir, String lane, String maneuver, String status) {
        System.out.printf("[%s, %s, %s] %s%n", dir, lane, maneuver, status);
    }

    private record Car(String direction, String lane, String maneuver, boolean wasNSGreen) {}
}