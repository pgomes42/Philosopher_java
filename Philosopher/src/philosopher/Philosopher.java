package philosopher;

/**
 * Dining Philosophers Problem Implementation
 *
 * @author pgomes
 */
public class Philosopher extends Thread {

    private final int id;
    private final Table table;
    private final int leftForkIndex;
    private final int rightForkIndex;
    private int mealsEaten = 0;
    private final int maxMeals;

    // Tempos para as ações (em milissegundos)
    private static final int EATING_TIME = 2000;   // 2 segundos
    private static final int SLEEPING_TIME = 2000; // 1 segundo (reduzido)
    private static final int TIME_TO_DIE = 1000;   // 5 segundos sem comer = morte

    // Tempo inicial da simulação (compartilhado entre todos os filósofos)
    private static long startTime = 0;

    // Controle global da simulação
    private static volatile boolean simulationRunning = true;

    // Controle de vida dos filósofos
    private long lastMealTime;
    private boolean isAlive = true;

    // Thread para monitorar morte com precisão
    private Thread deathMonitor;

    // Método para inicializar o tempo de início
    public static void initializeStartTime() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
    }

    // Método para obter o tempo decorrido desde o início
    private long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public Philosopher(int id, Table table, int maxMeals) {
        this.id = id;
        this.table = table;
        this.maxMeals = maxMeals;
        this.leftForkIndex = id;
        this.rightForkIndex = (id + 1) % table.getNumberOfPhilosophers();
        this.lastMealTime = System.currentTimeMillis(); // Inicia com tempo atual

        // Inicia o monitor de morte
        startDeathMonitor();
    }

    // Monitor de morte que verifica a cada 100ms
    private void startDeathMonitor() {
        deathMonitor = new Thread(() -> {
            while (isAlive && simulationRunning) {
                try {
                    Thread.sleep(100); // Verifica a cada 100ms
                    if (shouldDie() && isAlive && simulationRunning) {
                        die();
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        deathMonitor.setDaemon(true); // Thread daemon para não impedir o programa de terminar
        deathMonitor.start();
    }

    // Método para verificar se o filósofo deve morrer
    private boolean shouldDie() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastMealTime) > TIME_TO_DIE;
    }

    // Método para marcar que o filósofo morreu
    private void die() {
        isAlive = false;
        simulationRunning = false; // Para toda a simulação
        long deathTime = getElapsedTime();
        System.out.println("[" + deathTime + "ms] ☠️ Philosopher " + id + " DIED from starvation!");
        System.out.println("[" + deathTime + "ms] 🚨 SIMULATION TERMINATED - A philosopher has died!");
    }

    @Override
    public void run() {
        try {
            startThinking();
            while (mealsEaten < maxMeals && isAlive && simulationRunning) {
                if (!simulationRunning) {
                    stopThinking();
                    break;
                }

                // Tenta comer (isso vai parar o pensamento)
                eat();

                if (!simulationRunning || !isAlive) {
                    break;
                }

                // Dorme depois de comer
                sleep();

                if (!simulationRunning || !isAlive) {
                    break;
                }

                // Volta a pensar depois de dormir
                if (isAlive && simulationRunning) {
                    startThinking();
                }

                mealsEaten++;
            }

            if (isAlive && simulationRunning) {
                stopThinking();
                long finishTime = getElapsedTime();
                System.out.println("[" + finishTime + "ms] ✅ Philosopher " + id + " finished eating " + maxMeals + " meals.");
            } else if (!simulationRunning || !isAlive) {
                stopThinking();
                long stopTime = getElapsedTime();
                System.out.println("[" + stopTime + "ms] ⏹️ Philosopher " + id + " stopped due to simulation termination.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Philosopher " + id + " was interrupted.");
        }
    }

    private void startThinking() {
        long currentTime = getElapsedTime();
        System.out.println("[" + currentTime + "ms] 🤔 Philosopher " + id + " started thinking...");
    }

    private void stopThinking() {
        long currentTime = getElapsedTime();
        System.out.println("[" + currentTime + "ms] 🤔 Philosopher " + id + " stopped thinking.");
    }

    private void sleep() throws InterruptedException {
        if (!simulationRunning) {
            return;
        }

        long currentTime = getElapsedTime();
        System.out.println("[" + currentTime + "ms] 😴 Philosopher " + id + " is sleeping for " + SLEEPING_TIME + "ms...");

        // Dorme em pequenos intervalos para poder verificar se a simulação parou
        long sleepStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - sleepStart < SLEEPING_TIME && simulationRunning) {
            Thread.sleep(100); // Dorme 100ms por vez
        }
    }

    private void eat() throws InterruptedException {
               stopThinking();

        // Verifica se a simulação ainda está rodando
        if (!simulationRunning) {
            return;
        }

        // Pick up forks (avoid deadlock by ordering)
        Fork firstFork, secondFork;
        if (leftForkIndex < rightForkIndex) {
            firstFork = table.getFork(leftForkIndex);
            secondFork = table.getFork(rightForkIndex);
        } else {
            firstFork = table.getFork(rightForkIndex);
            secondFork = table.getFork(leftForkIndex);
        }

        // Pick up first fork
        firstFork.waitForFork();
        if (!simulationRunning) {
            firstFork.putDown(); // Libera o garfo se a simulação parou
            return;
        }
        long pickupTime1 = getElapsedTime();
        System.out.println("[" + pickupTime1 + "ms] 🍴 Philosopher " + id + " picked up fork " + firstFork.getId());

        // Pick up second fork
        secondFork.waitForFork();
        if (!simulationRunning) {
            firstFork.putDown(); // Libera ambos os garfos se a simulação parou
            secondFork.putDown();
            return;
        }
        long pickupTime2 = getElapsedTime();
        System.out.println("[" + pickupTime2 + "ms] 🍴 Philosopher " + id + " picked up fork " + secondFork.getId());

        // Eat
        long eatTime = getElapsedTime();
        System.out.println("[" + eatTime + "ms] 🍽️ Philosopher " + id + " is eating for " + EATING_TIME + "ms... (meal " + (mealsEaten + 1) + ")");
        Thread.sleep(EATING_TIME);

        // Atualiza o tempo da última refeição
        lastMealTime = System.currentTimeMillis();

        // Put down forks
        firstFork.putDown();
        long putdownTime1 = getElapsedTime();
        System.out.println("[" + putdownTime1 + "ms] ⬇️ Philosopher " + id + " put down fork " + firstFork.getId());

        secondFork.putDown();
        long putdownTime2 = getElapsedTime();
        System.out.println("[" + putdownTime2 + "ms] ⬇️ Philosopher " + id + " put down fork " + secondFork.getId());
    }

    /**
     * Main method to run the Dining Philosophers simulation
     */
    public static void main(String[] args) {
        int numberOfPhilosophers = 5;
        int maxMealsPerPhilosopher = 3;

        System.out.println("🍽️ DINING PHILOSOPHERS WITH DEATH SYSTEM 🍽️");
        System.out.println("⚠️ Philosophers will DIE if they don't eat within " + TIME_TO_DIE + "ms!");
        System.out.println("🤔 Philosophers think continuously when not eating or sleeping");
        System.out.println("⏱️ Eating time: " + EATING_TIME + "ms");
        System.out.println("⏱️ Sleeping time: " + SLEEPING_TIME + "ms");
        System.out.println("🎯 Target meals per philosopher: " + maxMealsPerPhilosopher);
        System.out.println();

        Table table = new Table(numberOfPhilosophers);
        Philosopher[] philosophers = new Philosopher[numberOfPhilosophers];

        // Create philosophers
        for (int i = 0; i < numberOfPhilosophers; i++) {
            philosophers[i] = new Philosopher(i, table, maxMealsPerPhilosopher);
        }

        // Start all philosophers
        System.out.println("Starting the Dining Philosophers simulation...");
        initializeStartTime(); // Inicializa o tempo 0
        System.out.println("[0ms] Simulation started!");

        for (Philosopher philosopher : philosophers) {
            philosopher.start();
        }

        // Wait for all philosophers to finish
        try {
            for (Philosopher philosopher : philosophers) {
                philosopher.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Estatísticas finais
        long totalTime = System.currentTimeMillis() - startTime;
        int survivors = 0;
        int deaths = 0;

        for (Philosopher philosopher : philosophers) {
            if (philosopher.isAlive) {
                survivors++;
            } else {
                deaths++;
            }
        }

        System.out.println("\n=== SIMULATION RESULTS ===");
        System.out.println("[" + totalTime + "ms] Simulation ended!");
        System.out.println("👥 Total philosophers: " + numberOfPhilosophers);
        System.out.println("✅ Survivors: " + survivors);
        System.out.println("☠️ Deaths: " + deaths);
        System.out.println("⏱️ Total simulation time: " + totalTime + "ms");
        System.out.println("⏰ Time limit per meal: " + TIME_TO_DIE + "ms");

        if (deaths > 0) {
            System.out.println("💀 Some philosophers starved to death!");
        } else {
            System.out.println("🎉 All philosophers survived!");
        }
    }
}
