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
        int id = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE / 2) : p.getId();
        int perc = p.getGlobalPercent();
        boolean ng = p.isNonGenuine();
        String[] msgDestination = p.getMsgDestination();
        String[] globalConfigPaths = p.getGlobalConfig();
        String[] localConfigPaths = p.getLocalConfigs();
        ClientThread[] clients = new ClientThread[clientCount];
        Thread[] clientThreads = new Thread[clientCount];

        for (int i = 0; i < clientCount; i++) {
            System.out.println("Starting client " + (id + i));
            Thread.sleep(r.nextInt(600));
            clients[i] = new ClientThread(id + i, idGroup, globalConfigPaths,
                    localConfigPaths, true, totalTime, valueSize, perc, ng, msgDestination);
            clientThreads[i] = new Thread(clients[i]);
            clientThreads[i].start();
        }

        for (int i = 0; i < clientCount; i++) {
            clientThreads[i].join();
            System.out.println("Client " + i + " finished execution");
        }
        System.exit(0);
    }

}
