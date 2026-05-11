# Multi-Threaded Traffic Control System

High-performance traffic intersection simulator written in Java, utilizing advanced concurrency primitives to model real-world traffic laws and pedestrian safety.

---

## 🚦 Overview

This project simulates a complex 4-way intersection with multiple lanes, dynamic traffic light phases, and pedestrian crossings. It solves the classic "producer-consumer" problem where lanes produce "car threads" that must safely traverse a shared resource (the intersection) without deadlocks or collisions.

## 🛠 Technical Stack

* **Language:** Java 21+
* **Concurrency:** `java.util.concurrent`
* **Synchronization:** `ReentrantLock` (Fair Mode), `Condition` variables.
* **Execution:** `ExecutorService` (Fixed Thread Pool).

## 🧠 Key Concurrency Features

### 1. Fair Scheduling
All intersection and lane locks are initialized with `fair = true`. This prevents **Thread Starvation**, ensuring that cars from a busy direction don't block side-street traffic indefinitely. Every car gets its turn based on its arrival time.

### 2. Three-Phase Traffic Logic
The system implements a cyclic state machine:
* **Phase 1:** North-South Green.
* **Phase 2:** West-East Green.
* **Phase 3:** **Pedestrian Exclusive Phase** (All-Red). All vehicle threads are suspended using `Condition.await()` while pedestrians "cross" the intersection.

### 3. Conflict Resolution (Yielding)
The `shouldYield()` algorithm implements real-world traffic rules:
* **Left Turn / U-Turn Priority:** Vehicle threads performing these maneuvers automatically check the `carsInIntersection` registry and yield to oncoming traffic moving `STRAIGHT` or `RIGHT`.
* **All-Red Clearance:** When a light changes, new threads wait for "stale" threads from the previous phase to exit the intersection before entering.

### 4. Thread-Safe Registry
The system uses a combination of `HashMap` (guarded by locks) and `CopyOnWriteArrayList` to manage the state of active vehicles, ensuring that status checks never throw a `ConcurrentModificationException`.

## 🏗 Architecture

* **`TrafficLightController`**: The "Brain". Manages locks, conditions, and the state of the intersection.
* **`TrafficLane` (Runnable)**: Simulates a specific lane (e.g., North-Left). Generates vehicles and manages their lifecycle (**Wait -> Entry -> Maneuver -> Exit**).
* **`TrafficLight` (Runnable)**: A background service that orchestrates phase switching.
* **`PedestrianGroup` (Runnable)**: High-priority entities that trigger wait conditions for all vehicle threads.

## 📈 Performance & Safety

* **Deadlock Prevention:** Implements a strict lock acquisition hierarchy (`laneLock` -> `lightLock`).
* **Resource Optimization:** Uses `Condition.signalAll()` to wake up only the necessary threads, reducing CPU context switching overhead.
* **Graceful Shutdown:** All threads respect the `interrupt()` signal, allowing for a clean termination of the simulation.

## 🚀 How to Run

```bash
javac TrafficIntersection.java
java TrafficIntersection
```

![image](https://github.com/user-attachments/assets/4f1ada7a-18af-452f-813a-4598862390e6)
