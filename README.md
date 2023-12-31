# Итоговый проект
Проект с подключенными библиотеками лемматизаторами.
Содержит несколько контроллеров, сервисов и репозиторий с подключением к бд MySQL.

Проект выполняет индексирование (сохраняет в базу данных страницы и найденные на них леммы) сайтов, указанных в файле настроек (application.yaml), а также отдельных страниц по запросу, а также производит поиск слов по найденным леммам.

## Требования системы - Java 17, БД MySQL

## Спецификация API

### Запуск полной индексации — GET /api/startIndexing
Метод без параметров

Формат ответа в случае успеха:
{
	'result': true
}
Формат ответа в случае ошибки:
{
	'result': false,
	'error': "Индексация уже запущена"
}

### Остановка текущей индексации — GET /api/stopIndexing
Метод без параметров.

Формат ответа в случае успеха:
{
	'result': true
}

Формат ответа в случае ошибки:
{
	'result': false,
	'error': "Индексация не запущена"
}

### Добавление или обновление отдельной страницы — POST /api/indexPage
Параметры:
url — адрес страницы, которую нужно переиндексировать.

Формат ответа в случае успеха:
{
	'result': true
}

Формат ответа в случае ошибки:
{
	'result': false,
	'error': "Текст сообщения об ошибке"
}

### Статистика — GET /api/statistics
Метод без параметров.

Формат ответа:
{
	'result': true,
	'statistics': {
		"total": {
			"sites": 10,
			"pages": 436423,
			"lemmas": 5127891,
			"indexing": true
},
"detailed": [
	{
		"url": "http://www.site.com",
		"name": "Имя сайта",
		"status": "INDEXED",
		"statusTime": 1600160357,
		"error": "Ошибка индексации: главная страница сайта недоступна",
		"pages": 5764,
		"lemmas": 321115
},
...
]
}

### Получение данных по поисковому запросу — GET /api/search
Параметры:

query — поисковый запрос;
site — сайт, по которому осуществлять поиск
offset — сдвиг от 0 для постраничного вывода (значение по умолчанию равно нулю);
limit — количество результатов, которое необходимо вывести (значение по умолчанию равно 20).

Формат ответа в случае успеха:
{
	'result': true,
	'count': 574,
	'data': [
		{
			"site": "http://www.site.com",
			"siteName": "Имя сайта", "uri": "/path/to/page/6784",
			"title": "Заголовок страницы, которую выводим",
			"snippet": "Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML",
			"relevance": 0.93362
},
...
]
}

Формат ответа в случае ошибки:
{
	'result': false,
	'error': "Задан пустой поисковый запрос"
}

## Работа с web-интерфейсом: при запуске страницы проекта (http://localhost:8080/) на экране открывается меню с тремя пунктами:
- в пункте "DASHBOARD" показано количество проиндексированных сайтов и страниц, и найденных лемм, а также для каждого сайта показан текущий статус индексации;
- в пункте "MANAGEMENT" два управляющих элемента: "START INDEXING" (после нажатия текст меняется на "STOP INDEXING") для запуска/остановки индексирования и форма добавления отдельной страницы;
- в пункте "SEARCH" - поиск текста по сохранённым в БД данным.

## Инструкция по запуску проекта:
- скачать исходный текст со страницы проекта, разрешить зависимости maven командой вида `mvn dependency:resolve`, прописать в файле настроек параметры доступа к базе данных MySQL параметры индексируемых сайтов, запустить проект на выполнение.
