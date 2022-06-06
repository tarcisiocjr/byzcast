package ch.usi.inf.dslab.bftamcast.client;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */

import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

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
        String[] globalConfigPaths = p.getGlobalConfig();
        String[] localConfigPaths = p.getLocalConfigs();
        int numGroups = localConfigPaths == null ? 1 : localConfigPaths.length;
        ProxyIf proxy = new Proxy(idClient + 1000 * idGroup, globalConfigPaths, localConfigPaths);
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
                    if(result != null) {
                        req.fromBytes(result);
                        System.out.println("previous value: " + (req.getValue() == null ? "NULL" : new String(req.getValue())));
                    }
                    else
                        System.err.println("Empty response from server.");
                    break;
                case 2:
                    System.out.println("Reading value from the map");
                    req.setType(RequestType.GET);
                    req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
                    req.setValue(null);
                    req.setDestination(new int[]{req.getKey() % numGroups});
                    result = proxy.atomicMulticast(req);
                    if(result != null) {
                        req.fromBytes(result);
                        System.out.println("value: " + (req.getValue() == null ? "NULL" : new String(req.getValue())));
                    }
                    else
                        System.err.println("Empty response from server.");
                    break;
                case 3:
                    System.out.println("Removing value in the map");
                    req.setType(RequestType.REMOVE);
                    req.setKey(Integer.parseInt(console.readLine("Enter the key: ")));
                    req.setDestination(new int[]{req.getKey() % numGroups});
                    result = proxy.atomicMulticast(req);
                    if(result != null) {
                        req.fromBytes(result);
                        System.out.println("removed value: " + (req.getValue() == null ? "NONE" : new String(req.getValue())));
                    }
                    else
                        System.err.println("Empty response from server.");
                    break;
                case 4:
                    System.out.println("Getting the map size");
                    req.setType(RequestType.SIZE);
                    req.setKey(0);
                    req.setValue(null);

                    dest = new int[2];
                    dest[0] = Integer.parseInt(console.readLine("Enter first dest group: "));
                    dest[1] = Integer.parseInt(console.readLine("Enter second dest group: "));

                    //dest = new int[numGroups];
                    //for (int i = 0; i < dest.length; i++)
                    //    dest[i] = i;

                    req.setDestination(dest);
                    result = proxy.atomicMulticast(req);
                    if (result != null) {
                        req.fromBytes(result);
                        result = req.getValue();
                        if (result != null) {
                            System.out.println("result size = " + result.length);
                            for (int i = 0; i < dest.length; i++)
                                System.out.println("Map size (group " + dest[i] + "): " + ByteBuffer.wrap(Arrays.copyOfRange(result, i * 4, i * 4 + 4)).getInt());
                            break;
                        }
                        System.err.println("ERROR: empty value.");
                    } else
                        System.err.println("ERROR: empty response.");
                    break;
                default:
                    System.err.println("Invalid option...");
            }
        }
    }
}
