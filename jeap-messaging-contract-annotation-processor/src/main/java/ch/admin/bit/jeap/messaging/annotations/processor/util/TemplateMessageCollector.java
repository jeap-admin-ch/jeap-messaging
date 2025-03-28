package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class TemplateMessageCollector {

    private static final String JSON_EXTENSION = ".json";
    private static final String EVENTS_KEY = "events";
    private static final String MESSAGES_KEY = "messages";
    private static final String MESSAGE_NAME_KEY = "messageName";
    private static final String EVENT_NAME_KEY = "eventName";
    private static final String TOPIC_NAME_KEY = "topicName";

    public Map<String, Set<String>> collectTemplateMessages(String templatePath, Messager messager) {
        Map<String, Set<String>> templateMessageMap = new HashMap<>();

        try (Stream<Path> paths = Files.walk(Paths.get(templatePath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(JSON_EXTENSION))
                    .forEach(path -> processFile(path, templateMessageMap, messager));
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error reading directory: " + templatePath + " - " + e.getMessage());
        }

        return templateMessageMap;
    }

    private void processFile(Path path, Map<String, Set<String>> templateMessageMap, Messager messager) {
        try {
            String content = Files.readString(path);
            JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

            addTemplateMessagesToMap(templateMessageMap, jsonObject.getAsJsonArray(EVENTS_KEY));
            addTemplateMessagesToMap(templateMessageMap, jsonObject.getAsJsonArray(MESSAGES_KEY));
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error processing file: " + path + " - " + e.getMessage());
        }
    }

    private void addTemplateMessagesToMap(Map<String, Set<String>> templateMessageMap, JsonArray templateMessageJsonArray) {
        if (templateMessageJsonArray != null) {
            for (int i = 0; i < templateMessageJsonArray.size(); i++) {
                JsonObject contract = templateMessageJsonArray.get(i).getAsJsonObject();
                String name = contract.has(MESSAGE_NAME_KEY) ? contract.get(MESSAGE_NAME_KEY).getAsString() : contract.get(EVENT_NAME_KEY).getAsString();
                String topicName = contract.get(TOPIC_NAME_KEY).getAsString();

                templateMessageMap.computeIfAbsent(name, k -> new TreeSet<>()).add(topicName);
            }
        }
    }
}
