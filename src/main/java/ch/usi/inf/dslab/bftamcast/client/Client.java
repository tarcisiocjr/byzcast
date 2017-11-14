package ch.usi.inf.dslab.bftamcast.client;

import ch.usi.inf.dslab.bftamcast.util.CLIParser;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Client {

    public static void main(String[] args) throws InterruptedException {
        CLIParser p = CLIParser.getClientParser(args);
        int totalTime = p.getDuration();
        int valueSize = p.getMsgSize();
        int clientCount = p.getClientCount();
        int idGroup = p.getGroup();
        int id = p.getId();
        int perc = p.getGlobalPercent();
        String globalConfigPath = p.getGlobalConfig();
        String[] localConfigPaths = p.getLocalConfigs();
        ClientThread[] clients = new ClientThread[clientCount];
        Thread[] clientThreads = new Thread[clientCount];

        for (int i = 0; i < clientCount; i++) {
            System.out.println("Starting client " + i);
            Thread.sleep(300);
            clients[i] = new ClientThread(id + i, idGroup, globalConfigPath, localConfigPaths, true, totalTime, valueSize, perc);
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
