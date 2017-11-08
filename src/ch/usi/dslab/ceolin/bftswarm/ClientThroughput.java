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

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 */
public class ClientThroughput implements Runnable {

  private boolean running = false;
  private int clientId;
  private int groupId;
  private int numOfGroups;
  private String localConfig;
  private String globalConfig;
  private Boolean verbose;
  private int runningTime;
  private static int VALUE_SIZE = 1024;

  private ClientThroughput(int clientId, int groupId, int numOfGroups, String localConfig, String globalConfig, Boolean verbose, int runningTime) {
    this.clientId = clientId;
    this.groupId = groupId;
    this.numOfGroups = numOfGroups;
    this.localConfig = localConfig;
    this.globalConfig = globalConfig;
    this.verbose = verbose;
    this.runningTime = runningTime;
    Thread thread = new Thread(this);
    thread.start();
  }

  @Override
  public void run() {
    this.running = true;
    System.out.println("Thread ID: " + Thread.currentThread().getId());
    System.out.println("Client ID: " + clientId);
    System.out.println("Number of Groups: " + numOfGroups);
    System.out.println("Local Config: " + localConfig);
    System.out.println("Global Config: " + globalConfig);
    System.out.println("Verbose: " + verbose);

    Proxy clientThroughput = new Proxy(clientId, groupId, localConfig, globalConfig);

    long startTime = System.currentTimeMillis();
    long totalTime = runningTime;
//    long totalTime = 120000;

    boolean toFinish = false;
    int ops = 0;
    int i = 0;
    while(!toFinish) {
      try {
        boolean result = insertValue(clientThroughput, ops, numOfGroups);
        if(!result){
          System.out.println("Error");
        }

        toFinish = (System.currentTimeMillis() - startTime >= totalTime);

        ops++;
        i++;
      } catch (Exception e){
        e.printStackTrace();
      }
    }
    System.out.println("total ops sent:" + ops);

    try {
      Thread.sleep(totalTime+100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    this.running = false;
  }

  public static void main(String[] args) throws InterruptedException {
    if (args.length < 7) {
      System.out.println(
          "Usage: ClientThroughput <client id> <local group id> <num of clients> <num of groups> <local config> <global config> <verbose?> <running time>");
      System.exit(-1);
    }

    List<ClientThroughput> workers = new ArrayList<>();

    int clientId = Integer.parseInt(args[0]);
    int groupId = Integer.parseInt(args[1]);
    int numOfClients = Integer.parseInt(args[2]);
    int numOfGroups = Integer.parseInt(args[3]);
    String localConfig = args[4];
    String globalConfig = args[5];
    boolean verbose = Boolean.parseBoolean(args[6]);
    int runningTime = Integer.parseInt(args[7]);

    System.out.println("Main thread with id " + Thread.currentThread().getId());

    Date start = new Date();

    for (int i = 0; i < numOfClients; i++) {
      workers.add(new ClientThroughput(clientId+i, groupId, numOfGroups, localConfig, globalConfig, verbose, runningTime));
    }

    // We must force the main thread to wait for all the workers
    // to finish their work before we check to see how long it took
    // to complete
    for (ClientThroughput worker : workers) {
      while (worker.running) {
        Thread.sleep(100);
      }
    }

    Date end = new Date();

    long difference = end.getTime() - start.getTime();

    System.out.println("Took " + difference/1000 + "seconds.");
  }

  private static boolean insertValue(Proxy client, int index, int numOfGroups) throws Exception {

    // bft-smart random implementation
    byte[] valueBytes;
    Random rand = new Random();
    valueBytes = new byte[VALUE_SIZE];
    rand.nextBytes(valueBytes);

    String key = "" + index;
    String message = "" + valueBytes;

    JSONObject objValue = new JSONObject();
    JSONArray groupValue = new JSONArray();

    for (int i = 0; i < numOfGroups; i++) {
      groupValue.add(i+1);
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
