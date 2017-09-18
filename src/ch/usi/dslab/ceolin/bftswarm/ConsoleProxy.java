/*
  BFT-SWARM - A (hopeful) high-throughput BFT-SMART implementation
  Copyright (C) 2017, University of Lugano

  This file is part of BFT-SWARM.

  BFT-SWARM is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package ch.usi.dslab.ceolin.bftswarm;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 */
import java.io.Console;
import java.util.Scanner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ConsoleProxy {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ConsoleProxy <proxy id> <group id>");
            System.exit(0);
        }

        Proxy client = new Proxy(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        Console console = System.console();

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("Select an option:");
            System.out.println("1. ADD A KEY/VALUE");
            System.out.println("2. READ A VALUE");
            System.out.println("3. REMOVE AND ENTRY");
            System.out.println("4. GET THE SIZE OF THE MAP");

            int cmd = sc.nextInt();

            switch (cmd) {
                case 1:
                    System.out.println("Putting value in the distributed map");
                    String key = console.readLine("Enter the key: ");
                    String message = console.readLine("Enter the value: ");
                    String targetGroup;
                    int targetN = Integer.parseInt(System.console().readLine("Enter the amount of groups: "));

                    JSONObject objValue = new JSONObject();
                    JSONArray groupValue = new JSONArray();

                    for (int i = 0; i < targetN; i++){
                        targetGroup = console.readLine("Enter the target group " + i + ": ");
                        groupValue.add(targetGroup);
                    }

                    objValue.put("key", new Integer (key));

                    objValue.put("targetGroup", groupValue);
                    objValue.put("message", new String (message));
                    System.out.println("objValue: " + objValue);
//                    String jsonValue = JSONValue.escape(objValue.toJSONString());
                    String jsonValue = JSONValue.toJSONString(objValue);
                    System.out.println("jsonValue: " + jsonValue);

                    String result = client.put(key, jsonValue);
                    System.out.println("Previous value: " + result);
                    break;
                case 2:
                    System.out.println("Reading value from the map");
                    key = console.readLine("Enter the key:");
                    result = client.get(key);
                    System.out.println("Value read: " + result);
                    break;
                case 3:
                    System.out.println("Removing value in the map");
                    key = console.readLine("Enter the key:");
                    result = client.remove(key);
                    System.out.println("Value removed: " + result);
                    break;
                case 4:
                    System.out.println("Getting the map size");
                    int size = client.size();
                    System.out.println("Map size: " + size);
                    break;
            }
        }
    }
}
