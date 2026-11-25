# binance-price-tracker

Тестовое задание. Сервис раз в N секунд берёт среднюю цену BNBUSDT с Binance (`/api/v3/avgPrice`), сохраняет в Postgres и отдаёт через REST.

Java 21, Spring Boot 3, PostgreSQL, Flyway.

## Как запустить

Через докер:

```bash
docker compose up --build
```

Приложение будет на `http://localhost:8080`.

Или из IDE: сначала поднять базу `docker compose up -d postgres`, потом запустить `PriceTrackerApplication`.

Тесты (нужен запущенный докер, там Testcontainers):

```bash
mvn test
```

## Переменные окружения

- `BINANCE_POLL_INTERVAL` — как часто опрашивать Binance, по умолчанию `PT30S`
- `BINANCE_SYMBOL` — торговая пара, по умолчанию `BNBUSDT`
- `BINANCE_BASE_URL` — адрес Binance API, по умолчанию `https://api.binance.com`
- `BINANCE_CONNECT_TIMEOUT` / `BINANCE_READ_TIMEOUT` — таймауты, по умолчанию `PT5S` / `PT10S`
- `TREND_FLAT_THRESHOLD_PERCENT` — если цена изменилась меньше чем на столько процентов, тренд `FLAT`. По умолчанию `0.5`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — подключение к базе, по умолчанию `jdbc:postgresql://localhost:5432/prices`, `prices`, `prices`
- `APP_PORT` — порт приложения в docker compose, по умолчанию `8080`

## Эндпоинты

Даты `from` и `to` передаются в формате `2026-11-25T00:00:00Z`.

Последняя цена:
```bash
curl http://localhost:8080/api/v1/prices/latest
```

Средняя цена за период:
```bash
curl "http://localhost:8080/api/v1/prices/average?from=2026-11-25T00:00:00Z&to=2026-11-26T00:00:00Z"
```

Все записи за период (с пагинацией):
```bash
curl "http://localhost:8080/api/v1/prices?from=2026-11-25T00:00:00Z&to=2026-11-26T00:00:00Z&page=0&size=20"
```

Тренд за период (`UPWARD`, `DOWNWARD` или `FLAT`, сравниваю первую и последнюю цену):
```bash
curl "http://localhost:8080/api/v1/prices/trend?from=2026-11-25T00:00:00Z&to=2026-11-26T00:00:00Z"
```

## Пару слов про реализацию

- Если Binance не ответил, ошибка просто пишется в лог, а на следующем запуске будет новая попытка.
- Если пришёл 429, опрос встаёт на паузу на столько, сколько сказано в `Retry-After`.
- Дубли не появляются: в таблице уникальный ключ по `symbol` + `close_time` из ответа Binance и `ON CONFLICT DO NOTHING`.
- Цена хранится как `BigDecimal` / `NUMERIC`, без double.
