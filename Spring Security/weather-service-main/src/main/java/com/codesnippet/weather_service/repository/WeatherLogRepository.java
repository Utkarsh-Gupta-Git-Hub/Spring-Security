package com.codesnippet.weather_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherLogRepository extends JpaRepository<WeatherLog, Long> {
}
