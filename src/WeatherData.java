public class WeatherData {

    public String city;

    public double temperature;

    public String description;

    public int humidity;

    public double windSpeed;

    public WeatherData(
            String city,
            double temperature,
            String description,
            int humidity,
            double windSpeed) {

        this.city = city;

        this.temperature = temperature;

        this.description = description;

        this.humidity = humidity;

        this.windSpeed = windSpeed;
    }
}