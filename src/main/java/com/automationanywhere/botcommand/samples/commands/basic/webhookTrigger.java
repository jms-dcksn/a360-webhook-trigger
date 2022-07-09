/*
 * Copyright (c) 2020 Automation Anywhere.
 * All rights reserved.
 *
 * This software is the proprietary information of Automation Anywhere.
 * You shall use it only in accordance with the terms of the license agreement
 * you entered into with Automation Anywhere.
 */
/**
 *@author: James Dickson
 */
package com.automationanywhere.botcommand.samples.commands.basic;


import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.RecordValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import static com.automationanywhere.commandsdk.model.AttributeType.HELP;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.record.Record;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.annotations.rules.NumberInteger;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import static com.automationanywhere.commandsdk.model.DataType.RECORD;

@BotCommand(commandType = BotCommand.CommandType.Trigger)
@CommandPkg(label = "Webhook Trigger", description = "Starts web server on localhost and listens on defined port and path",
        icon = "bolt.svg", name = "webTrigger",
        return_type = RECORD, return_name = "TriggerData",
        return_description = "Available keys: triggerType, requestData")
public class webhookTrigger {

    private static final Map<String, HttpServer> taskMap = new ConcurrentHashMap<>();
    @TriggerId
    private String triggerUid;
    @TriggerConsumer
    private Consumer consumer;

    public void onHook(String request) {
        consumer.accept(getRecordValue(request));
    }

    class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] response;
            if ("GET".equals(httpExchange.getRequestMethod())) {
                //do something
                onHook("GET request received");
                response = "Your bot was triggered".getBytes();
                httpExchange.sendResponseHeaders(200, response.length);
            }
            else if ("POST".equals(httpExchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                int b;
                StringBuilder buf = new StringBuilder();
                while ((b = br.read()) != -1) {
                    buf.append((char) b);
                }
                br.close();
                isr.close();
                onHook(buf.toString());
                response = "Your bot was triggered".getBytes();
                httpExchange.sendResponseHeaders(200, response.length);
            }
            else {
                response = "Bad Request".getBytes();
                httpExchange.sendResponseHeaders(400, response.length);
            }
            OutputStream os = httpExchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private RecordValue getRecordValue(String request) {
        List<Schema> schemas = new LinkedList<>();
        List<Value> values = new LinkedList<>();
        schemas.add(new Schema("triggerType"));
        values.add(new StringValue("Webhook Trigger"));
        schemas.add(new Schema("requestData"));
        values.add(new StringValue(request));
        RecordValue recordValue = new RecordValue();
        recordValue.set(new Record(schemas, values));
        return recordValue;
    }

    @StartListen
    public void startTrigger(
            @Idx(index = "1", type = HELP)
            @Pkg(label = "Setup Info",
                    description = "Enter a hostname, port number and a path parameter to formulate the URI for the incoming " +
                            "webhook. e.g. with port 3000 and path parameter 'trigger', the URI would be http://<hostname>:3000/trigger " +
                            "or http://localhost:3000/trigger")
                    String help,
            @Idx(index = "2", type = AttributeType.TEXT)
            @Pkg(label = "Device Host Name or localhost")
            @NotEmpty String host,
            @Idx(index = "3", type = AttributeType.NUMBER)
            @Pkg(label = "Port", description = "e.g. 5000")
            @NumberInteger
            @NotEmpty Double port,
            @Idx(index = "4", type = AttributeType.TEXT)
            @Pkg(label = "Path Parameter", description = "e.g. trigger")
            @NotEmpty String path) {

        //input check
        if (host.equals("")) { throw new BotCommandException("Host cannot be empty"); }

        try {
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            HttpServer server = HttpServer.create(new InetSocketAddress(host, port.intValue()), 0);
            server.createContext("/" + path, new MyHttpHandler());
            server.setExecutor(threadPoolExecutor);

            if (taskMap.get(triggerUid) == null) {
                synchronized (this) {
                    if (taskMap.get(triggerUid) == null) {
                        taskMap.put(triggerUid, server);
                        server.start();
                    }
                }
            }
        } catch (Exception e) {
            throw new BotCommandException("Something went wrong starting the listener. Please check your inputs. Error: " + e);
        }
    }
    /*
     * Cancel all the task and clear the map.
     */
    @StopAllTriggers
    public void stopAllTriggers() {
        taskMap.forEach((k, v) -> {
            try {
                v.stop(0);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BotCommandException(e);
            }
            taskMap.remove(k);
        });
    }

    /*
     * Cancel the task and remove from map
     *
     * @param triggerUid
     */
    @StopListen
    public void stopListen(String triggerUid) throws Exception {
        taskMap.get(triggerUid).stop(0);
        taskMap.remove(triggerUid);
    }

    public String getTriggerUid() {
        return triggerUid;
    }

    public void setTriggerUid(String triggerUid) {
        this.triggerUid = triggerUid;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

}
