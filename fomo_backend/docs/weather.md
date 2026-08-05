# Discover weather source

The Android Discover Hero uses the keyless Open-Meteo forecast endpoint for Johannesburg and refreshes while the Discover view is open. The request includes current conditions, seven daily forecasts, 48 hourly forecasts, and 96 fifteen-minute forecast points with the `best_match` model. No API key, paid SDK, or secret is used.

Open-Meteo is an open-source weather API implementation. The application retains the city snapshot weather value from self-hosted Supabase when the request is unavailable. For a fully self-hosted weather data service, deploy an Open-Meteo-compatible service and replace the endpoint in `OpenMeteoWeatherRepository`; raw weather observation/forecast data still requires a licensed public data source.
