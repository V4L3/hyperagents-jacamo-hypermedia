package org.hyperagents.jacamo.artifacts.wot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;

import cartago.INTERNAL_OPERATION;
import cartago.LINK;
import cartago.ObsProperty;
import jason.asSyntax.ASSyntax;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.hyperagents.jacamo.artifacts.yggdrasil.Notification;
import org.apache.hc.client5.http.fluent.Request;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension to the ThingArtifact class that adds Yggdrasil-specific WebSub
 * support.
 * WebSubThingArtifact is a subclass of ThingArtifact and provides additional
 * functionality
 * for registering WebSub to a Yggdrasil node.
 *
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St. Gallen
 * - Valentin Berger, Interactions-HSG, University of St.Gallen
 *
 */
public class WebSubThingArtifact extends ThingArtifact {

    @Override
    public void init(String url) {
        super.init(url);
        exposeWebSubIRIs(url);
    }

    @LINK
    public void onNotification(Notification notification) {

        String message = notification.getMessage();

        if (isJsonLdFormat(message)) {
            // Extract the "propertyName" and "value" from the JSON-LD message
            String propertyName = extractValueFromJson(message, "propertyName");
            String value = extractValueFromJson(message, "value");

            // Split the value to extract function parameters
            String[] params = value.split(",");

            // Extract annotations from the JSON-LD message
            HashMap<String, String> annotations = extractAnnotationsFromJsonLd(message);

            ObsProperty op = this.defineObsProperty(propertyName, (Object[]) params);
            HashMap<String, Integer> timestampsMap = extractTimestamps(message);
            if (!timestampsMap.isEmpty()) {
                op.addAnnot(
                        ASSyntax.createStructure("vectorClock",
                                ASSyntax.createString(timestampsMap.toString())));

                op.addAnnot(
                        ASSyntax.createStructure("toBeChecked",
                                ASSyntax.createString("true")));
            }

            for (Map.Entry<String, String> entry : annotations.entrySet()) {
                op.addAnnot(ASSyntax.createStructure(entry.getKey(), ASSyntax.createString(entry.getValue())));
            }

        } else {
            log("The state of this ThingArtifact has changed: " + message);

            String obsProp = message;
            String functor = obsProp.substring(0, obsProp.indexOf("("));
            String[] params = obsProp.substring(obsProp.indexOf("(") + 1, obsProp.length() - 1).split(",");

            // Handle the observable property using original format
            if (this.hasObsPropertyByTemplate(functor, (Object[]) params)) {
                this.updateObsProperty(functor, (Object[]) params);
            } else {
                this.defineObsProperty(functor, (Object[]) params);
            }
        }
    }

    // Helper method to extract timestamps into a HashMap
    private HashMap<String, Integer> extractTimestamps(String json) {
        HashMap<String, Integer> timestampsMap = new HashMap<>();

        int timestampsStartIndex = json.indexOf("\"timestamps\": {");
        if (timestampsStartIndex != -1) {
            timestampsStartIndex += "\"timestamps\": {".length();
            int timestampsEndIndex = json.indexOf("}", timestampsStartIndex);

            if (timestampsEndIndex != -1) {
                String timestampsString = json.substring(timestampsStartIndex, timestampsEndIndex).trim();

                // Split and parse each key-value pair
                String[] pairs = timestampsString.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                        Integer value = Integer.parseInt(keyValue[1].trim());
                        timestampsMap.put(key, value);
                    }
                }
            }
        }

        return timestampsMap;
    }

    // Helper method to check if the message is in JSON-LD format
    private boolean isJsonLdFormat(String message) {
        return message.contains("\"@context\"") && message.contains("\"type\"");
    }

    // Helper method to extract values from JSON-like strings
    private String extractValueFromJson(String json, String key) {
        String keyWithQuotes = "\"" + key + "\": ";
        int startIndex = json.indexOf(keyWithQuotes) + keyWithQuotes.length();
        if (startIndex == -1) {
            return null; // Key not found
        }

        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1 || (json.indexOf("}", startIndex) != -1 && json.indexOf("}", startIndex) < endIndex)) {
            endIndex = json.indexOf("}", startIndex);
        }
        if (endIndex == -1 || (json.indexOf("]", startIndex) != -1 && json.indexOf("]", startIndex) < endIndex)) {
            endIndex = json.indexOf("]", startIndex);
        }

        if (endIndex == -1) {
            endIndex = json.length();
        }

        String value = json.substring(startIndex, endIndex).trim().replaceAll("^\"|\"$", "");

        return value;
    }

    // Helper method to extract annotations from the JSON-LD message
    private HashMap<String, String> extractAnnotationsFromJsonLd(String jsonLd) {
        HashMap<String, String> annotations = new HashMap<>();
        String annotKey = "\"annotations\": {";
        int annotStartIndex = jsonLd.indexOf(annotKey);
        if (annotStartIndex != -1) {
            annotStartIndex += annotKey.length();
            int annotEndIndex = jsonLd.indexOf("}", annotStartIndex);
            if (annotEndIndex != -1) {
                String annotsString = jsonLd.substring(annotStartIndex, annotEndIndex).trim();

                // Split and parse each key-value pair
                String[] pairs = annotsString.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                        String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                        annotations.put(key, value);
                    }
                }
            }
        }
        return annotations;
    }

    /*
     * Expose WebSub IRIs to the Agent from the given URL. This method checks the
     * headers and the content of the URL if it is an HTML document.
     */
    private void exposeWebSubIRIs(String url) {
        try {
            ClassicHttpResponse classicResponse = (ClassicHttpResponse) Request.get(url).execute().returnResponse();
            Header[] linkHeaders = classicResponse.getHeaders("Link");
            String contentType = classicResponse.getFirstHeader("content-type").getValue();
            HttpEntity entity = classicResponse.getEntity();

            String content = entity != null && contentType.contains("text/html")
                    ? EntityUtils.toString(entity)
                    : null;

            Optional<String> hub = Optional.empty();
            Optional<String> topic = Optional.empty();

            // Parse the Link headers
            for (Header header : linkHeaders) {
                Map<String, String> links = parseLinkHeader(header.getValue());
                if (links.containsKey("hub")) {
                    hub = Optional.of(links.get("hub"));
                }
                if (links.containsKey("self")) {
                    topic = Optional.of(links.get("self"));
                }
                if (hub.isPresent() && topic.isPresent()) {
                    break;
                }
            }

            if (hub.isPresent() && topic.isPresent()) {
                log("Found WebSub links in headers: " + hub.get() + ", " + topic.get());
                defineObsProperty("websub", hub.get(), topic.get());
                return;
            }

            // Parse the HTML content if headers did not contain the links
            if (content != null) {
                hub = extractLinkFromContent(content, "<link rel=\"hub\" href=\"([^\"]+)\">");
                topic = extractLinkFromContent(content, "<link rel=\"self\" href=\"([^\"]+)\">");
            }

            if (hub.isPresent() && topic.isPresent()) {
                log("Found WebSub links in Document: " + hub.get() + ", " + topic.get());
                defineObsProperty("websub", hub.get(), topic.get());
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> parseLinkHeader(String headerValue) {
        Map<String, String> links = new HashMap<>();
        String[] parts = headerValue.split(",\\s*<");
        for (String part : parts) {
            String[] linkAndRel = part.split(">;\\s*rel=\"");
            if (linkAndRel.length == 2) {
                String url = linkAndRel[0].replace("<", "");
                String rel = linkAndRel[1].replace("\"", "");
                if ("hub".equals(rel) || "self".equals(rel)) {
                    links.put(rel, url);
                }
            }
        }
        return links;
    }

    private Optional<String> extractLinkFromContent(String content, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

}
