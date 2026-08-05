# Discover weather source

The Android Discover Hero uses the keyless Open-Meteo current-weather endpoint for Johannesburg and refreshes while the Discover view is open. No API key, paid SDK, or secret is used.

Open-Meteo is an open-source weather API implementation. The application retains the city snapshot weather value from self-hosted Supabase when the request is unavailable. For a fully self-hosted weather data service, deploy an Open-Meteo-compatible service and replace the endpoint in `OpenMeteoWeatherRepository`; raw weather observation/forecast data still requires a licensed public data source.
