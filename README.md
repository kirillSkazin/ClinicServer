# Сервер

## Предварительные требования

1. Установлен Docker Desktop.
2. Docker Desktop запущен.

## Быстрый запуск через Docker

Самый простой способ запустить сервер с базой данных:

```powershell
cd Server
copy .env.example .env
docker compose up --build
```

Ожидаемое состояние:

- `clinic-postgres` - `healthy`;
- `clinic-server` - `running`.

Перед запуском откройте созданный `.env` и при необходимости поменяйте:

```properties
POSTGRES_PASSWORD=change-me-postgres-password
BOOTSTRAP_ADMIN_PASSWORD=change-me-admin-password
```

После запуска сервер будет доступен для клиента на `127.0.0.1:8765`.

## Стартовый администратор

При первом запуске, когда таблица `users` пуста, сервер создаёт администратора из настроек `bootstrap.*` или `BOOTSTRAP_*`. Для запуска логин по умолчанию - `admin`, пароль берётся из `.env` или аргумента `--bootstrap.admin.password`, по умолчанию пароль `admin123`.

## Начальные данные

При запуске сервер автоматически выполняет `src/main/resources/db/seed.sql`. Скрипт добавляет 5 врачей, их специализации, расписание на будние дни и одного стартового пациента. Его можно выполнять повторно: существующие врачи, пациент и расписание будут обновлены, дубли не создаются.

Автозаполнение можно отключить:

```properties
BOOTSTRAP_SEED_DEFAULT_DATA=false
```

Учебные логины врачей:

```text
doctor.ivanova
doctor.petrov
doctor.smirnova
doctor.kuznetsov
doctor.sokolova
```

Учебный пациент:

```text
patient.demo
```

Пароль для врачей и пациента берётся из `BOOTSTRAP_SEED_USER_PASSWORD`; по умолчанию: `password`.

## Запуск клиента

В отдельном терминале из проекта клиента:

```powershell
cd Client
mvn javafx:run
```

Или после сборки:

```powershell
cd Client
mvn clean package
java -jar target\clinic-client.jar
```

## Email-напоминания

По умолчанию в `application.properties` отправка писем выключена:

```properties
mail.enabled=false
```

Для Docker включите почту только в локальном `.env`:

```properties
MAIL_ENABLED=true
MAIL_USERNAME=your-mail@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-mail@gmail.com
```

Для Gmail нужен пароль приложения, а не обычный пароль от аккаунта.

## Тесты

```powershell
cd Server
mvn -B test
```


