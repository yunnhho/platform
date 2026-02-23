import React, { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";

const API_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";
const WS_URL = process.env.REACT_APP_WS_URL || "ws://localhost:8080";

const SENSOR_CATALOG = [
  { id: "SENSOR_01", name: "1층 동측 화재감지 센서", zone: "1층 동측 생산라인" },
  { id: "SENSOR_02", name: "1층 서측 유해가스 센서", zone: "1층 서측 저장구역" },
  { id: "SENSOR_03", name: "2층 중앙 진동감지 센서", zone: "2층 중앙 설비구역" },
  { id: "SENSOR_04", name: "2층 북측 열화상 센서", zone: "2층 북측 점검구역" },
];

const KNOWN_SENSOR_IDS = new Set(SENSOR_CATALOG.map((sensor) => sensor.id));

function statusClass(status) {
  if (status === "CRITICAL") return "critical";
  if (status === "WARNING") return "warning";
  if (status === "NORMAL") return "normal";
  return "unknown";
}

function statusLabel(status) {
  if (status === "CRITICAL") return "위험";
  if (status === "WARNING") return "주의";
  if (status === "NORMAL") return "정상";
  return "수신 대기";
}

function formatTime(value) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 19);
}

export default function App() {
  const [sensors, setSensors] = useState([]);
  const [audit, setAudit] = useState(null);
  const [outbox, setOutbox] = useState(null);
  const [lastSyncAt, setLastSyncAt] = useState(null);

  const mergeSensor = (prev, sensor) => {
    const next = prev.filter((item) => item.sensorId !== sensor.sensorId);
    return [...next, sensor];
  };

  const load = async () => {
    const [sensorRes, auditRes, outboxRes] = await Promise.allSettled([
      fetch(`${API_URL}/api/sensors/latest`),
      fetch(`${API_URL}/api/audit/stats`),
      fetch(`${API_URL}/api/outbox/stats`),
    ]);

    if (sensorRes.status === "fulfilled" && sensorRes.value.ok) {
      const sensorData = await sensorRes.value.json();
      const knownSensors = (sensorData || []).filter((item) =>
        KNOWN_SENSOR_IDS.has(item.sensorId)
      );
      setSensors(knownSensors);
    }

    if (auditRes.status === "fulfilled" && auditRes.value.ok) {
      setAudit(await auditRes.value.json());
    }

    if (outboxRes.status === "fulfilled" && outboxRes.value.ok) {
      setOutbox(await outboxRes.value.json());
    }

    setLastSyncAt(new Date());
  };

  useEffect(() => {
    load().catch(() => {});
    const timer = setInterval(() => load().catch(() => {}), 3000);

    const client = new Client({
      brokerURL: `${WS_URL}/ws/sensors`,
      reconnectDelay: 3000,
    });

    client.onConnect = () => {
      client.subscribe("/topic/sensors", (message) => {
        const sensor = JSON.parse(message.body);
        if (!KNOWN_SENSOR_IDS.has(sensor.sensorId)) {
          return;
        }
        setSensors((prev) => mergeSensor(prev, sensor));
      });
    };

    client.activate();

    return () => {
      clearInterval(timer);
      client.deactivate();
    };
  }, []);

  const sensorMap = useMemo(() => {
    const map = new Map();
    sensors.forEach((sensor) => map.set(sensor.sensorId, sensor));
    return map;
  }, [sensors]);

  const displaySensors = useMemo(
    () =>
      SENSOR_CATALOG.map((meta) => ({
        ...meta,
        data: sensorMap.get(meta.id) || null,
      })),
    [sensorMap]
  );

  const criticalCount = displaySensors.filter(
    (sensor) => sensor.data?.status === "CRITICAL"
  ).length;
  const warningCount = displaySensors.filter(
    (sensor) => sensor.data?.status === "WARNING"
  ).length;
  const waitingCount = displaySensors.filter((sensor) => !sensor.data).length;

  return (
    <main className="page">
      <header className="hero">
        <div>
          <p className="eyebrow">산업안전 실시간 현황판</p>
          <h1>현장 안전 모니터링 대시보드</h1>
          <p className="subtitle">
            센서 이름과 위치 중심으로 표시하여 관리자도 즉시 위험 상태를 파악할 수
            있습니다.
          </p>
        </div>
        <div className="sync-box">
          <span>최근 동기화</span>
          <strong>{lastSyncAt ? lastSyncAt.toLocaleTimeString("ko-KR") : "-"}</strong>
        </div>
      </header>

      <section className="kpi-grid">
        <article className="kpi danger">
          <h2>위험 센서</h2>
          <p>{criticalCount}개</p>
        </article>
        <article className="kpi warn">
          <h2>주의 센서</h2>
          <p>{warningCount}개</p>
        </article>
        <article className="kpi wait">
          <h2>수신 대기</h2>
          <p>{waitingCount}개</p>
        </article>
        <article className="kpi info">
          <h2>Outbox 대기</h2>
          <p>{outbox?.pending ?? 0}건</p>
        </article>
      </section>

      <section className="sensor-grid">
        {displaySensors.map((sensor) => (
          <article
            key={sensor.id}
            className={`sensor-card ${statusClass(sensor.data?.status)}`}
          >
            <div className="sensor-top">
              <h3>{sensor.name}</h3>
              <span className="status-chip">{statusLabel(sensor.data?.status)}</span>
            </div>
            <p className="zone">{sensor.zone}</p>
            <div className="reading">
              <span>위험도 지수</span>
              <strong>{sensor.data ? sensor.data.value : "-"}</strong>
            </div>
            <p className="meta">센서 ID: {sensor.id}</p>
            <p className="meta">수신 시각: {formatTime(sensor.data?.timestamp)}</p>
          </article>
        ))}
      </section>

      <section className="manager-panel">
        <article className="panel">
          <h3>감사 로그 요약</h3>
          <p>전체 시도: {audit?.totalAttempts ?? 0}건</p>
          <p>UPDATE 차단: {audit?.updateAttempts ?? 0}건</p>
          <p>DELETE 차단: {audit?.deleteAttempts ?? 0}건</p>
        </article>
        <article className="panel">
          <h3>관리자 확인 가이드</h3>
          <p>1. 위험 센서가 1개 이상이면 즉시 현장 점검</p>
          <p>2. 수신 대기가 늘면 네트워크/설비 통신 상태 점검</p>
          <p>3. Outbox 대기가 증가하면 알림 전송 지연 여부 점검</p>
        </article>
      </section>
    </main>
  );
}
