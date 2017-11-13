package ch.usi.inf.dslab.bftamcast.client;

import java.util.Arrays;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Client {

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 3) {
            System.out.println(
                    "Usage: java Client <cliend COUNT> <client group> <global config> <local config 0> <local config 1> ... <local config N>");
            System.exit(-1);
        }

        int totalTime = 20;
        int valueSize = 32;
        int clientCount = Integer.parseInt(args[0]);
        int idGroup = Integer.parseInt(args[1]);
        String globalConfigPath = args[2];
        String[] localConfigPaths = args.length == 3 ? null : Arrays.copyOfRange(args, 3, args.length);
        ClientThread[] clients = new ClientThread[clientCount];
        Thread[] clientThreads = new Thread[clientCount];

        for (int i = 0; i < clientCount; i++) {
            System.out.println("Starting client " + i);
            clients[i] = new ClientThread(i, idGroup, globalConfigPath, localConfigPaths, true, totalTime, valueSize);
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
