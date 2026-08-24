import javax.swing.*;
import java.awt.*;

public class WeatherAppGUI extends JFrame {

    private JTextField cityField;

    private JTextArea resultArea;

    private JButton weatherButton;

    private JButton forecastButton;

    private JButton clearCacheButton;

    private JLabel statusLabel;

    private WeatherAPIClient apiClient;

    public WeatherAppGUI() {

        apiClient =
                new WeatherAPIClient();

        setTitle(
                "🌤 Weather Application");

        setSize(750, 600);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        createGUI();
    }

    private void createGUI() {

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(10, 10));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        15,
                        15,
                        15));

        // ================= TOP =================

        JPanel topPanel =
                new JPanel(
                        new FlowLayout());

        JLabel cityLabel =
                new JLabel("City:");

        cityField =
                new JTextField(20);

        weatherButton =
                new JButton(
                        "Get Weather");

        forecastButton =
                new JButton(
                        "5-Day Forecast");

        clearCacheButton =
                new JButton(
                        "Clear Cache");

        topPanel.add(cityLabel);

        topPanel.add(cityField);

        topPanel.add(weatherButton);

        topPanel.add(forecastButton);

        topPanel.add(clearCacheButton);

        // ================= RESULT =================

        resultArea =
                new JTextArea();

        resultArea.setEditable(false);

        resultArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        14));

        resultArea.setLineWrap(false);

        JScrollPane scrollPane =
                new JScrollPane(
                        resultArea);

        // ================= STATUS =================

        statusLabel =
                new JLabel(
                        "Ready");

        statusLabel.setBorder(
                BorderFactory.createEtchedBorder());

        // ================= ACTIONS =================

        weatherButton.addActionListener(
                e -> getWeather());

        forecastButton.addActionListener(
                e -> getForecast());

        clearCacheButton.addActionListener(
                e -> {

                    apiClient.clearCache();

                    statusLabel.setText(
                            "Cache cleared");

                    JOptionPane.showMessageDialog(
                            this,
                            "Weather cache cleared.");
                });

        // ================= ADD =================

        mainPanel.add(
                topPanel,
                BorderLayout.NORTH);

        mainPanel.add(
                scrollPane,
                BorderLayout.CENTER);

        mainPanel.add(
                statusLabel,
                BorderLayout.SOUTH);

        add(mainPanel);
    }

    // ================= CURRENT WEATHER =================

    private void getWeather() {

        String city =
                cityField.getText()
                        .trim();

        if (city.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a city name.");

            return;
        }

        weatherButton.setEnabled(false);

        statusLabel.setText(
                "Fetching weather...");

        new SwingWorker<WeatherData, Void>() {

            @Override
            protected WeatherData
            doInBackground()
                    throws Exception {

                return apiClient
                        .getWeather(city);
            }

            @Override
            protected void done() {

                try {

                    WeatherData weather =
                            get();

                    StringBuilder output =
                            new StringBuilder();

                    output.append(
                            "====================================\n");

                    output.append(
                            "       🌤 WEATHER INFORMATION\n");

                    output.append(
                            "====================================\n\n");

                    output.append(
                                    "Location: ")
                            .append(weather.city)
                            .append("\n");

                    output.append(
                                    "Temperature: ")
                            .append(
                                    String.format(
                                            "%.1f°C",
                                            weather.temperature))
                            .append("\n");

                    output.append(
                                    "Weather: ")
                            .append(weather.description)
                            .append("\n");

                    output.append(
                                    "Humidity: ")
                            .append(weather.humidity)
                            .append("%\n");

                    output.append(
                                    "Wind Speed: ")
                            .append(
                                    String.format(
                                            "%.1f m/s",
                                            weather.windSpeed))
                            .append("\n");

                    output.append(
                                    "\nCache Entries: ")
                            .append(
                                    apiClient
                                            .getCacheSize());

                    output.append(
                            apiClient
                                    .getAlerts(weather));

                    resultArea.setText(
                            output.toString());

                    statusLabel.setText(
                            "Weather loaded successfully");

                } catch (Exception ex) {

                    resultArea.setText(
                            "❌ ERROR\n\n"
                                    + ex.getMessage());

                    statusLabel.setText(
                            "Request failed");
                }

                weatherButton.setEnabled(true);
            }

        }.execute();
    }

    // ================= FORECAST =================

    private void getForecast() {

        String city =
                cityField.getText()
                        .trim();

        if (city.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a city name.");

            return;
        }

        forecastButton.setEnabled(false);

        statusLabel.setText(
                "Fetching 5-day forecast...");

        new SwingWorker<String, Void>() {

            @Override
            protected String
            doInBackground()
                    throws Exception {

                return apiClient
                        .getForecast(city);
            }

            @Override
            protected void done() {

                try {

                    resultArea.setText(
                            get());

                    statusLabel.setText(
                            "Forecast loaded successfully");

                } catch (Exception ex) {

                    resultArea.setText(
                            "❌ FORECAST ERROR\n\n"
                                    + ex.getMessage());

                    statusLabel.setText(
                            "Forecast request failed");
                }

                forecastButton.setEnabled(true);
            }

        }.execute();
    }



    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    WeatherAppGUI app =
                            new WeatherAppGUI();

                    app.setVisible(true);
                });
    }
}