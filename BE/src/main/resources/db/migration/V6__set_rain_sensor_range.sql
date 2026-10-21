UPDATE sensors
SET unit = 'mức',
    min_value = 0.00,
    max_value = 500.00
WHERE sensor_key = 'rain';
