package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.util.CLIParser;

import java.util.Random;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Client {

    public static void main(String[] args) throws InterruptedException {
        CLIParser p = CLIParser.getClientParser(args);
        Random r = new Random();
        int totalTime = p.getDuration();
        int valueSize = p.getMsgSize();
        int clientCount = p.getClientCount();
        int idGroup = p.getGroup();
        int id = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE) : p.getId();
        int perc = p.getGlobalPercent();
        boolean ng = p.isNonGenuine();
        int async = p.getAsync();
        String globalConfigPath = p.getGlobalConfig();
        String[] localConfigPaths = p.getLocalConfigs();
        ClientThread[] clients = new ClientThread[clientCount];
        Thread[] clientThreads = new Thread[clientCount];

        if (async == 0) {
            for (int i = 0; i < clientCount; i++) {
                System.out.println("Starting client " + (id + i));
                Thread.sleep(r.nextInt(600));
                clients[i] = new ClientThread(id + i, idGroup, globalConfigPath,
                        localConfigPaths, true, totalTime, valueSize, perc, ng);
                clientThreads[i] = new Thread(clients[i]);
                clientThreads[i].start();
            }

            for (int i = 0; i < clientCount; i++) {
                clientThreads[i].join();
                System.out.println("Client " + i + " finished execution");

            }
        } else {
            for (int i = 0; i < clientCount; i++) {
                System.out.println("Starting client " + (id + i));
                Thread.sleep(r.nextInt(600));
                clients[i] = new AsyncClientThread(id + i, idGroup, globalConfigPath, localConfigPaths,
                        true, totalTime, valueSize, perc, async, ng);
                clientThreads[i] = new Thread(clients[i]);
                clientThreads[i].start();
            }


            try {
                Thread.sleep((totalTime) * 1000);
                for (ClientThread c : clients) {
                    ((AsyncClientThread) c).stop();
                    Thread.sleep(r.nextInt(600));
                    ((AsyncClientThread) c).saveStats();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
/*
        ExecutorService exec = Executors.newFixedThreadPool(clients.length);
        Collection<Future<?>> tasks = new LinkedList<>();

        for (ClientThread c : clients) {
            tasks.add(exec.submit(c));
        }

        // wait for tasks completion
        for (Future<?> currTask : tasks) {
            try {
                currTask.get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        exec.shutdown();
*/

        System.exit(0);
    }

}
