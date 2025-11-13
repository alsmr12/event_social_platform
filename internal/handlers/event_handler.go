package handlers

import (
	"event_social_platform/internal/models"
	"event_social_platform/internal/repository"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

type EventHandler struct {
	eventRepo       *repository.EventRepository
	userRepo        *repository.UserRepository
	eventSubRepo    *repository.EventSubscriptionRepository
	achievementRepo *repository.AchievementRepository
}

func NewEventHandler(eventRepo *repository.EventRepository, userRepo *repository.UserRepository, eventSubRepo *repository.EventSubscriptionRepository, achievementRepo *repository.AchievementRepository) *EventHandler {
	return &EventHandler{
		eventRepo:       eventRepo,
		userRepo:        userRepo,
		eventSubRepo:    eventSubRepo,
		achievementRepo: achievementRepo,
	}
}

func (h *EventHandler) ShowCreateEventForm(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":       "Создание события",
		"NavActive":   "create_event",
		"CurrentUser": currentUser,
	})
}

func (h *EventHandler) CreateEvent(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	var req models.CreateEventRequest
	if err := c.ShouldBind(&req); err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Создание события",
			"NavActive":   "event",
			"Error":       "Неверные данные формы",
			"CurrentUser": currentUser,
		})
		return
	}

	// Парсим дату и время
	eventTime, err := time.Parse("2006-01-02T15:04", req.DateTime)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Создание события",
			"NavActive":   "event",
			"Error":       "Неверный формат даты и времени",
			"CurrentUser": currentUser,
		})
		return
	}

	event := &models.Event{
		Title:           req.Title,
		Description:     req.Description,
		Type:            req.Type,
		DateTime:        eventTime,
		Location:        req.Location,
		CreatorID:       currentUser.ID,
		IsPrivate:       req.IsPrivate,
		MaxParticipants: req.MaxParticipants,
	}

	// Генерируем код приглашения и приватный ключ для приватных событий
	if event.IsPrivate {
		event.GenerateInviteCode()
		event.GeneratePrivateKey()
	}

	if err := h.eventRepo.CreateEvent(event); err != nil {
		c.HTML(http.StatusInternalServerError, "base.html", gin.H{
			"Title":       "Создание события",
			"NavActive":   "event",
			"Error":       "Ошибка при создании события: " + err.Error(),
			"CurrentUser": currentUser,
		})
		return
	}
	if h.achievementRepo != nil {
		go h.achievementRepo.UpdateAchievementsOnEventCreated(currentUser.ID)
	}
	c.Redirect(http.StatusSeeOther, "/events")
}

func (h *EventHandler) GetAllEvents(c *gin.Context) {
    // Получаем параметр фильтра из URL
    filter := c.DefaultQuery("filter", "upcoming")

    var events []*models.Event
    var err error

    // Получаем события в зависимости от фильтра
    if filter == "past" {
        events, err = h.eventRepo.GetPastEvents()
    } else {
        events, err = h.eventRepo.GetUpcomingEvents()
    }

    if err != nil {
        c.HTML(http.StatusInternalServerError, "base.html", gin.H{
            "Title":       "События",
            "NavActive":   "events",
            "Error":       "Ошибка получения событий",
            "CurrentUser": GetUserFromContext(c),
        })
        return
    }

    currentUser := GetUserFromContext(c)

    // ФИЛЬТРУЕМ: показываем события, к которым пользователь имеет доступ
    var filteredEvents []*models.Event
    for _, event := range events {
        // Если событие публичное - показываем всем
        if !event.IsPrivate {
            filteredEvents = append(filteredEvents, event)
            continue
        }

        // Если событие приватное, проверяем доступ
        if currentUser != nil {
            hasAccess, _ := h.eventRepo.CanUserAccessEvent(currentUser.ID, event.ID)
            if hasAccess {
                filteredEvents = append(filteredEvents, event)
            }
        }
        // Если пользователь не авторизован или нет доступа - не показываем приватное событие
    }

    // Для каждого события получаем информацию о подписках и определяем, прошло ли оно
    for _, event := range filteredEvents {
        // Добавляем информацию о подписках
        if currentUser != nil {
            isSubscribed, _ := h.eventSubRepo.IsSubscribed(currentUser.ID, event.ID)
            event.IsSubscribed = isSubscribed
        }

        subscribersCount, _ := h.eventSubRepo.GetSubscribersCount(event.ID)
        event.SubscribersCount = subscribersCount

        // Определяем, является ли событие прошедшим
        event.IsPast = time.Now().After(event.DateTime)
    }

    // Получаем сообщение из URL
    message := c.Query("message")

    c.HTML(http.StatusOK, "base.html", gin.H{
        "Title":          "События",
        "NavActive":      "events",
        "Events":         filteredEvents,
        "CurrentUser":    currentUser,
        "Message":        message,
        "ShowPastEvents": filter == "past",
    })
}

// GetEvent - обработчик для получения события по ID (публичные события)
func (h *EventHandler) GetEvent(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.HTML(http.StatusBadRequest, "base.html", gin.H{
			"Title":       "Событие",
			"NavActive":   "event",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Событие",
			"NavActive":   "event",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)

	// Проверяем доступ к приватному событию
	if event.IsPrivate {
		hasAccess, _ := h.eventRepo.CanUserAccessEvent(currentUser.ID, event.ID)
		if !hasAccess {
			c.HTML(http.StatusForbidden, "base.html", gin.H{
				"Title":       "Доступ запрещен",
				"NavActive":   "event",
				"Error":       "Это приватное событие. У вас нет доступа.",
				"CurrentUser": currentUser,
			})
			return
		}
	}

	// Получаем информацию о подписке
	var isSubscribed bool
	var subscribersCount int64
	if currentUser != nil {
		isSubscribed, _ = h.eventSubRepo.IsSubscribed(currentUser.ID, event.ID)
		subscribersCount, _ = h.eventSubRepo.GetSubscribersCount(event.ID)
	}

	// Определяем, является ли событие прошедшим
	event.IsPast = time.Now().After(event.DateTime)

	// Получаем базовый URL для формирования ссылки
	baseURL := getBaseURL(c)

	message := c.Query("message")
	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":            event.Title,
		"NavActive":        "event",
		"Event":            event,
		"CurrentUser":      currentUser,
		"IsSubscribed":     isSubscribed,
		"SubscribersCount": subscribersCount,
		"Message":          message,
		"BaseURL":          baseURL,
	})
}

// GetPrivateEvent - обработчик для приватных событий по уникальному ключу
func (h *EventHandler) GetPrivateEvent(c *gin.Context) {
	privateKey := c.Param("key")
	currentUser := GetUserFromContext(c)
	
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	event, err := h.eventRepo.GetEventByPrivateKey(privateKey)
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Событие не найдено",
			"NavActive":   "events",
			"Error":       "Неверная ссылка или событие было удалено",
			"CurrentUser": currentUser,
		})
		return
	}

	// Получаем информацию о подписке
	var isSubscribed bool
	var subscribersCount int64
	if currentUser != nil {
		isSubscribed, _ = h.eventSubRepo.IsSubscribed(currentUser.ID, event.ID)
		subscribersCount, _ = h.eventSubRepo.GetSubscribersCount(event.ID)
	}

	// Определяем, является ли событие прошедшим
	event.IsPast = time.Now().After(event.DateTime)

	// Получаем базовый URL для формирования ссылки
	baseURL := getBaseURL(c)

	c.HTML(http.StatusOK, "base.html", gin.H{
		"Title":            event.Title,
		"NavActive":        "event",
		"Event":            event,
		"CurrentUser":      currentUser,
		"IsSubscribed":     isSubscribed,
		"SubscribersCount": subscribersCount,
		"BaseURL":          baseURL,
	})
}

// AccessByInviteCode - доступ к событию по коду приглашения
func (h *EventHandler) AccessByInviteCode(c *gin.Context) {
	code := c.Param("code")
	currentUser := GetUserFromContext(c)
	
	if currentUser == nil {
		c.Redirect(http.StatusSeeOther, "/login")
		return
	}

	event, err := h.eventRepo.GetEventByInviteCode(code)
	if err != nil {
		c.HTML(http.StatusNotFound, "base.html", gin.H{
			"Title":       "Событие не найдено",
			"NavActive":   "events",
			"Error":       "Неверный код приглашения или событие было удалено",
			"CurrentUser": currentUser,
		})
		return
	}

	// Перенаправляем на приватную ссылку события
	c.Redirect(http.StatusSeeOther, "/event/private/"+event.PrivateKey)
}

// AccessByInviteCodeForm - форма для ввода кода приглашения
func (h *EventHandler) AccessByInviteCodeForm(c *gin.Context) {
    code := c.Query("code")
    if code != "" {
        // Если код передан в URL параметре, перенаправляем сразу
        c.Redirect(http.StatusSeeOther, "/invite/"+code)
        return
    }

    currentUser := GetUserFromContext(c)
    c.HTML(http.StatusOK, "base.html", gin.H{
        "Title":       "Доступ по коду приглашения",
        "NavActive":   "events", 
        "CurrentUser": currentUser,
    })
}

func (h *EventHandler) DeleteEvent(c *gin.Context) {
	currentUser := GetUserFromContext(c)
	if currentUser == nil {
		c.Redirect(302, "/login")
		return
	}

	// Получаем ID события из URL
	eventID := c.Param("id")

	// Преобразуем в число
	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.JSON(400, gin.H{"error": "Invalid event ID"})
		return
	}

	// Получаем событие из базы данных
	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.JSON(404, gin.H{"error": "Event not found"})
		return
	}

	// Проверяем, является ли пользователь создателем события
	if event.CreatorID != currentUser.ID {
		c.JSON(403, gin.H{"error": "Access denied"})
		return
	}

	// Удаляем событие
	err = h.eventRepo.DeleteEvent(uint(id))
	if err != nil {
		c.JSON(500, gin.H{"error": "Error deleting event"})
		return
	}

	// Перенаправляем обратно на страницу событий
	c.Redirect(302, "/events")
}

// Показать форму редактирования
func (h *EventHandler) ShowEditEventForm(c *gin.Context) {
	eventID := c.Param("id")

	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "events",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	event, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(404, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "events",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	currentUser := GetUserFromContext(c)
	if currentUser == nil || currentUser.ID != event.CreatorID {
		c.HTML(403, "base.html", gin.H{
			"Title":       "Ошибка",
			"NavActive":   "events",
			"Error":       "Доступ запрещен",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Передаем BaseURL для формы редактирования
	baseURL := getBaseURL(c)

	c.HTML(200, "base.html", gin.H{
		"Title":       "Редактировать событие",
		"NavActive":   "edit_event",
		"Event":       event,
		"CurrentUser": currentUser,
		"BaseURL":     baseURL,
	})
}

// Обновить событие
func (h *EventHandler) UpdateEvent(c *gin.Context) {
	eventID := c.Param("id")

	id, err := strconv.ParseUint(eventID, 10, 32)
	if err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Error":       "Неверный ID события",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Получаем существующее событие
	existingEvent, err := h.eventRepo.GetEventByID(uint(id))
	if err != nil {
		c.HTML(404, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Error":       "Событие не найдено",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Проверяем, что текущий пользователь - создатель события
	currentUser := GetUserFromContext(c)
	if currentUser == nil || currentUser.ID != existingEvent.CreatorID {
		c.HTML(403, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Error":       "Доступ запрещен",
			"CurrentUser": GetUserFromContext(c),
		})
		return
	}

	// Парсим форму
	var form models.CreateEventRequest
	if err := c.ShouldBind(&form); err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Event":       existingEvent,
			"CurrentUser": currentUser,
			"Error":       "Неверные данные формы",
		})
		return
	}

	// Парсим дату и время
	dateTime, err := time.Parse("2006-01-02T15:04", form.DateTime)
	if err != nil {
		c.HTML(400, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Event":       existingEvent,
			"CurrentUser": currentUser,
			"Error":       "Неверный формат даты и времени",
		})
		return
	}

	// Сохраняем предыдущее состояние приватности
	wasPrivate := existingEvent.IsPrivate

	// Обновляем поля события
	existingEvent.Title = form.Title
	existingEvent.Description = form.Description
	existingEvent.Type = form.Type
	existingEvent.DateTime = dateTime
	existingEvent.Location = form.Location
	existingEvent.IsPrivate = form.IsPrivate
	existingEvent.MaxParticipants = form.MaxParticipants

	// Если событие стало приватным и у него нет приватного ключа - генерируем
	if existingEvent.IsPrivate && existingEvent.PrivateKey == "" {
		existingEvent.GeneratePrivateKey()
	}

	// Если событие стало приватным и у него нет кода - генерируем
	if existingEvent.IsPrivate && existingEvent.InviteCode == "" {
		existingEvent.GenerateInviteCode()
	}

	// Если событие из приватного стало публичным - очищаем приватные данные
	if !existingEvent.IsPrivate && wasPrivate {
		existingEvent.InviteCode = ""
		existingEvent.PrivateKey = ""
	}

	// Обрабатываем координаты
	if form.Latitude != "" {
		if lat, err := strconv.ParseFloat(form.Latitude, 64); err == nil {
			existingEvent.Latitude = lat
		}
	}
	if form.Longitude != "" {
		if lng, err := strconv.ParseFloat(form.Longitude, 64); err == nil {
			existingEvent.Longitude = lng
		}
	}

	// Сохраняем изменения
	err = h.eventRepo.UpdateEvent(existingEvent)
	if err != nil {
		c.HTML(500, "base.html", gin.H{
			"Title":       "Редактировать событие",
			"NavActive":   "edit_event",
			"Event":       existingEvent,
			"CurrentUser": currentUser,
			"Error":       "Ошибка при сохранении изменений",
		})
		return
	}

	// Перенаправляем на соответствующую страницу события
	if existingEvent.IsPrivate {
		c.Redirect(302, "/event/private/"+existingEvent.PrivateKey)
	} else {
		c.Redirect(302, "/event/"+eventID)
	}
}

// getBaseURL - вспомогательная функция для получения базового URL
func getBaseURL(c *gin.Context) string {
	scheme := "http"
	if c.Request.TLS != nil || c.Request.Header.Get("X-Forwarded-Proto") == "https" {
		scheme = "https"
	}
	return scheme + "://" + c.Request.Host
}