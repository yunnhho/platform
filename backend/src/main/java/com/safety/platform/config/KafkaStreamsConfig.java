package com.safety.platform.config;

import com.safety.platform.dto.SensorData;
import com.safety.platform.dto.SensorStatistics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableKafkaStreams
@ConditionalOnProperty(name = "spring.kafka.streams.auto-startup", havingValue = "true", matchIfMissing = true)
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, SensorData> sensorStatsStream(StreamsBuilder builder) {
        JsonSerde<SensorData> sensorDataSerde = new JsonSerde<>(SensorData.class);
        JsonSerde<SensorStatistics> sensorStatisticsSerde = new JsonSerde<>(SensorStatistics.class);

        KStream<String, SensorData> input = builder.stream(
            "sensor-readings",
            Consumed.with(Serdes.String(), sensorDataSerde)
        );

        input.groupBy((key, value) -> value.getSensorId(), Grouped.with(Serdes.String(), sensorDataSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(30)))
            .aggregate(
                SensorStatistics::new,
                (key, value, aggregate) -> {
                    aggregate.addReading(value.getValue());
                    return aggregate;
                },
                Materialized.<String, SensorStatistics, WindowStore<Bytes, byte[]>>as("sensor-statistics-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(sensorStatisticsSerde)
                    .withLoggingEnabled(Map.of(
                        "retention.ms", "86400000",
                        "cleanup.policy", "compact"
                    ))
                    .withCachingEnabled()
            )
            .toStream()
            .map((Windowed<String> windowedKey, SensorStatistics stats) ->
                org.apache.kafka.streams.KeyValue.pair(windowedKey.key(), stats))
            .to("sensor-stats", Produced.with(Serdes.String(), sensorStatisticsSerde));

        return input;
    }
}
