import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class WeatherAPIClient {

    // KEEP YOUR WORKING API KEY HERE
    private final String API_KEY = "your_real_key";

    private final HttpClient client;

    // 10-minute cache
    private static final long CACHE_DURATION =
            10 * 60 * 1000;

    private final Map<String, CachedWeather> cache =
            new HashMap<>();

    public WeatherAPIClient() {

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // =====================================================
    // CURRENT WEATHER
    // =====================================================

    public WeatherData getWeather(String city)
            throws Exception {

        String cacheKey =
                city.toLowerCase().trim();

        // Check cache first
        CachedWeather cached =
                cache.get(cacheKey);

        if (cached != null &&
                System.currentTimeMillis()
                        - cached.timestamp
                        < CACHE_DURATION) {

            System.out.println(
                    "Using cached weather for "
                            + city);

            return cached.weather;
        }

        String encodedCity =
                URLEncoder.encode(
                        city,
                        StandardCharsets.UTF_8);

        String url =
                "https://api.openweathermap.org/data/2.5/weather"
                        + "?q=" + encodedCity
                        + "&appid=" + API_KEY
                        + "&units=metric";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header(
                                "Accept",
                                "application/json")
                        .GET()
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new Exception(
                    "API Error - Status: "
                            + response.statusCode()
                            + "\n"
                            + response.body());
        }

        String json = response.body();

        double temperature =
                extractNumber(
                        json,
                        "\"temp\":");

        int humidity =
                (int) extractNumber(
                        json,
                        "\"humidity\":");

        double windSpeed =
                extractNumber(
                        json,
                        "\"speed\":");

        String description =
                extractString(
                        json,
                        "\"description\":\"");

        WeatherData weather =
                new WeatherData(
                        city,
                        temperature,
                        description,
                        humidity,
                        windSpeed);

        // Save result in cache
        cache.put(
                cacheKey,
                new CachedWeather(weather));

        return weather;
    }

    // =====================================================
    // 5-DAY FORECAST
    // =====================================================

    public String getForecast(String city)
            throws Exception {

        String encodedCity =
                URLEncoder.encode(
                        city,
                        StandardCharsets.UTF_8);

        String url =
                "https://api.openweathermap.org/data/2.5/forecast"
                        + "?q=" + encodedCity
                        + "&appid=" + API_KEY
                        + "&units=metric";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header(
                                "Accept",
                                "application/json")
                        .GET()
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new Exception(
                    "Forecast API Error - Status: "
                            + response.statusCode()
                            + "\n"
                            + response.body());
        }

        String json = response.body();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "====================================\n");

        result.append(
                "          5-DAY FORECAST\n");

        result.append(
                "====================================\n\n");

        int listStart =
                json.indexOf("\"list\":[");

        if (listStart == -1) {

            throw new Exception(
                    "Forecast data not found.");
        }

        int position =
                listStart + 8;

        int count = 0;

        // OpenWeather provides 40 forecast entries
        // (every 3 hours for 5 days)
        while (count < 40) {

            int itemStart =
                    json.indexOf(
                            "{\"dt\":",
                            position);

            if (itemStart == -1) {
                break;
            }

            int itemEnd =
                    findObjectEnd(
                            json,
                            itemStart);

            if (itemEnd == -1) {
                break;
            }

            String item =
                    json.substring(
                            itemStart,
                            itemEnd + 1);

            String date =
                    extractString(
                            item,
                            "\"dt_txt\":\"");

            double temperature =
                    extractNumber(
                            item,
                            "\"temp\":");

            int humidity =
                    (int) extractNumber(
                            item,
                            "\"humidity\":");

            double wind =
                    extractNumber(
                            item,
                            "\"speed\":");

            String description =
                    extractString(
                            item,
                            "\"description\":\"");

            result.append(
                            "Date: ")
                    .append(date)
                    .append("\n");

            result.append(
                            "Temperature: ")
                    .append(
                            String.format(
                                    "%.1f°C",
                                    temperature))
                    .append("\n");

            result.append(
                            "Weather: ")
                    .append(description)
                    .append("\n");

            result.append(
                            "Humidity: ")
                    .append(humidity)
                    .append("%\n");

            result.append(
                            "Wind Speed: ")
                    .append(
                            String.format(
                                    "%.1f m/s",
                                    wind))
                    .append("\n");

            result.append(
                    "------------------------------------\n");

            position =
                    itemEnd + 1;

            count++;
        }

        if (count == 0) {

            throw new Exception(
                    "No forecast entries found.");
        }

        return result.toString();
    }

    // =====================================================
    // FIND END OF JSON OBJECT
    // =====================================================

    private int findObjectEnd(
            String json,
            int start) {

        int depth = 0;

        boolean insideString = false;

        boolean escaped = false;

        for (int i = start;
             i < json.length();
             i++) {

            char c =
                    json.charAt(i);

            if (escaped) {

                escaped = false;

                continue;
            }

            if (c == '\\'
                    && insideString) {

                escaped = true;

                continue;
            }

            if (c == '"') {

                insideString =
                        !insideString;

                continue;
            }

            if (!insideString) {

                if (c == '{') {

                    depth++;
                }

                else if (c == '}') {

                    depth--;

                    if (depth == 0) {

                        return i;
                    }
                }
            }
        }

        return -1;
    }

    // =====================================================
    // WEATHER ALERTS
    // =====================================================

    public String getAlerts(
            WeatherData weather) {

        StringBuilder alerts =
                new StringBuilder();

        alerts.append(
                "\n\n========== WEATHER ALERTS ==========\n");

        boolean alertFound = false;

        if (weather.temperature >= 40) {

            alerts.append(
                    "⚠ HIGH TEMPERATURE WARNING\n");

            alertFound = true;
        }

        if (weather.temperature <= 5) {

            alerts.append(
                    "⚠ LOW TEMPERATURE WARNING\n");

            alertFound = true;
        }

        if (weather.humidity >= 90) {

            alerts.append(
                    "⚠ VERY HIGH HUMIDITY\n");

            alertFound = true;
        }

        if (weather.windSpeed >= 15) {

            alerts.append(
                    "⚠ HIGH WIND WARNING\n");

            alertFound = true;
        }

        String description =
                weather.description
                        .toLowerCase();

        if (description.contains(
                "storm")) {

            alerts.append(
                    "⚠ STORM WARNING\n");

            alertFound = true;
        }

        if (description.contains(
                "rain")) {

            alerts.append(
                    "⚠ RAIN ALERT\n");

            alertFound = true;
        }

        if (!alertFound) {

            alerts.append(
                    "✓ No weather alerts\n");
        }

        return alerts.toString();
    }

    // =====================================================
    // JSON NUMBER EXTRACTION
    // =====================================================

    private double extractNumber(
            String json,
            String key) {

        int keyPosition =
                json.indexOf(key);

        if (keyPosition == -1) {

            return 0;
        }

        int start =
                keyPosition + key.length();

        int end = start;

        while (end < json.length()
                && "0123456789.-"
                .indexOf(
                        json.charAt(end)) >= 0) {

            end++;
        }

        String value =
                json.substring(
                        start,
                        end);

        if (value.isEmpty()) {

            return 0;
        }

        return Double.parseDouble(value);
    }

    // =====================================================
    // JSON STRING EXTRACTION
    // =====================================================

    private String extractString(
            String json,
            String key) {

        int keyPosition =
                json.indexOf(key);

        if (keyPosition == -1) {

            return "Unknown";
        }

        int start =
                keyPosition + key.length();

        int end =
                json.indexOf(
                        "\"",
                        start);

        if (end == -1) {

            return "Unknown";
        }

        return json.substring(
                start,
                end);
    }

    // =====================================================
    // CACHE
    // =====================================================

    public int getCacheSize() {

        return cache.size();
    }

    public void clearCache() {

        cache.clear();

        System.out.println(
                "Weather cache cleared.");
    }

    // =====================================================
    // CACHE CLASS
    // =====================================================

    private static class CachedWeather {

        WeatherData weather;

        long timestamp;

        CachedWeather(
                WeatherData weather) {

            this.weather =
                    weather;

            this.timestamp =
                    System.currentTimeMillis();
        }
    }
}