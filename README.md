
# SearchEngine
Данный проект реализует поисковый движок, предоставляющий пользователю специальный API со следующими основными функциями:
<li>  предварительное индексирование сайтов;</li>
<li>  выдача основных сведений по сайтам;</li>
<li> поиск ключевых слов в проиндексированных сайтах и предоставление их пользователю.</li>

## Веб-страница
В проект также входит веб-страница, которая позволяет управлять процессами, реализованными в движке.
Страница содержит три вкладки.

### Вкладка DASHBOARD
<img src=".\readme_assets\dashboard.png" width="80%"/><p>
Эта вкладка открывается по умолчанию. На ней отображается общая статистика по всем проиндексированным сайтам, а также детальная статистика и статус по каждому из сайтов (статистика, получаемая по запросу /statistics).

### Вкладка MANAGEMENT
<img src="./readme_assets/management.png" width="80%"/><p>
На этой вкладке находятся инструменты управления поисковым движком — запуск (запрос /startIndexing) и остановка (запрос /stopIndexing) полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу по ссылке (запрос /indexPage/{pagePath}). Отметим, что если в последнем запросе присутствует только URL сайта без завершающего слэша (/), как в приведённом выше скриншоте, то индексироваться будет указанный сайт целиком.

### Вкладка SEARCH
<img src="./readme_assets/search.png" width="80%"/><p>
<img src="./readme_assets/search_2.png" width="80%"/><p>
Эта вкладка предназначена для тестирования поискового движка. На ней находится поле поиска и выпадающий список с выбором сайта, по которому искать, а при нажатии на кнопку SEARCH выводятся результаты поиска (по запросу /search).

### Кодировка страницы
На всех вкладках присутствует выпадающий список Language (Язык). При выборе одного из двух вариантов — English или Русский — соответственно поменяется кодировка страницы. Пример русской раскладки:
<img src="./readme_assets/lang_ru.png" width="80%"/><p>

### Стек используемых технологий
Java Core, Spring Boot, JPA, Hibernate, JDBC, Security, PostgreSQL, REST API, JSOUP, Maven, Git, Swagger, MySql Thymeleaf
Также библиотеки лемматизации – RussianMorphology, EnglishMorphology  и стемминга (нахождения основы слова) - stemmer.

### Зависимости
Для работы системы в файле pom.xml необходимо добавить информацию о фреймворке:
<parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.4</version>
        <relativePath/>
    </parent> 
а также ссылку на репозиторий для скачивания зависимостей лемматизатора:
<repositories>
        <repository>
            <id>skillbox-gitlab</id>
            <url>https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven</url>
        </repository>
    </repositories>
Также нужно указать подключение следующих зависимостей apache Maven:
 spring-boot-starter-security
 spring-boot-starter-thymeleaf
 spring-boot-starter-web
 spring-boot-starter-data-jpa
 postgresql
 jsoup
Для работы парсинга страниц нужно подключить JSOUP :
<dependency>           
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.16.1</version>
        </dependency>

### Настройка СУБД
Данное приложение по выбору может работать либо с СУБД MySQL, либо с СУБД PostgreSQL. Выбор зависит от значения параметра spring.profiles.active в конфигурационном файле application.yaml, который определяет профиль и находится в корне проекта. Параметр может принимать одно из двух значений:
<li>  mysql — для работы с MySQL;</li>
<li>  postgresql — для работы с PostgreSQL.</li>
В зависимости от значения заданного профиля подключается один из двух соответствующих конфигурационных файлов: application-mysql.yaml или application-postgresql.yaml, которые тоже находятся в корне проекта.
В файлах настройки нужно указать адрес базы данных, имя базы данных, имя пользователя, пароль.
Параметр  hibernate.ddl-auto: со значением crate – при запуске приложения база данных пересоздаётся, а со значением update база данных остается прежней.
Приложение допускает подключение и к другим реляционным БД. Для этого, по аналогии с PostgreSQL, нужно создать файл application-{profilename}.yaml. Кроме того, в файле pom.xml нужно подключить подходящую зависимость, обеспечивающую работу с выбранной БД.
Для настройки DOCER используются команды:<br><br>
<b>POSTGRESQL</b> – “docker run -p 5431:5432 --name postgresql -e POSTGRES_PASSWORD=Testtest -e POSTGRES_USER=root -e POSTGRES_DB=search_engine -d postgres”<br><br>
<b>MYSQL</b> – “docker run -d --name mysql -p 3305:3306 -e MYSQL_ROOT_PASSWORD=Testtest -e MYSQL_DATABASE=search_engine mysql:8”,<br>
“docker exec -it mysql bash -l”,<br>
“mysql -uroot -pTesttest
CREATE USER 'piton'@'172.17.0.1' IDENTIFIED BY 'Testtest';
GRANT ALL ON *.* TO 'piton'@'172.17.0.1';
FLUSH PRIVILEGES;<br>
SELECT user, host FROM mysql.user;”

### Файл настройки
Стартовая страница поискового движка находится по адресу : http://localhost:8080/
Сразу при старте система запрашивает логин/пароль, которые указаны в файле конфигурации src/resources/application.yml:
  security:
    user:
      name: admin
      password: 12345
      roles: ADMIN

### Список sites
Здесь приведён список сайтов, которые программа будет в состоянии индексировать. Каждый сайт характеризуется следующими параметрами:
•  url — адрес сайта;
•  name — имя сайта. Оно, в частности, выводится в списке сайтов на вкладке DASHBOARD;

indexing-settings:<br>
  sites:<br>
    - url: http://музей-ямщика.рф<br>
      name: Музей Ямщика<br>
    - url: http://playback.ru<br>
      name: PlayBack.Ru<br>
    - url: https://www.lutherancathedral.ru/<br>
      name: Lutherancathedral.ru<br>
    - url: https://www.svetlovka.ru/<br>
      name: Svetlovka.ru<br>

### Настройка Леммонизатора
Для преобразования слов в леммы неообходимо подключение зависимостей morph, morphology, dictionary-reader, english, russianиз источника : org.apache.lucene.morphology необходимо ещё создать (либо отредактировать если он имеется - в Windows он располагается в директории C:/Users/<Имя вашего пользователя>/.m2) файл settings.xml, в котором указать токен для получения данных из публичного репозитория. В файл нужно внести следующие строки:<br>
```python
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <configuration>
                <httpHeaders>
                    <property>
                        <name>Authorization</name>
                        <value>Bearer 
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</value>
                    </property>
                </httpHeaders>
            </configuration>
        </server>
   </servers>
</settings>
```

### Запуск программы
Репозиторий с приложением SearchEngine находится по адресу https://github.com/IaPiton/Diplom<br>
Если проект загрузить на локальный диск, то он готов к тому, чтобы его можно было скомпилировать и запустить с помощью среды разработки IntelliJ IDEA.<br>
Перед первой компиляцией программы следует выполнить следующие шаги:<br>
<b>1</b> Скачать и установить Docer https://www.docker.com/products/docker-desktop/.<br>
<b>2</b>Настроить СУБД<br>
<b>3</b>Вести изменения в файл application.yaml<br>
Теперь приложение можно компилировать и запускать из программной среды.<br>
Приложение может быть также запущено командной строкой. Для этого оно должно быть скомпилировано в панели Maven среды разработки командой package:
Результат компиляции, SearchEngine-1.0-SNAPSHOT.jar, будет располагаться в папке target. Этот файл можно перенести в любую доступную папку на компьютере, при этом в той же папке должны находиться файлы application.yaml. Приложение запускается строкой java -jar SearchEngine-0.0.1-SNAPSHOT.jar.

