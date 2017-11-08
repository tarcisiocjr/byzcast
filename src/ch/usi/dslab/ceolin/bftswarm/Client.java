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

import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.util.Random;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 */

public class Client {

  private static int VALUE_SIZE = 1024;

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println(
          "Usage: java Client <cliend id> <num groups> <local config> <global config> <verbose?>");
      System.exit(-1);
    }

    long startTime = System.currentTimeMillis();
    long totalTime = 120000;
    boolean toFinish = false;

    int idClient = Integer.parseInt(args[0]);
    int idGroup = 0;
    int numGroups = Integer.parseInt(args[1]);
    boolean verbose = Boolean.parseBoolean(args[4]);

    String localConfigPath = args[2];
    String globalConfigPath = args[3];

    Proxy client = new Proxy(idClient, idGroup, localConfigPath, globalConfigPath);

    int ops = 0;
    while (!toFinish) {
      try {
        boolean result = insertValue(client, ops, numGroups);
        if (!result) {
          System.out.println("Problem");
        }

        toFinish = (System.currentTimeMillis() - startTime >= totalTime);

        if (verbose && (ops % 100 == 0)) {
          System.out.println("ops sent:" + ops);
        }
        ops++;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.out.println("total ops sent:" + ops);
    System.exit(-1);
  }

  private static boolean insertValue(Proxy client, int index, int numGroups) throws Exception {

    // bft-smart random implementation.....
    byte[] valueBytes = null;
    Random rand = new Random();
    valueBytes = new byte[VALUE_SIZE];
    rand.nextBytes(valueBytes);

    String key = "" + index;
    String message = "" + valueBytes;

    JSONObject objValue = new JSONObject();
    JSONArray groupValue = new JSONArray();

    for (int i = 0; i < numGroups; i++) {
      groupValue.add(i);
    }

    objValue.put("key", new Integer(key));
    objValue.put("message", message);
    objValue.put("targetGroup", groupValue);

    String jsonValue = JSONValue.toJSONString(objValue);

    String result = client.put(key, jsonValue);
    //        System.out.println("Previous value:" + result);

    return true;
  }
}
