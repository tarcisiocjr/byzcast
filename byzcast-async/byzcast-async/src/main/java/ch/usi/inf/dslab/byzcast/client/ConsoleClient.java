package ch.usi.inf.dslab.byzcast.client;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin@acad.pucrs.br
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */

import ch.usi.inf.dslab.byzcast.kvs.Request;
import ch.usi.inf.dslab.byzcast.kvs.RequestType;
import ch.usi.inf.dslab.byzcast.util.CLIParser;

import java.io.Console;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class ConsoleClient {

    public static void main(String[] args) {
        CLIParser p = CLIParser.getClientParser(args);
        Random r = new Random();
        int idGroup = p.getGroup();
        int idClient = p.getId() == 0 ? r.nextInt(Integer.MAX_VALUE) : p.getId();
        String globalConfigPath = p.getGlobalConfig();
        String[] localConfigPaths = p.getLocalConfigs();
        int numGroups = localConfigPaths == null ? 1 : localConfigPaths.length;
        ProxyIf proxy = new Proxy(idClient + 1000 * idGroup, globalConfigPath, localConfigPaths);
        Request req = new Request();
        int[] dest;
        byte[] result;

        Console console = System.console();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("Select an option:");
            System.out.println("1. ADD A KEY/VALUE");
            System.out.println("2. READ A VALUE");
            System.out.println("3. REMOVE AND ENTRY");
            System.out.println("4. GET THE SIZE OF THE MAP (multi-partition)");

            int cmd = sc.nextInt();

            switch (cmd) {
                case 1:
                    System.out.println("Putting value in the distributed map");
                    req.setType(RequestType.PUT);
                    req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
                    req.setDestination(new int[]{req.getKey() % numGroups});
                    req.setValue(console.readLine("Enter the value: ").getBytes());
                    result = proxy.atomicMulticast(req);
                    System.out.println("previous value: " + (result == null ? "NULL" : new String(result)));
                    break;
                case 2:
                    System.out.println("Reading value from the map");
                    req.setType(RequestType.GET);
                    req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
                    req.setValue(null);
                    req.setDestination(new int[]{req.getKey() % numGroups});
                    result = proxy.atomicMulticast(req);
                    System.out.println("value: " + (result == null ? "NULL" : new String(result)));
                    break;
                case 3:
                    System.out.println("Removing value in the map");
                    req.setType(RequestType.REMOVE);
                    req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
                    req.setDestination(new int[]{req.getKey() % numGroups});
                    result = proxy.atomicMulticast(req);
                    System.out.println("removed value: " + (result == null ? "NULL" : new String(result)));
                    break;
                case 4:
                    System.out.println("Getting the map size");
                    req.setType(RequestType.SIZE);
                    req.setKey(0);
                    req.setValue(null);
                    dest = new int[numGroups];
                    for (int i = 0; i < dest.length; i++)
                        dest[i] = i;

                    req.setDestination(dest);
                    result = proxy.atomicMulticast(req);
                    System.out.println("result size = " + result.length);
                    for (int i = 0; i < dest.length; i++)
                        System.out.println("Map size (group " + i + "): " + (result == null ? "NULL" : ByteBuffer.wrap(Arrays.copyOfRange(result, i * 4, i * 4 + 4)).getInt()));


                    break;
                default:
                    System.err.println("Invalid option...");
            }
        }
    }
}
